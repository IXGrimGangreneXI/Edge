package org.asf.edge.mmoserver.networking.channels.extensions.messages.servertime;

import org.asf.edge.mmoserver.networking.channels.extensions.ISodClientExtensionMessage;
import org.asf.edge.mmoserver.networking.packets.ISmartfoxExtensionMessage;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

public class ServerboundDateSyncRequestMessage implements ISodClientExtensionMessage {

	@Override
	public ISmartfoxExtensionMessage createInstance() {
		return new ServerboundDateSyncRequestMessage();
	}

	@Override
	public String extensionName() {
		return "";
	}

	@Override
	public String messageID() {
		return "DT";
	}

	@Override
	public void parse(SmartfoxPayload payload) {
	}

	@Override
	public void build(SmartfoxPayload payload) {
	}

}
