package org.asf.edge.mmoserver.networking.channels.extensions.handlers.servertime;

import java.io.IOException;

import org.asf.edge.mmoserver.networking.channels.extensions.messages.servertime.ClientboundTimeSyncMessage;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.servertime.ServerboundTimeSyncRequestMessage;
import org.asf.edge.mmoserver.networking.packets.ExtensionMessageChannel;
import org.asf.edge.mmoserver.networking.packets.IExtensionMessageHandler;

public class TimeSyncRequestHandler implements IExtensionMessageHandler<ServerboundTimeSyncRequestMessage> {

	@Override
	public Class<ServerboundTimeSyncRequestMessage> messageClass() {
		return ServerboundTimeSyncRequestMessage.class;
	}

	@Override
	public boolean handle(ServerboundTimeSyncRequestMessage message, ExtensionMessageChannel channel)
			throws IOException {
		ClientboundTimeSyncMessage msg = new ClientboundTimeSyncMessage();
		msg.timestamp = System.currentTimeMillis();
		channel.sendMessage(msg);
		return true;
	}

}
