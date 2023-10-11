package org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound;

import org.asf.edge.mmoserver.networking.packets.ISmartfoxPacket;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPacketData;

public class ClientboundHandshakeStartPacket implements ISmartfoxPacket {

	public String sessionToken;
	public int compressionThreshold;
	public int maxMessageSize;

	@Override
	public ISmartfoxPacket createInstance() {
		return new ClientboundHandshakeStartPacket();
	}

	@Override
	public short packetID() {
		return 0;
	}

	@Override
	public void parse(SmartfoxPacketData packet) {
		sessionToken = packet.payload.getString("tk");
		compressionThreshold = packet.payload.getInt("ct");
		maxMessageSize = packet.payload.getInt("ms");
	}

	@Override
	public void build(SmartfoxPacketData packet) {
		packet.payload.setString("tk", sessionToken);
		packet.payload.setInt("ct", compressionThreshold);
		packet.payload.setInt("ms", maxMessageSize);
	}

}
