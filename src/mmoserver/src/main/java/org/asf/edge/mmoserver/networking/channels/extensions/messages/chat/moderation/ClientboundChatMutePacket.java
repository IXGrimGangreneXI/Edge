package org.asf.edge.mmoserver.networking.channels.extensions.messages.chat.moderation;

import org.asf.edge.mmoserver.networking.packets.ISmartfoxExtensionMessage;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

public class ClientboundChatMutePacket implements ISmartfoxExtensionMessage {

	public String filterResult;
	public String muteMessage;
	public int muteTimeMinutes;

	@Override
	public ISmartfoxExtensionMessage createInstance() {
		return new ClientboundChatMutePacket();
	}

	@Override
	public String messageID() {
		return "SCF";
	}

	@Override
	public void parse(SmartfoxPayload payload) {
		String[] arr = payload.getStringArray("arr");
		filterResult = arr[3];
		muteMessage = arr[4];
		muteTimeMinutes = Integer.parseInt(arr[5]);
	}

	@Override
	public void build(SmartfoxPayload payload) {
		String[] arr = new String[6];
		arr[0] = "SCF";
		arr[1] = "-1";
		arr[2] = "CB";
		arr[3] = filterResult;
		arr[4] = muteMessage;
		arr[5] = Integer.toString(muteTimeMinutes);
		payload.setStringArray("arr", arr);
	}

}
