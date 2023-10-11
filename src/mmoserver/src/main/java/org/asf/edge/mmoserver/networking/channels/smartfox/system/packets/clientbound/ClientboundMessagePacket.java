package org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound;

import org.asf.edge.mmoserver.entities.smartfox.SfsUser;
import org.asf.edge.mmoserver.io.SequenceReader;
import org.asf.edge.mmoserver.io.SequenceWriter;
import org.asf.edge.mmoserver.networking.packets.ISmartfoxPacket;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPacketData;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

public class ClientboundMessagePacket implements ISmartfoxPacket {

	public byte type;
	public int roomID;
	public int sender;
	public SfsUser senderData;
	public String message;
	public SmartfoxPayload parameters;

	@Override
	public ISmartfoxPacket createInstance() {
		return new ClientboundMessagePacket();
	}

	@Override
	public short packetID() {
		return 7;
	}

	@Override
	public void parse(SmartfoxPacketData packet) {
		// Write headers
		type = packet.payload.getByte("t");
		if (type == 0) {
			roomID = packet.payload.getInt("r");
			sender = packet.payload.getInt("u");
		} else if (type == 1 || type == 2 || type == 3 || type == 5) {
			sender = packet.payload.getInt("u");

			// Read sender data
			if (packet.payload.has("sd")) {
				SequenceReader rd = new SequenceReader(packet.payload.getObjectArray("sd"));
				senderData = SfsUser.readFrom(rd);
			}
		}

		// Write message
		message = packet.payload.getString("m");
		if (packet.payload.has("p"))
			parameters = packet.payload.getObject("p");
	}

	@Override
	public void build(SmartfoxPacketData packet) {
		packet.payload.setByte("t", type);
		if (type == 0) {
			packet.payload.setInt("r", roomID);
			packet.payload.setInt("u", sender);
		} else if (type == 1 || type == 2 || type == 3 || type == 5) {
			packet.payload.setInt("u", sender);

			// Write sender data
			SequenceWriter writer = new SequenceWriter();
			senderData.writeTo(writer);
			packet.payload.setObjectArray("sd", writer.toArray());
		}
		packet.payload.setString("m", message);
		if (parameters != null)
			packet.payload.setObject("p", parameters);
	}

}
