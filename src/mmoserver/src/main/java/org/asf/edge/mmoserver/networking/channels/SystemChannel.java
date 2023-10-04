package org.asf.edge.mmoserver.networking.channels;

import org.asf.edge.mmoserver.networking.channels.system.handlers.LoginRequestHandler;
import org.asf.edge.mmoserver.networking.channels.system.packets.serverbound.ServerboundLoginRequestPacket;
import org.asf.edge.mmoserver.networking.channels.system.packets.serverbound.ServerboundSetRoomVariablePacket;
import org.asf.edge.mmoserver.networking.packets.AbstractPacketChannel;

public class SystemChannel extends AbstractPacketChannel {

	@Override
	public byte channelID() {
		return 0;
	}

	@Override
	public AbstractPacketChannel createInstance() {
		return new SystemChannel();
	}

	@Override
	protected void registerPackets() {
		registerPacket(new ServerboundLoginRequestPacket());
		registerPacket(new ServerboundSetRoomVariablePacket());
	}

	@Override
	protected void registerPacketHandlers() {
		registerHandler(new LoginRequestHandler());
	}

}
