package org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound.sync;

import org.asf.edge.mmoserver.networking.packets.ISmartfoxPacket;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPacketData;

public class ClientboundPlayerLeaveRoomPacket implements ISmartfoxPacket {

	public int roomID;
	public int userID;

	@Override
	public ISmartfoxPacket createInstance() {
		return new ClientboundPlayerLeaveRoomPacket();
	}

	@Override
	public short packetID() {
		return 1002;
	}

	@Override
	public void parse(SmartfoxPacketData packet) {
	}

	@Override
	public void build(SmartfoxPacketData packet) {
		packet.payload.setInt("r", roomID);
		packet.payload.setInt("u", userID);
	}

}
