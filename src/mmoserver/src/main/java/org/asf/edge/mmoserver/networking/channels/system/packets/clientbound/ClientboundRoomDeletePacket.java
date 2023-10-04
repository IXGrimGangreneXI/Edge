package org.asf.edge.mmoserver.networking.channels.system.packets.clientbound;

import org.asf.edge.mmoserver.networking.packets.ISmartfoxPacket;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPacketData;

public class ClientboundRoomDeletePacket implements ISmartfoxPacket {

	public int roomID;

	@Override
	public ISmartfoxPacket createInstance() {
		return new ClientboundRoomDeletePacket();
	}

	@Override
	public short packetID() {
		return 1003;
	}

	@Override
	public void parse(SmartfoxPacketData packet) {
	}

	@Override
	public void build(SmartfoxPacketData packet) {
		packet.payload.setInt("r", roomID);
	}

}
