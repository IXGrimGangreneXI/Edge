package org.asf.edge.mmoserver.networking.channels.extensions.handlers.chat;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.asf.edge.common.commands.CommandContext;
import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.services.textfilter.FilterSeverity;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.common.services.textfilter.result.FilterResult;
import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.mmoserver.entities.smartfox.RoomInfo;
import org.asf.edge.mmoserver.events.chat.PlayerChatMessageSentEvent;
import org.asf.edge.mmoserver.networking.SmartfoxClient;
import org.asf.edge.mmoserver.networking.channels.extensions.ChatChannel;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.chat.ClientboundPostChatMessagePacket;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.chat.ClientboundSendChatMessageSuccessPacket;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.chat.ServerboundSendChatMessagePacket;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.chat.moderation.ClientboundChatMutePacket;
import org.asf.edge.mmoserver.networking.packets.ExtensionMessageChannel;
import org.asf.edge.mmoserver.networking.packets.IExtensionMessageHandler;
import org.asf.edge.modules.eventbus.EventBus;

import com.google.gson.JsonPrimitive;

public class ChatMessageHandler implements IExtensionMessageHandler<ServerboundSendChatMessagePacket> {

	@Override
	public Class<ServerboundSendChatMessagePacket> messageClass() {
		return ServerboundSendChatMessagePacket.class;
	}

	@Override
	public boolean handle(ServerboundSendChatMessagePacket message, ExtensionMessageChannel channel)
			throws IOException {
		SmartfoxClient client = channel.getClient();
		PlayerInfo player = client.getObject(PlayerInfo.class);
		if (player != null) {
			// Check message
			message.message = message.message.trim();
			if (message.message.isBlank())
				return true; // Empty

			// Check command
			if (message.message.startsWith(">")) {
				// Command
				String cmd = message.message.substring(1);
				cmd = cmd.trim();

				// Get command context
				CommandContext ctx = player.getClient().getObject(CommandContext.class);
				if (ctx == null) {
					ctx = CommandContext.getFor(player.getAccount());
					ctx.enableCommandLogging();
					player.getClient().setObject(CommandContext.class, ctx);
				}

				// Run command
				ctx.runCommand(cmd, t -> {
					// Send message
					ClientboundPostChatMessagePacket pkt = new ClientboundPostChatMessagePacket();
					pkt.message = t;
					pkt.userID = "";
					pkt.displayName = "[EDGE]";
					try {
						player.getClient().getExtensionChannel(ChatChannel.class).sendMessage(pkt);
					} catch (IOException e) {
					}
				});

				// Return
				return true;
			}

			// Check chat enabled
			if (!player.getAccount().isChatEnabled() || player.getAccount().isGuestAccount()) {
				// Send failure
				ClientboundChatMutePacket msg = new ClientboundChatMutePacket();
				msg.filterResult = "";
				msg.muteMessage = "Chat is not enabled for your account";
				msg.muteTimeMinutes = 0;
				channel.sendMessage(msg);
				return true;
			}

			// Filter message
			TextFilterService filter = TextFilterService.getInstance();
			FilterResult res = filter.filter(message.message, player.getAccount().isStrictChatFilterEnabled());

			// Load moderation data
			AccountKvDataContainer data = player.getAccount().getAccountKeyValueContainer();
			data = data.getChildContainer("moderation");

			// Check result
			if (res.getSeverity() == FilterSeverity.INSTAMUTE) {
				// Mute
				if (!data.entryExists("isMuted") || !data.getEntry("isMuted").getAsBoolean()) {
					// Create mute entry
					// Mute for 30 minutes
					long unmute = System.currentTimeMillis() + (30 * 60 * 1000);
					String reason = "Illegal word in chat";

					// Set mute
					data.setEntry("muteReason", new JsonPrimitive(reason));
					data.setEntry("unmuteTimestamp", new JsonPrimitive(unmute));
					data.setEntry("isMuted", new JsonPrimitive(true));

					// Dispatch moderation event
					// TODO: dispatch event when moderation comes in place
					data = data;
				}
			}

			// Check mute
			if (data.entryExists("isMuted") && data.getEntry("isMuted").getAsBoolean()) {
				// Check unmute
				long unmute = -1;
				if (data.entryExists("unmuteTimestamp"))
					unmute = data.getEntry("unmuteTimestamp").getAsLong();

				// Load reason
				String reasonStr = "";
				if (data.entryExists("muteReason") && !data.getEntry("muteReason").getAsString().isEmpty())
					reasonStr = "\n\nReason: " + data.getEntry("muteReason").getAsString();

				// Check
				if (unmute == -1 || System.currentTimeMillis() < unmute) {
					// Send failure
					ClientboundChatMutePacket msg = new ClientboundChatMutePacket();
					msg.filterResult = "";
					if (unmute != -1) {
						msg.muteMessage = "You have been muted and cannot speak in chats, you will be unmuted in {{bannedtime}}."
								+ reasonStr;
						msg.muteTimeMinutes = (int) ((unmute - System.currentTimeMillis()) / 1000 / 60);
					} else
						msg.muteMessage = "You have been muted and cannot speak in chats." + reasonStr;
					channel.sendMessage(msg);
					return true;
				}
			}

			// Update message
			message.message = res.getFilterResult();

			// Check clan
			if (message.isClanChat) {
				// Check clan
				// TODO: clans
				message = message;

				// Skip as the user is not in the clan
				return true;
			}

			// Dispatch event
			PlayerChatMessageSentEvent ev = new PlayerChatMessageSentEvent(player, message.message, message.clanID);
			EventBus.getInstance().dispatchEvent(ev);
			if (ev.isCancelled())
				return true;

			// Log to server log
			if (!message.isClanChat)
				LogManager.getLogger("Chat").info("Chat: " + player.getSave().getUsername() + ": " + message.message);
			else {
				String clanName = message.clanID;
				// TODO: load clan name
				LogManager.getLogger("Chat").info(
						"Clan chat (" + clanName + "): " + player.getSave().getUsername() + ": " + message.message);
			}

			// Log to chat log
			// TODO: write to chat log for moderation

			// Send success
			ClientboundSendChatMessageSuccessPacket success = new ClientboundSendChatMessageSuccessPacket();
			success.message = message.message;
			success.isClanChat = message.isClanChat;
			success.clanID = message.clanID;
			channel.sendMessage(success);

			// Find rooms
			for (RoomInfo room : player.getJoinedRooms()) {
				// Send chat message
				for (PlayerInfo plr : room.getPlayers()) {
					// Skip self
					if (plr.getSave().getSaveID().equals(player.getSave().getSaveID()))
						continue;

					// Check clan
					if (message.isClanChat) {
						// Check clan
						// TODO: clans
						message = message;

						// Skip as the user is not in the clan
						continue;
					}

					// Filter based on settings
					String filtered = filter.filterString(message.message,
							plr.getAccount().isStrictChatFilterEnabled());

					// Send message
					ClientboundPostChatMessagePacket pkt = new ClientboundPostChatMessagePacket();
					pkt.message = filtered;
					pkt.isClanChat = message.isClanChat;
					pkt.clanID = message.clanID;
					pkt.userID = player.getSave().getSaveID();
					pkt.displayName = player.getSave().getUsername();
					plr.getClient().getExtensionChannel(ChatChannel.class).sendMessage(pkt);
				}
			}
		}

		// Return
		return true;
	}

}
