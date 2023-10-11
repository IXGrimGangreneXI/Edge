package org.asf.edge.mmoserver.networking.channels.extensions;

import org.asf.edge.mmoserver.networking.channels.extensions.handlers.uservars.SetUserVarsHandler;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.uservars.ServerboundSetUserVarsMessage;
import org.asf.edge.mmoserver.networking.packets.ExtensionMessageChannel;

public class UserVarsChannel extends ExtensionMessageChannel {

	@Override
	public ExtensionMessageChannel createInstance() {
		return new UserVarsChannel();
	}

	@Override
	protected void registerMessages() {
		registerMessage(new ServerboundSetUserVarsMessage());
	}

	@Override
	protected void registerMessageHandlers() {
		registerHandler(new SetUserVarsHandler());
	}

}
