package org.asf.edge.mmoserver.networking.channels.extensions.messages.chat;

import org.asf.edge.mmoserver.networking.packets.ISmartfoxExtensionMessage;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

public class ClientboundSendChatMessageSuccessPacket implements ISmartfoxExtensionMessage {

	public String message;
	public String clanID;

	public boolean isClanChat = false;

	@Override
	public ISmartfoxExtensionMessage createInstance() {
		return new ClientboundSendChatMessageSuccessPacket();
	}

	@Override
	public String messageID() {
		return "SCA";
	}

	@Override
	public void parse(SmartfoxPayload payload) {
		String[] arr = payload.getStringArray("arr");
		message = arr[3];
		if (arr[4] != null && !arr[4].isEmpty()) {
			isClanChat = true;
			clanID = arr[4];
		}
	}

	@Override
	public void build(SmartfoxPayload payload) {
		String[] arr = new String[6];
		arr[0] = "SCA";
		arr[1] = "-1";
		arr[2] = "1";
		arr[3] = message;
		arr[4] = isClanChat ? clanID : "";
		arr[5] = "1";
		payload.setStringArray("arr", arr);
	}

}
