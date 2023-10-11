package org.asf.edge.mmoserver.networking.channels.extensions;

import org.asf.edge.mmoserver.networking.packets.ExtensionMessageChannel;

public class RoomChannel extends ExtensionMessageChannel {

	@Override
	public ExtensionMessageChannel createInstance() {
		return new RoomChannel();
	}

	@Override
	protected void registerMessages() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void registerMessageHandlers() {
		// TODO Auto-generated method stub

	}

}
