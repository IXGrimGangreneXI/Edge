package org.asf.edge.mmoserver.networking.channels.extensions;

import org.asf.edge.mmoserver.networking.packets.ExtensionMessageChannel;

public class ChatChannel extends ExtensionMessageChannel {

	@Override
	public ExtensionMessageChannel createInstance() {
		return new ChatChannel();
	}

	@Override
	protected void registerMessages() {
	}

	@Override
	protected void registerMessageHandlers() {
	}

}
