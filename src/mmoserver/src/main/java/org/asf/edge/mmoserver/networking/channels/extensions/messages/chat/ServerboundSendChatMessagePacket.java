package org.asf.edge.mmoserver.networking.channels.extensions.messages.chat;

import org.asf.edge.mmoserver.networking.channels.extensions.ISodClientExtensionMessage;
import org.asf.edge.mmoserver.networking.packets.ISmartfoxExtensionMessage;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

public class ServerboundSendChatMessagePacket implements ISodClientExtensionMessage {

	public String message;
	public String clanID;
	public boolean isClanChat = false;

	@Override
	public ISmartfoxExtensionMessage createInstance() {
		return new ServerboundSendChatMessagePacket();
	}

	@Override
	public String messageID() {
		return "SCM";
	}

	@Override
	public String extensionName() {
		return "che";
	}

	@Override
	public void parse(SmartfoxPayload payload) {
		message = payload.getString("chm");
		if (payload.has("tgid")) {
			isClanChat = true;
			clanID = payload.getString("tgid");
		}
	}

	@Override
	public void build(SmartfoxPayload payload) {
		payload.setInt("cty", 1);
		payload.setString("chm", message);
		if (isClanChat)
			payload.setString("tgid", clanID);
	}

}
