package org.asf.edge.mmoserver.networking.channels.extensions.messages.rooms;

import org.asf.edge.mmoserver.networking.channels.extensions.ISodClientExtensionMessage;
import org.asf.edge.mmoserver.networking.packets.ISmartfoxExtensionMessage;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

public class JoinOwnerRoomMessage implements ISodClientExtensionMessage {

	public String roomName;
	public String ownerID;

	@Override
	public ISmartfoxExtensionMessage createInstance() {
		return new JoinOwnerRoomMessage();
	}

	@Override
	public String extensionName() {
		return "le";
	}

	@Override
	public String messageID() {
		return "JO";
	}

	@Override
	public void parse(SmartfoxPayload payload) {
		roomName = payload.getString("rn");
		ownerID = payload.getString("0");
	}

	@Override
	public void build(SmartfoxPayload payload) {
		payload.setString("rn", roomName);
		payload.setString("0", ownerID);
	}

}
