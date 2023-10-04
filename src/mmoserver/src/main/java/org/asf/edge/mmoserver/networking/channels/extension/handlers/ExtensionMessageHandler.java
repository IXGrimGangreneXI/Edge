package org.asf.edge.mmoserver.networking.channels.extension.handlers;

import java.io.IOException;

import org.asf.edge.mmoserver.networking.channels.extension.packets.serverbound.ServerboundExtensionMessage;
import org.asf.edge.mmoserver.networking.packets.AbstractPacketChannel;
import org.asf.edge.mmoserver.networking.packets.IPacketHandler;

public class ExtensionMessageHandler implements IPacketHandler<ServerboundExtensionMessage> {

	@Override
	public Class<ServerboundExtensionMessage> packetClass() {
		return ServerboundExtensionMessage.class;
	}

	@Override
	public boolean handle(ServerboundExtensionMessage packet, AbstractPacketChannel channel) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

}
