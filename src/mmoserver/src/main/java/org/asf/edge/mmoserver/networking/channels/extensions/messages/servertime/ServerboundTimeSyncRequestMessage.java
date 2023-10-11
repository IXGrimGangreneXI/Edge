package org.asf.edge.mmoserver.networking.channels.extensions.messages.servertime;

import org.asf.edge.mmoserver.networking.channels.extensions.ISodClientExtensionMessage;
import org.asf.edge.mmoserver.networking.packets.ISmartfoxExtensionMessage;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

public class ServerboundTimeSyncRequestMessage implements ISodClientExtensionMessage {

	@Override
	public ISmartfoxExtensionMessage createInstance() {
		return new ServerboundTimeSyncRequestMessage();
	}

	@Override
	public String extensionName() {
		return "";
	}

	@Override
	public String messageID() {
		return "PNG";
	}

	@Override
	public void parse(SmartfoxPayload payload) {
	}

	@Override
	public void build(SmartfoxPayload payload) {
	}

}
