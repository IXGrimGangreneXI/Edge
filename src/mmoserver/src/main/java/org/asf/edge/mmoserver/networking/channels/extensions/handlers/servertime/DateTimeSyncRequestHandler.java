package org.asf.edge.mmoserver.networking.channels.extensions.handlers.servertime;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.asf.edge.mmoserver.networking.channels.extensions.messages.servertime.ClientboundDateSyncMessage;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.servertime.ServerboundDateSyncRequestMessage;
import org.asf.edge.mmoserver.networking.packets.ExtensionMessageChannel;
import org.asf.edge.mmoserver.networking.packets.IExtensionMessageHandler;

public class DateTimeSyncRequestHandler implements IExtensionMessageHandler<ServerboundDateSyncRequestMessage> {

	@Override
	public Class<ServerboundDateSyncRequestMessage> messageClass() {
		return ServerboundDateSyncRequestMessage.class;
	}

	@Override
	public boolean handle(ServerboundDateSyncRequestMessage message, ExtensionMessageChannel channel)
			throws IOException {
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		ClientboundDateSyncMessage msg = new ClientboundDateSyncMessage();
		msg.dateStr = fmt.format(new Date(System.currentTimeMillis()));
		channel.sendMessage(msg);
		return true;
	}

}
