package org.asf.edge.mmoserver.networking.channels.extensions.handlers.uservars;

import java.io.IOException;

import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.mmoserver.entities.smartfox.RoomInfo;
import org.asf.edge.mmoserver.entities.smartfox.SfsUser;
import org.asf.edge.mmoserver.entities.smartfox.UserVariable;
import org.asf.edge.mmoserver.networking.SmartfoxClient;
import org.asf.edge.mmoserver.networking.channels.extensions.UserVarsChannel;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.uservars.ClientboundRefreshUserVarsMessage;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.uservars.ClientboundSetUserVarsMessage;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.uservars.ClientboundSetUserVarsMessage.UserVarUpdate;
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
					// Assign variables
					message.vars.forEach((key, val) -> {
						UserVariable var = usr.getVariable(key);
						if (var != null)
							var.setValue(val);
						else
							var = usr.addVariable(key, val);
					});

					// Broadcast update
					ClientboundSetUserVarsMessage update = new ClientboundSetUserVarsMessage();
					message.vars.forEach((key, val) -> {
						UserVarUpdate u = new UserVarUpdate();
						u.userID = usr.getUserNumericID();
						u.roomID = room.getRoomID();
						u.vars.put(key, val);
						update.varUpdates.add(u);
					});
					for (PlayerInfo plr : room.getPlayers()) {
						if (!plr.getSave().getSaveID().equals(player.getSave().getSaveID()))
							plr.getClient().getExtensionChannel(UserVarsChannel.class).sendMessage(update);
					}
					for (PlayerInfo plr : room.getSpectatorPlayers()) {
						if (!plr.getSave().getSaveID().equals(player.getSave().getSaveID()))
							plr.getClient().getExtensionChannel(UserVarsChannel.class).sendMessage(update);
					}

					// Broadcast refresh
					ClientboundRefreshUserVarsMessage ref = new ClientboundRefreshUserVarsMessage();
					ref.userID = player.getClient().getSessionNumericID();
					for (PlayerInfo plr : room.getPlayers()) {
						plr.getClient().getExtensionChannel(UserVarsChannel.class).sendMessage(ref);
					}
					for (PlayerInfo plr : room.getSpectatorPlayers()) {
						plr.getClient().getExtensionChannel(UserVarsChannel.class).sendMessage(ref);
					}
				}
			}
			for (RoomInfo room : player.getSpectatingRooms()) {
				SfsUser usr = room.getSfsUser(player.getSave().getSaveID());
				if (usr != null) {
					// Assign variables
					message.vars.forEach((key, val) -> {
						UserVariable var = usr.getVariable(key);
						if (var != null)
							var.setValue(val);
						else
							var = usr.addVariable(key, val);
					});

					// Broadcast update
					ClientboundSetUserVarsMessage update = new ClientboundSetUserVarsMessage();
					message.vars.forEach((key, val) -> {
						UserVarUpdate u = new UserVarUpdate();
						u.userID = usr.getUserNumericID();
						u.roomID = room.getRoomID();
						u.vars.put(key, val);
						update.varUpdates.add(u);
					});
					for (PlayerInfo plr : room.getPlayers()) {
						if (!plr.getSave().getSaveID().equals(player.getSave().getSaveID()))
							plr.getClient().getExtensionChannel(UserVarsChannel.class).sendMessage(update);
					}
					for (PlayerInfo plr : room.getSpectatorPlayers()) {
						if (!plr.getSave().getSaveID().equals(player.getSave().getSaveID()))
							plr.getClient().getExtensionChannel(UserVarsChannel.class).sendMessage(update);
					}

					// Broadcast refresh
					ClientboundRefreshUserVarsMessage ref = new ClientboundRefreshUserVarsMessage();
					ref.userID = player.getClient().getSessionNumericID();
					for (PlayerInfo plr : room.getPlayers()) {
						plr.getClient().getExtensionChannel(UserVarsChannel.class).sendMessage(ref);
					}
					for (PlayerInfo plr : room.getSpectatorPlayers()) {
						plr.getClient().getExtensionChannel(UserVarsChannel.class).sendMessage(ref);
					}
				}
			}

		}
		return true;
	}

}
