package org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.serverbound;

import org.asf.edge.mmoserver.networking.packets.ISmartfoxPacket;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPacketData;

public class ServerboundLoginRequestPacket implements ISmartfoxPacket {

	public String zoneName;
	public String sessionToken;
	public String productTokenHash;

	@Override
	public ISmartfoxPacket createInstance() {
		return new ServerboundLoginRequestPacket();
	}

	@Override
	public short packetID() {
		return 1;
	}

	@Override
	public void parse(SmartfoxPacketData packet) {
		zoneName = packet.payload.getString("zn");
		sessionToken = packet.payload.getString("un");
		productTokenHash = packet.payload.getString("pw");
	}

	@Override
	public void build(SmartfoxPacketData packet) {
		packet.payload.setString("zn", zoneName);
		packet.payload.setString("un", sessionToken);
		packet.payload.setString("pw", productTokenHash);
	}

}
