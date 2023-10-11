package org.asf.edge.mmoserver.networking.channels.extensions.messages.chat.moderation;

import org.asf.edge.mmoserver.networking.packets.ISmartfoxExtensionMessage;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

public class ClientboundChatMessageBlockedPacket implements ISmartfoxExtensionMessage {

	public String filterResult;

	@Override
	public ISmartfoxExtensionMessage createInstance() {
		return new ClientboundChatMessageBlockedPacket();
	}

	@Override
	public String messageID() {
		return "SCF";
	}

	@Override
	public void parse(SmartfoxPayload payload) {
		String[] arr = payload.getStringArray("arr");
		filterResult = arr[3];
	}

	@Override
	public void build(SmartfoxPayload payload) {
		String[] arr = new String[4];
		arr[0] = "SCF";
		arr[1] = "-1";
		arr[2] = "MB";
		arr[3] = filterResult;
		payload.setStringArray("arr", arr);
	}

}
