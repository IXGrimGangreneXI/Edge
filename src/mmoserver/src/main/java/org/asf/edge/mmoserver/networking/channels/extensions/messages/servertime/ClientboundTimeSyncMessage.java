package org.asf.edge.mmoserver.networking.channels.extensions.messages.servertime;

import org.asf.edge.mmoserver.networking.packets.ISmartfoxExtensionMessage;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

public class ClientboundTimeSyncMessage implements ISmartfoxExtensionMessage {

	public long timestamp;

	@Override
	public ISmartfoxExtensionMessage createInstance() {
		return new ClientboundTimeSyncMessage();
	}

	@Override
	public String messageID() {
		return "PNG";
	}

	@Override
	public void parse(SmartfoxPayload payload) {
		timestamp = Long.parseLong(payload.getObjectArray("arr")[1].toString());
	}

	@Override
	public void build(SmartfoxPayload payload) {
		payload.setObjectArray("arr", new Object[] { "PNG", timestamp });
	}

}
