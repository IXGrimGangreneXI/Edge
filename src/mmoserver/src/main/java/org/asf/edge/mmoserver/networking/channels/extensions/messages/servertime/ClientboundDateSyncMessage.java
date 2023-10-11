package org.asf.edge.mmoserver.networking.channels.extensions.messages.servertime;

import org.asf.edge.mmoserver.networking.packets.ISmartfoxExtensionMessage;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

public class ClientboundDateSyncMessage implements ISmartfoxExtensionMessage {

	public String dateStr;

	@Override
	public ISmartfoxExtensionMessage createInstance() {
		return new ClientboundDateSyncMessage();
	}

	@Override
	public String messageID() {
		return "DT";
	}

	@Override
	public void parse(SmartfoxPayload payload) {
		dateStr = payload.getObjectArray("arr")[1].toString();
	}

	@Override
	public void build(SmartfoxPayload payload) {
		payload.setStringArray("arr", new String[] { "DT", dateStr });
	}

}
