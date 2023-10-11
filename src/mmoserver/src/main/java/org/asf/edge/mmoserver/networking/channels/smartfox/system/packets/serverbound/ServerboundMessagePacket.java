package org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.serverbound;

import org.asf.edge.mmoserver.networking.packets.ISmartfoxPacket;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPacketData;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

public class ServerboundMessagePacket implements ISmartfoxPacket {

	public byte type;
	public int roomID = -1;
	public int recipient;
	public String recipientString;
	public byte recipientMode;
	public int sender;
	public String message;
	public SmartfoxPayload parameters;

	@Override
	public ISmartfoxPacket createInstance() {
		return new ServerboundMessagePacket();
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
		} else if (type == 2 || type == 3) {
			recipientMode = (byte) packet.payload.getInt("rm");
			if (recipientMode != 2)
				recipient = packet.payload.getInt("rc");
			else
				recipientString = packet.payload.getString("rc");
		} else
			recipient = packet.payload.getInt("rc");

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
		} else if (type == 2 || type == 3) {
			packet.payload.setInt("rm", recipientMode);
			if (recipientMode != 2)
				packet.payload.setInt("rc", recipient);
			else
				packet.payload.setString("rc", recipientString);
		} else
			packet.payload.setInt("rc", recipient);
		packet.payload.setString("m", message);
		if (parameters != null)
			packet.payload.setObject("p", parameters);
	}

}
