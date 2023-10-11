package org.asf.edge.mmoserver.networking.channels.extensions;

import org.asf.edge.mmoserver.networking.channels.extensions.handlers.servertime.TimeSyncRequestHandler;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.servertime.ServerboundTimeSyncRequestMessage;
import org.asf.edge.mmoserver.networking.packets.ExtensionMessageChannel;

public class ServerTimeChannel extends ExtensionMessageChannel {

	@Override
	public ExtensionMessageChannel createInstance() {
		return new ServerTimeChannel();
	}

	@Override
	protected void registerMessages() {
		registerMessage(new ServerboundTimeSyncRequestMessage());
	}

	@Override
	protected void registerMessageHandlers() {
		registerHandler(new TimeSyncRequestHandler());
	}

}
