package org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound;

import org.asf.edge.mmoserver.networking.packets.ISmartfoxPacket;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPacketData;

public class ClientboundRoomUserCountChangedPacket implements ISmartfoxPacket {

	public int roomID;
	public short userCount;
	public short spectatorCount = -1;

	@Override
	public ISmartfoxPacket createInstance() {
		return new ClientboundRoomUserCountChangedPacket();
	}

	@Override
	public short packetID() {
		return 1001;
	}

	@Override
	public void parse(SmartfoxPacketData packet) {
	}

	@Override
	public void build(SmartfoxPacketData packet) {
		packet.payload.setInt("r", roomID);
		packet.payload.setShort("uc", userCount);
		if (spectatorCount != -1)
			packet.payload.setShort("sc", spectatorCount);
	}

}
