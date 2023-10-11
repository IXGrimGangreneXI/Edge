package org.asf.edge.mmoserver.networking.channels.extensions.handlers.uservars;

import java.io.IOException;

import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.mmoserver.entities.smartfox.RoomInfo;
import org.asf.edge.mmoserver.entities.smartfox.SfsUser;
import org.asf.edge.mmoserver.entities.smartfox.UserVariable;
import org.asf.edge.mmoserver.networking.SmartfoxClient;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.uservars.ServerboundSetUserVarsMessage;
import org.asf.edge.mmoserver.networking.packets.ExtensionMessageChannel;
import org.asf.edge.mmoserver.networking.packets.IExtensionMessageHandler;

public class SetUserVarsHandler implements IExtensionMessageHandler<ServerboundSetUserVarsMessage> {

	@Override
	public Class<ServerboundSetUserVarsMessage> messageClass() {
		return ServerboundSetUserVarsMessage.class;
	}

	@Override
	public boolean handle(ServerboundSetUserVarsMessage message, ExtensionMessageChannel channel) throws IOException {
		SmartfoxClient client = channel.getClient();
		PlayerInfo player = client.getObject(PlayerInfo.class);
		if (player != null) {
			// Apply to each room
			for (RoomInfo room : player.getJoinedRooms()) {
				SfsUser usr = room.getSfsUser(player.getSave().getSaveID());
				if (usr != null) {
					message.vars.forEach((key, val) -> {
						UserVariable var = usr.getVariable(key);
						if (var != null)
							var.setValue(val);
						else
							var = usr.addVariable(key, val);
					});
				}
			}
			for (RoomInfo room : player.getSpectatingRooms()) {
				SfsUser usr = room.getSfsUser(player.getSave().getSaveID());
				if (usr != null) {
					message.vars.forEach((key, val) -> {
						UserVariable var = usr.getVariable(key);
						if (var != null)
							var.setValue(val);
						else
							var = usr.addVariable(key, val);
					});
				}
			}
		}
		return true;
	}

}
