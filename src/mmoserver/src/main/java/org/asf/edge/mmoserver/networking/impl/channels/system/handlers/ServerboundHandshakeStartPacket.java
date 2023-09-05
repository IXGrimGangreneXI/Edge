package org.asf.edge.mmoserver.networking.impl.channels.system.handlers;

import org.asf.edge.mmoserver.networking.packets.ISmartfoxPacket;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPacketData;

public class ServerboundHandshakeStartPacket implements ISmartfoxPacket {

	public String apiVersion;
	public String clientType;

	@Override
	public ISmartfoxPacket createInstance() {
		return new ServerboundHandshakeStartPacket();
	}

	@Override
	public short packetID() {
		return 0;
	}

	@Override
	public void parse(SmartfoxPacketData packet) {
		apiVersion = packet.payload.getString("api");
		clientType = packet.payload.getString("cl");
	}

	@Override
	public void build(SmartfoxPacketData packet) {
		packet.payload.setString("api", apiVersion);
		packet.payload.setString("cl", clientType);
	}

}
