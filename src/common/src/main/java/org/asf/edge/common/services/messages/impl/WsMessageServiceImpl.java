package org.asf.edge.common.services.messages.impl;

import java.io.IOException;
import java.util.ArrayList;

import org.asf.edge.common.entities.messages.WsMessage;
import org.asf.edge.common.entities.messages.defaultmessages.WsRankMessage;
import org.asf.edge.common.events.achievements.RankChangedEvent;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.achievements.AchievementManager;
import org.asf.edge.common.services.messages.PlayerMessenger;
import org.asf.edge.common.services.messages.WsMessageService;
import org.asf.edge.common.xmls.messages.MessageInfoData;
import org.asf.edge.modules.eventbus.EventListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class WsMessageServiceImpl extends WsMessageService {

	@EventListener
	public void onRankChange(RankChangedEvent event) throws IOException {
		AccountObject acc = event.getAccount();

		// Create rank change message
		int rankType = event.getEntityRank().getTypeID().getPointTypeID();
		if ((rankType == 1 || rankType == 8) && event.hasRankChanged()) {
			// Prepare message
			WsRankMessage msg = new WsRankMessage();
			msg.rankType = rankType;
			switch (rankType) {

			// Viking
			case 1: {
				msg.particle = "PfPlayerLevelUpFxDO";
				msg.levelUpMessage = new String[] { "You've leveled up to Rank "
						+ (AchievementManager.getInstance().getRankIndex(event.getNewRank()) + 1) };
				break;
			}

			// Dragon
			case 8: {
				msg.particle = "PfDragonLevelUpFxDO";
				msg.levelUpMessage = new String[] { "Your dragon bond with {{PetName}} the {{PetType}} is now Rank "
						+ (AchievementManager.getInstance().getRankIndex(event.getNewRank()) + 1) };
				break;
			}

			}

			// Send message
			getMessengerFor(acc).sendSessionMessage(msg);
		}
	}

	private class PlayerMessengerImpl extends PlayerMessenger {
		public AccountObject account;
		public AccountSaveContainer save;

		@Override
		public WsMessage[] getQueuedMessages(boolean unreadOnly) throws IOException {
			ArrayList<WsMessage> messages = new ArrayList<WsMessage>();

			if (save != null) {
				// Load container
				AccountDataContainer messagesContainer = save.getSaveData().getChildContainer("wsservicemessages");

				// Find messages
				for (String key : messagesContainer.getEntryKeys()) {
					if (key.startsWith("msg-")) {
						// Parse ID
						int messageID = Integer.parseInt(key.substring(4));

						// Read json
						JsonObject msgD = messagesContainer.getEntry(key).getAsJsonObject();
						if (unreadOnly && !msgD.get("isRead").getAsBoolean()) {
							// Load message
							MessageInfoData msg = new ObjectMapper().readValue(msgD.get("messageObject").toString(),
									MessageInfoData.class);
							msg.messageQueueID = messageID;

							// Deserialize
							WsMessage message = WsMessageServiceImpl.this.deserializeMessage(msg);
							messages.add(message);
						}

						// Check persistence
						if (!msgD.get("persistent").getAsBoolean()) {
							// Delete
							deleteMessage(messageID);
						}
					}
				}
			}

			// Load container
			AccountDataContainer messagesContainer = account.getAccountData().getChildContainer("wsservicemessages");

			// Find messages
			for (String key : messagesContainer.getEntryKeys()) {
				if (key.startsWith("msg-")) {
					// Parse ID
					int messageID = Integer.parseInt(key.substring(4));

					// Read json
					JsonObject msgD = messagesContainer.getEntry(key).getAsJsonObject();

					// Compare
					if (msgD.get("session").getAsLong() != account.getLastLoginTime()) {
						// Delete
						deleteMessage(messageID);
						continue;
					}

					// Load message
					MessageInfoData msg = new ObjectMapper().readValue(msgD.get("messageObject").toString(),
							MessageInfoData.class);
					msg.messageQueueID = messageID;

					// Deserialize
					WsMessage message = WsMessageServiceImpl.this.deserializeMessage(msg);
					messages.add(message);

					// Delete
					deleteMessage(messageID);
				}
			}

			// Return
			return messages.toArray(t -> new WsMessage[t]);
		}

		@Override
		public void deleteMessage(int messageID) throws IOException {
			if (save != null) {
				// Load container
				AccountDataContainer messagesContainer = save.getSaveData().getChildContainer("wsservicemessages");
				if (messagesContainer.entryExists("msg-" + messageID)) {
					messagesContainer.deleteEntry("msg-" + messageID);
					return;
				}
			}

			// Find non-persistent
			// Load container
			AccountDataContainer messagesContainer = account.getAccountData().getChildContainer("wsservicemessages");

			// Find message
			if (messagesContainer.entryExists("msg-" + messageID)) {
				// Delete
				messagesContainer.deleteEntry("msg-" + messageID);
			}
		}

		@Override
		public void markMessageRead(int messageID) throws IOException {
			if (save != null) {
				// Load container
				AccountDataContainer messagesContainer = save.getSaveData().getChildContainer("wsservicemessages");

				// Find message
				if (messagesContainer.entryExists("msg-" + messageID)) {
					// Read json
					JsonObject msgD = messagesContainer.getEntry("msg-" + messageID).getAsJsonObject();

					// Update
					msgD.addProperty("isRead", true);

					// Save
					messagesContainer.setEntry("msg-" + messageID, msgD);
					return;
				}
			}

			// Load container
			AccountDataContainer messagesContainer = account.getAccountData().getChildContainer("wsservicemessages");

			// Find message
			if (messagesContainer.entryExists("msg-" + messageID)) {
				// Delete
				deleteMessage(messageID);
			}
		}

		@Override
		public void sendPersistentMessage(WsMessage message) throws IOException {
			if (save == null)
				throw new IllegalArgumentException(
						"Cannot send persistent messages from getMessengerFor(account), this can only be used from getMessengerFor(account,save)");

			// Load container
			AccountDataContainer messagesContainer = save.getSaveData().getChildContainer("wsservicemessages");

			// Create object
			MessageInfoData msg = new MessageInfoData();
			message.serialize(msg);

			// Allocate ID
			int messageID = 0;
			while (messagesContainer.entryExists("msg-" + messageID))
				messageID++;
			msg.messageQueueID = messageID;

			// Create information object
			JsonObject msgD = new JsonObject();
			msgD.addProperty("isRead", false);
			msgD.addProperty("persistent", true);
			msgD.add("messageObject", JsonParser.parseString(new ObjectMapper().writeValueAsString(msg)));

			// Save
			messagesContainer.setEntry("msg-" + messageID, msgD);
		}

		@Override
		public void sendSessionMessage(WsMessage message) throws IOException {
			// Load container
			AccountDataContainer messagesContainer = account.getAccountData().getChildContainer("wsservicemessages");

			// Create object
			MessageInfoData msg = new MessageInfoData();
			message.serialize(msg);

			// Allocate ID
			int messageID = -1;
			while (messagesContainer.entryExists("msg-" + messageID))
				messageID--;
			msg.messageQueueID = messageID;

			// Create information object
			JsonObject msgD = new JsonObject();
			msgD.addProperty("session", account.getLastLoginTime());
			msgD.add("messageObject", JsonParser.parseString(new ObjectMapper().writeValueAsString(msg)));

			// Save
			messagesContainer.setEntry("msg-" + messageID, msgD);
		}

	}

	@Override
	public void initService() {
	}

	@Override
	public PlayerMessenger getMessengerFor(AccountObject account, AccountSaveContainer save) {
		PlayerMessengerImpl i = new PlayerMessengerImpl();
		i.account = account;
		i.save = save;
		return i;
	}

	@Override
	public PlayerMessenger getMessengerFor(AccountObject account) {
		PlayerMessengerImpl i = new PlayerMessengerImpl();
		i.account = account;
		return i;
	}

}
