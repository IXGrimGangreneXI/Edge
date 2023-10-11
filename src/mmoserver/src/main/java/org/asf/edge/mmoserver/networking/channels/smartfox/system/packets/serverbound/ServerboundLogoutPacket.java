package org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.serverbound;

import org.asf.edge.mmoserver.networking.packets.ISmartfoxPacket;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPacketData;

public class ServerboundLogoutPacket implements ISmartfoxPacket {

	@Override
	public ISmartfoxPacket createInstance() {
		return new ServerboundLogoutPacket();
	}

	@Override
	public short packetID() {
		return 26;
	}

	@Override
	public void parse(SmartfoxPacketData packet) {
	}

	@Override
	public void build(SmartfoxPacketData packet) {
	}

}
