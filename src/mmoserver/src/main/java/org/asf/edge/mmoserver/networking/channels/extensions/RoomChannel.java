package org.asf.edge.mmoserver.networking.channels.extensions;

import org.asf.edge.mmoserver.networking.channels.extensions.handlers.rooms.JoinRoomHandler;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.rooms.JoinRoomMessage;
import org.asf.edge.mmoserver.networking.packets.ExtensionMessageChannel;

public class RoomChannel extends ExtensionMessageChannel {

	@Override
	public ExtensionMessageChannel createInstance() {
		return new RoomChannel();
	}

	@Override
	protected void registerMessages() {
		registerMessage(new JoinRoomMessage());
	}

	@Override
	protected void registerMessageHandlers() {
		registerHandler(new JoinRoomHandler());
	}

}
