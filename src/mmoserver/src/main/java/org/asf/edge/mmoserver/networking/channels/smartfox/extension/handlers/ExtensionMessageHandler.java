package org.asf.edge.mmoserver.networking.channels.smartfox.extension.handlers;

import java.io.IOException;

import org.asf.edge.mmoserver.networking.packets.PacketChannel;
import org.asf.edge.mmoserver.networking.channels.smartfox.extension.packets.serverbound.ServerboundExtensionMessage;
import org.asf.edge.mmoserver.networking.packets.IPacketHandler;

public class ExtensionMessageHandler implements IPacketHandler<ServerboundExtensionMessage> {

	@Override
	public Class<ServerboundExtensionMessage> packetClass() {
		return ServerboundExtensionMessage.class;
	}

	@Override
	public boolean handle(ServerboundExtensionMessage packet, PacketChannel channel) throws IOException {
		return channel.getClient().handleExtension(packet);
	}

}
