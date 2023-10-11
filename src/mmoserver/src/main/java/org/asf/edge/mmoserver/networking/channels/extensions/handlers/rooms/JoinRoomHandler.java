package org.asf.edge.mmoserver.networking.channels.extensions.handlers.rooms;

import java.io.IOException;

import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.mmoserver.networking.SmartfoxClient;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.rooms.JoinRoomMessage;
import org.asf.edge.mmoserver.networking.packets.ExtensionMessageChannel;
import org.asf.edge.mmoserver.networking.packets.IExtensionMessageHandler;

public class JoinRoomHandler implements IExtensionMessageHandler<JoinRoomMessage> {

	@Override
	public Class<JoinRoomMessage> messageClass() {
		return JoinRoomMessage.class;
	}

	@Override
	public boolean handle(JoinRoomMessage message, ExtensionMessageChannel channel) throws IOException {
		SmartfoxClient client = channel.getClient();
		PlayerInfo player = client.getObject(PlayerInfo.class);
		if (player != null) {
			// Join room
			player.joinMmoRoom(message.roomName);
		}
		return true;
	}

}
