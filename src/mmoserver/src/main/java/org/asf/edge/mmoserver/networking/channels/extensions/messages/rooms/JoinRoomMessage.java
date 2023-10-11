package org.asf.edge.mmoserver.networking.channels.extensions.messages.rooms;

import org.asf.edge.mmoserver.networking.channels.extensions.ISodClientExtensionMessage;
import org.asf.edge.mmoserver.networking.packets.ISmartfoxExtensionMessage;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

public class JoinRoomMessage implements ISodClientExtensionMessage {

	public String roomName;

	@Override
	public ISmartfoxExtensionMessage createInstance() {
		return new JoinRoomMessage();
	}

	@Override
	public String extensionName() {
		return "le";
	}

	@Override
	public String messageID() {
		return "JA";
	}

	@Override
	public void parse(SmartfoxPayload payload) {
		roomName = payload.getString("rn");
	}

	@Override
	public void build(SmartfoxPayload payload) {
		payload.setString("rn", roomName);
	}

}
