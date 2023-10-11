package org.asf.edge.mmoserver.networking.channels.extensions;

import org.asf.edge.mmoserver.networking.channels.extensions.handlers.chat.ChatMessageHandler;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.chat.ServerboundSendChatMessagePacket;
import org.asf.edge.mmoserver.networking.packets.ExtensionMessageChannel;

public class ChatChannel extends ExtensionMessageChannel {

	@Override
	public ExtensionMessageChannel createInstance() {
		return new ChatChannel();
	}

	@Override
	protected void registerMessages() {
		registerMessage(new ServerboundSendChatMessagePacket());
	}

	@Override
	protected void registerMessageHandlers() {
		registerHandler(new ChatMessageHandler());
	}

}
