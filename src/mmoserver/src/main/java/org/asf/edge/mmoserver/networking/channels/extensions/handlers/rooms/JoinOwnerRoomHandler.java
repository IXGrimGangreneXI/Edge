package org.asf.edge.mmoserver.networking.channels.extensions.handlers.rooms;

import java.io.IOException;

import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.mmoserver.networking.SmartfoxClient;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.rooms.JoinOwnerRoomMessage;
import org.asf.edge.mmoserver.networking.packets.ExtensionMessageChannel;
import org.asf.edge.mmoserver.networking.packets.IExtensionMessageHandler;

public class JoinOwnerRoomHandler implements IExtensionMessageHandler<JoinOwnerRoomMessage> {

	@Override
	public Class<JoinOwnerRoomMessage> messageClass() {
		return JoinOwnerRoomMessage.class;
	}

	@Override
	public boolean handle(JoinOwnerRoomMessage message, ExtensionMessageChannel channel) throws IOException {
		SmartfoxClient client = channel.getClient();
		PlayerInfo player = client.getObject(PlayerInfo.class);
		if (player != null) {
			// Join room
			player.joinMmoRoom(message.roomName, message.ownerID);
		}
		return true;
	}

}
