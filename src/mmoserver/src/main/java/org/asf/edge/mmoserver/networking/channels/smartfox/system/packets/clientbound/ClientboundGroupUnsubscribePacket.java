package org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound;

import org.asf.edge.mmoserver.networking.packets.ISmartfoxPacket;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPacketData;

public class ClientboundGroupUnsubscribePacket implements ISmartfoxPacket {

	public String groupID;

	@Override
	public ISmartfoxPacket createInstance() {
		return new ClientboundGroupUnsubscribePacket();
	}

	@Override
	public short packetID() {
		return 16;
	}

	@Override
	public void parse(SmartfoxPacketData packet) {
	}

	@Override
	public void build(SmartfoxPacketData packet) {
		packet.payload.setString("g", groupID);
	}

}
