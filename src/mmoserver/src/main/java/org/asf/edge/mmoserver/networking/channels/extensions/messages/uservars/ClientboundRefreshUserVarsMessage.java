package org.asf.edge.mmoserver.networking.channels.extensions.messages.uservars;

import org.asf.edge.mmoserver.networking.channels.extensions.ISodClientExtensionMessage;
import org.asf.edge.mmoserver.networking.packets.ISmartfoxExtensionMessage;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

public class ClientboundRefreshUserVarsMessage implements ISodClientExtensionMessage {

	public int userID;

	@Override
	public ISmartfoxExtensionMessage createInstance() {
		return new ClientboundRefreshUserVarsMessage();
	}

	@Override
	public String extensionName() {
		return "we";
	}

	@Override
	public String messageID() {
		return "SUV";
	}

	@Override
	public void parse(SmartfoxPayload payload) {
		userID = payload.getInt("MID");
	}

	@Override
	public void build(SmartfoxPayload payload) {
		payload.setInt("MID", userID);
	}

}
