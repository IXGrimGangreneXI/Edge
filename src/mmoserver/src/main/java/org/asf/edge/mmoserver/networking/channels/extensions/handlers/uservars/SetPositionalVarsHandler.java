package org.asf.edge.mmoserver.networking.channels.extensions.handlers.uservars;

import java.io.IOException;

import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.mmoserver.entities.positional.PositionalVariableContainer;
import org.asf.edge.mmoserver.entities.smartfox.RoomInfo;
import org.asf.edge.mmoserver.entities.smartfox.SfsUser;
import org.asf.edge.mmoserver.networking.SmartfoxClient;
import org.asf.edge.mmoserver.networking.channels.extensions.UserVarsChannel;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.uservars.ClientboundSetPositionalVarsMessage;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.uservars.ServerboundSetPositionalVarsMessage;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.uservars.ClientboundSetPositionalVarsMessage.UserVarUpdate;
import org.asf.edge.mmoserver.networking.packets.ExtensionMessageChannel;
import org.asf.edge.mmoserver.networking.packets.IExtensionMessageHandler;

public class SetPositionalVarsHandler implements IExtensionMessageHandler<ServerboundSetPositionalVarsMessage> {

	@Override
	public Class<ServerboundSetPositionalVarsMessage> messageClass() {
		return ServerboundSetPositionalVarsMessage.class;
	}

	@Override
	public boolean handle(ServerboundSetPositionalVarsMessage message, ExtensionMessageChannel channel)
			throws IOException {
		SmartfoxClient client = channel.getClient();
		PlayerInfo player = client.getObject(PlayerInfo.class);
		if (player != null) {
			// Apply to each room
			for (RoomInfo room : player.getJoinedRooms()) {
				SfsUser usr = room.getSfsUser(player.getSave().getSaveID());
				if (usr != null) {
					// Apply variables
					PositionalVariableContainer varCont = usr.getObject(PositionalVariableContainer.class);
					if (varCont == null)
						varCont = new PositionalVariableContainer();

					// Apply variables
					varCont.positionalVariables.putAll(message.vars);

					// Save
					usr.setObject(PositionalVariableContainer.class, varCont);

					// Broadcast update
					ClientboundSetPositionalVarsMessage update = new ClientboundSetPositionalVarsMessage();
					UserVarUpdate u = new UserVarUpdate();
					u.userID = usr.getUserNumericID();
					message.vars.forEach((key, val) -> {
						u.vars.put(key, val);
					});
					update.varUpdates.add(u);
					for (PlayerInfo plr : room.getPlayers()) {
						if (!plr.getSave().getSaveID().equals(player.getSave().getSaveID()))
							plr.getClient().getExtensionChannel(UserVarsChannel.class).sendMessage(update);
					}
					for (PlayerInfo plr : room.getSpectatorPlayers()) {
						if (!plr.getSave().getSaveID().equals(player.getSave().getSaveID()))
							plr.getClient().getExtensionChannel(UserVarsChannel.class).sendMessage(update);
					}
				}
			}
			for (RoomInfo room : player.getSpectatingRooms()) {
				SfsUser usr = room.getSfsUser(player.getSave().getSaveID());
				if (usr != null) {
					// Apply variables
					PositionalVariableContainer varCont = usr.getObject(PositionalVariableContainer.class);
					if (varCont == null)
						varCont = new PositionalVariableContainer();

					// Apply variables
					varCont.positionalVariables.putAll(message.vars);

					// Save
					usr.setObject(PositionalVariableContainer.class, varCont);

					// Broadcast update
					ClientboundSetPositionalVarsMessage update = new ClientboundSetPositionalVarsMessage();
					UserVarUpdate u = new UserVarUpdate();
					u.userID = usr.getUserNumericID();
					message.vars.forEach((key, val) -> {
						u.vars.put(key, val);
					});
					update.varUpdates.add(u);
					for (PlayerInfo plr : room.getPlayers()) {
						if (!plr.getSave().getSaveID().equals(player.getSave().getSaveID()))
							plr.getClient().getExtensionChannel(UserVarsChannel.class).sendMessage(update);
					}
					for (PlayerInfo plr : room.getSpectatorPlayers()) {
						if (!plr.getSave().getSaveID().equals(player.getSave().getSaveID()))
							plr.getClient().getExtensionChannel(UserVarsChannel.class).sendMessage(update);
					}
				}
			}
		}
		return true;
	}

}
