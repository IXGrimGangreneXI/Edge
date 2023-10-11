package org.asf.edge.mmoserver.networking.channels.extensions.messages.chat.moderation;

import org.asf.edge.mmoserver.networking.packets.ISmartfoxExtensionMessage;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

public class ClientboundModerationPacket implements ISmartfoxExtensionMessage {

	public String moderationMessage;

	public boolean isMute;
	public int muteTimeMinutes;

	@Override
	public ISmartfoxExtensionMessage createInstance() {
		return new ClientboundModerationPacket();
	}

	@Override
	public String messageID() {
		return "SMM";
	}

	@Override
	public void parse(SmartfoxPayload payload) {
		String[] arr = payload.getStringArray("arr");
		isMute = arr[2].equals("SILENCE");
		moderationMessage = arr[3];
		if (isMute)
			muteTimeMinutes = Integer.parseInt(arr[4]);
	}

	@Override
	public void build(SmartfoxPayload payload) {
		String[] arr = new String[isMute ? 5 : 4];
		arr[0] = "SMM";
		arr[1] = "-1";
		arr[2] = isMute ? "SILENCE" : "UNSILENCED";
		arr[3] = moderationMessage;
		if (isMute)
			arr[4] = Integer.toString(muteTimeMinutes);
		payload.setStringArray("arr", arr);
	}

}
