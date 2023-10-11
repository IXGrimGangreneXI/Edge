package org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound.sync;

import org.asf.edge.mmoserver.entities.smartfox.SfsUser;
import org.asf.edge.mmoserver.io.SequenceWriter;
import org.asf.edge.mmoserver.networking.packets.ISmartfoxPacket;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPacketData;

public class ClientboundPlayerJoinRoomPacket implements ISmartfoxPacket {

	public int roomID;
	public SfsUser user;

	@Override
	public ISmartfoxPacket createInstance() {
		return new ClientboundPlayerJoinRoomPacket();
	}

	@Override
	public short packetID() {
		return 1000;
	}

	@Override
	public void parse(SmartfoxPacketData packet) {
	}

	@Override
	public void build(SmartfoxPacketData packet) {
		// Write room ID
		packet.payload.setInt("r", roomID);

		// Write user
		SequenceWriter writer = new SequenceWriter();
		user.writeTo(writer);
		packet.payload.setObjectArray("u", writer.toArray());
	}

}
