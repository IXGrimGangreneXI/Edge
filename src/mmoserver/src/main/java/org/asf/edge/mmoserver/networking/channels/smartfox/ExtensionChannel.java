package org.asf.edge.mmoserver.networking.channels.smartfox;

import org.asf.edge.mmoserver.networking.channels.smartfox.extension.handlers.ExtensionMessageHandler;
import org.asf.edge.mmoserver.networking.channels.smartfox.extension.packets.serverbound.ServerboundExtensionMessage;
import org.asf.edge.mmoserver.networking.packets.PacketChannel;

public class ExtensionChannel extends PacketChannel {

	@Override
	public byte channelID() {
		return 1;
	}

	@Override
	public PacketChannel createInstance() {
		return new ExtensionChannel();
	}

	@Override
	protected void registerPackets() {
		registerPacket(new ServerboundExtensionMessage());
	}

	@Override
	protected void registerPacketHandlers() {
		registerHandler(new ExtensionMessageHandler());
	}

}
