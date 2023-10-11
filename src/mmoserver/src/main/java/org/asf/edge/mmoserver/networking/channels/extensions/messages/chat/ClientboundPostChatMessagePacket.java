package org.asf.edge.mmoserver.networking.channels.extensions.messages.chat;

import org.asf.edge.mmoserver.networking.packets.ISmartfoxExtensionMessage;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

public class ClientboundPostChatMessagePacket implements ISmartfoxExtensionMessage {

	public String message;
	public String userID;
	public String clanID;
	public String displayName;

	public boolean isClanChat = false;

	@Override
	public ISmartfoxExtensionMessage createInstance() {
		return new ClientboundPostChatMessagePacket();
	}

	@Override
	public String messageID() {
		return "CMR";
	}

	@Override
	public void parse(SmartfoxPayload payload) {
		String[] arr = payload.getStringArray("arr");
		userID = arr[2];
		message = arr[4];
		if (arr[5] != null && !arr[5].isEmpty()) {
			isClanChat = true;
			clanID = arr[5];
		}
		displayName = arr[7];
	}

	@Override
	public void build(SmartfoxPayload payload) {
		String[] arr = new String[8];
		arr[0] = "CMR";
		arr[1] = "-1";
		arr[2] = userID;
		arr[3] = "1";
		arr[4] = message;
		arr[5] = isClanChat ? clanID : "";
		arr[6] = "1";
		arr[7] = displayName;
		payload.setStringArray("arr", arr);
	}

}
