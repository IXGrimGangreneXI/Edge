package org.asf.edge.mmoserver.networking.channels.smartfox;

import org.asf.edge.mmoserver.networking.channels.smartfox.system.handlers.LoginRequestHandler;
import org.asf.edge.mmoserver.networking.channels.smartfox.system.handlers.LogoutHandler;
import org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.serverbound.ServerboundLoginRequestPacket;
import org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.serverbound.ServerboundLogoutPacket;
import org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.serverbound.ServerboundSetRoomVariablePacket;
import org.asf.edge.mmoserver.networking.packets.PacketChannel;

public class SystemChannel extends PacketChannel {

	@Override
	public byte channelID() {
		return 0;
	}

	@Override
	public PacketChannel createInstance() {
		return new SystemChannel();
	}

	@Override
	protected void registerPackets() {
		registerPacket(new ServerboundLoginRequestPacket());
		registerPacket(new ServerboundSetRoomVariablePacket());
		registerPacket(new ServerboundLogoutPacket());
	}

	@Override
	protected void registerPacketHandlers() {
		registerHandler(new LoginRequestHandler());
		registerHandler(new LogoutHandler());
	}

}
