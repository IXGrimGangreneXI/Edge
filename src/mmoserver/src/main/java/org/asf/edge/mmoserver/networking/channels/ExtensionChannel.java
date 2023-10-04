package org.asf.edge.mmoserver.networking.channels;

import org.asf.edge.mmoserver.networking.channels.extension.handlers.ExtensionMessageHandler;
import org.asf.edge.mmoserver.networking.channels.extension.packets.serverbound.ServerboundExtensionMessage;
import org.asf.edge.mmoserver.networking.packets.AbstractPacketChannel;

public class ExtensionChannel extends AbstractPacketChannel {

	@Override
	public byte channelID() {
		return 1;
	}

	@Override
	public AbstractPacketChannel createInstance() {
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
