package org.asf.edge.gameplayapi.services.quests.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.account.AccountDataContainer;
import org.asf.edge.common.account.AccountSaveContainer;
import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.gameplayapi.entities.quests.UserQuestInfo;
import org.asf.edge.gameplayapi.entities.quests.UserQuestStatus;
import org.asf.edge.gameplayapi.http.handlers.gameplayapi.ContentWebServiceV2Processor;
import org.asf.edge.gameplayapi.services.quests.QuestManager;
import org.asf.edge.gameplayapi.xmls.inventories.InventoryUpdateResponseData;
import org.asf.edge.gameplayapi.xmls.inventories.InventoryUpdateResponseData.ItemUpdateBlock;
import org.asf.edge.gameplayapi.xmls.inventories.SetCommonInventoryRequestData;
import org.asf.edge.gameplayapi.xmls.inventories.CommonInventoryData.ItemBlock;
import org.asf.edge.gameplayapi.xmls.quests.MissionData;
import org.asf.edge.gameplayapi.xmls.quests.MissionData.AchievementRewardBlock;
import org.asf.edge.gameplayapi.xmls.quests.SetTaskStateResultData;
import org.asf.edge.gameplayapi.xmls.quests.SetTaskStateResultData.CompletedMissionInfoBlock;
import org.asf.edge.gameplayapi.xmls.quests.edgespecific.QuestRegistryManifest;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonArray;

public class QuestManagerImpl extends QuestManager {

	private Logger logger;
	private ArrayList<Integer> defaultActiveQuests = new ArrayList<Integer>();
	private ArrayList<Integer> defaultUnlockedQuests = new ArrayList<Integer>();
	private HashMap<Integer, MissionData> quests = new HashMap<Integer, MissionData>();
	private HashMap<Integer, MissionData> allQuests = new HashMap<Integer, MissionData>();
	private static Random rnd = new Random();

	@Override
	public void initService() {
		logger = LogManager.getLogger("QuestManager");

		// Load quests
		logger.info("Loading quest data...");
		try {
			// Load XML
			InputStream strm = getClass().getClassLoader().getResourceAsStream("questdata.xml");
			String data = new String(strm.readAllBytes(), "UTF-8");
			strm.close();

			// Load into map
			XmlMapper mapper = new XmlMapper();
			QuestRegistryManifest questReg = mapper.readValue(data, QuestRegistryManifest.class);

			// Load quests
			logger.info("Loading quest definitions...");
			for (MissionData quest : questReg.defaultQuestDefs.questDefs) {
				// Load quest
				quests.put(quest.id, quest);
				scanQuest(quest);
			}

			// Load default active quests
			logger.info("Loading default active quests...");
			for (int id : questReg.defaultStartedQuests.defaultStartedQuests) {
				MissionData quest = allQuests.get(id);
				if (quest != null) {
					logger.debug("Registered default active quest: " + id + ": " + quest.name);
					defaultActiveQuests.add(id);
				}
			}

			// Load default active quests
			logger.info("Loading default unlocked quests...");
			for (int id : questReg.defaultUnlockedQuests.defaultUnlockedQuests) {
				MissionData quest = allQuests.get(id);
				if (quest != null) {
					logger.debug("Registered default unlocked quest: " + id + ": " + quest.name);
					defaultUnlockedQuests.add(id);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void scanQuest(MissionData quest) {
		allQuests.put(quest.id, quest);
		logger.debug("Registered quest definition: " + quest.id + ": " + quest.name);
		if (quest.childMissions != null) {
			for (MissionData cQuest : quest.childMissions)
				scanQuest(cQuest);
		}
	}

	@Override
	public int[] getQuestIDs() {
		int[] ids = new int[quests.size()];
		int i = 0;
		for (int id : quests.keySet())
			ids[i++] = id;
		return ids;
	}

	@Override
	public int[] getAllQuestIDs() {
		int[] ids = new int[allQuests.size()];
		int i = 0;
		for (int id : allQuests.keySet())
			ids[i++] = id;
		return ids;
	}

	@Override
	public MissionData getQuestDef(int id) {
		return allQuests.get(id);
	}

	@Override
	public MissionData[] getQuestDefs() {
		return quests.values().toArray(t -> new MissionData[t]);
	}

	@Override
	public MissionData[] getAllQuestDefs() {
		return allQuests.values().toArray(t -> new MissionData[t]);
	}

	@Override
	public UserQuestInfo getUserQuest(AccountSaveContainer save, int id) {
		// Find def
		MissionData def = getQuestDef(id);
		if (def == null)
			return null;
		return new UserQuestInfoImpl(def, save);
	}

	@Override
	public UserQuestInfo[] getCompletedQuests(AccountSaveContainer save) {
		try {
			// Load completed quests
			AccountDataContainer data = save.getSaveData().getChildContainer("quests");
			if (!data.entryExists("completedquests"))
				return new UserQuestInfo[0];

			// Create list
			ArrayList<UserQuestInfo> quests = new ArrayList<UserQuestInfo>();
			JsonArray completedQuests = data.getEntry("completedquests").getAsJsonArray();
			for (JsonElement ele : completedQuests) {
				// Find quest
				quests.add(getUserQuest(save, ele.getAsInt()));
			}

			// Return
			return quests.toArray(t -> new UserQuestInfo[t]);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public UserQuestInfo[] getActiveQuests(AccountSaveContainer save) {
		try {
			// Load active quests
			AccountDataContainer data = save.getSaveData().getChildContainer("quests");
			if (!data.entryExists("activequests"))
				return new UserQuestInfo[0];

			// Create list
			ArrayList<UserQuestInfo> quests = new ArrayList<UserQuestInfo>();
			JsonArray activeQuests = data.getEntry("activequests").getAsJsonArray();
			for (JsonElement ele : activeQuests) {
				// Find quest
				quests.add(getUserQuest(save, ele.getAsInt()));
			}

			// Return
			return quests.toArray(t -> new UserQuestInfo[t]);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public UserQuestInfo[] getUnlockedQuests(AccountSaveContainer save) {
		// Create list of default unlocked quests
		ArrayList<Integer> defaultQuests = new ArrayList<Integer>(defaultUnlockedQuests);

		// Find user-specific unlocked quests
		try {
			AccountDataContainer data = save.getSaveData().getChildContainer("quests");
			if (data.entryExists("unlockedquests")) {
				// Load quest ids into memory
				JsonArray unlocked = data.getEntry("unlockedquests").getAsJsonArray();
				for (JsonElement ele : unlocked) {
					// Find quest
					defaultQuests.add(ele.getAsInt());
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// Create list
		ArrayList<UserQuestInfo> quests = new ArrayList<UserQuestInfo>();
		for (int id : defaultQuests) {
			quests.add(getUserQuest(save, id));
		}

		// Return
		return quests.toArray(t -> new UserQuestInfo[t]);
	}

	private class UserQuestInfoImpl extends UserQuestInfo {

		private MissionData def;
		private AccountDataContainer data;
		private AccountSaveContainer save;
		private UserQuestStatus status;
		private JsonObject questInfo;

		public UserQuestInfoImpl(MissionData def, AccountSaveContainer save) {
			this.def = def;
			this.save = save;

			// Load object
			try {
				data = save.getSaveData().getChildContainer("quests");
				if (data.entryExists("quest-" + def.id)) {
					// Load quest
					questInfo = data.getEntry("quest-" + def.id).getAsJsonObject();

					// Load status
					switch (questInfo.get("status").getAsInt()) {

					case 0:
						status = UserQuestStatus.COMPLETED;
						break;
					case 1:
						status = UserQuestStatus.ACTIVE;
						break;
					case 2:
						status = UserQuestStatus.INACTIVE;
						break;

					}
				} else {
					// Not found
					status = UserQuestStatus.INACTIVE;
					questInfo = new JsonObject();
					questInfo.addProperty("status", 2);
					questInfo.add("payload", new JsonObject());
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public int getQuestID() {
			return def.id;
		}

		@Override
		public MissionData getDef() {
			return def;
		}

		@Override
		public MissionData getData() {
			// Retrieve data
			MissionData data = def.copy();

			// Get child missions
			if (data.childMissions != null) {
				for (int i = 0; i < data.childMissions.length; i++) {
					// Make sure the children are updated too
					data.childMissions[i] = getUserQuest(save, data.childMissions[i].id).getData();
				}
			}

			// Apply modifications
			JsonObject payload = questInfo.get("payload").getAsJsonObject();
			if (payload.has("quest")) {
				// Apply quest object modifications
				JsonObject questPayload = payload.get("quest").getAsJsonObject();
				if (questPayload.has("accepted"))
					data.accepted = questPayload.get("accepted").getAsBoolean();
				if (questPayload.has("completed"))
					data.completed = questPayload.get("completed").getAsInt();
			}
			if (payload.has("tasks") && data.tasks != null) {
				// Apply task modifications
				JsonObject tasksBase = payload.get("tasks").getAsJsonObject();
				for (MissionData.TaskBlock task : data.tasks) {
					if (tasksBase.has(Integer.toString(task.id))) {
						// Apply modification
						JsonObject taskPayload = tasksBase.get(Integer.toString(task.id)).getAsJsonObject();
						if (taskPayload.has("failed"))
							task.failed = taskPayload.get("failed").getAsBoolean();
						if (taskPayload.has("completed"))
							task.completed = taskPayload.get("completed").getAsBoolean() ? 1 : 0;
						if (taskPayload.has("payload"))
							task.payload = taskPayload.get("payload").getAsString();
					}
				}
			}

			// Return
			return data;
		}

		@Override
		public UserQuestStatus getStatus() {
			return status;
		}

		@Override
		public void setAcceptedField(boolean accepted) {
			// Update info

			// Load payload
			JsonObject payload = questInfo.get("payload").getAsJsonObject();
			JsonObject questPayload;
			if (payload.has("quest"))
				questPayload = payload.get("quest").getAsJsonObject();
			else {
				questPayload = new JsonObject();
				payload.add("quest", questPayload);
			}
			questPayload.addProperty("accepted", accepted);

			// Save
			try {
				data.setEntry("quest-" + def.id, questInfo);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void setTaskFailedField(int taskID, boolean failed) {
			// Update info

			// Load payload
			JsonObject payload = questInfo.get("payload").getAsJsonObject();
			JsonObject tasksBase;
			if (payload.has("tasks"))
				tasksBase = payload.get("tasks").getAsJsonObject();
			else {
				tasksBase = new JsonObject();
				payload.add("tasks", tasksBase);
			}
			JsonObject taskPayload;
			if (tasksBase.has(Integer.toString(taskID)))
				taskPayload = tasksBase.get(Integer.toString(taskID)).getAsJsonObject();
			else {
				taskPayload = new JsonObject();
				tasksBase.add(Integer.toString(taskID), taskPayload);
			}
			taskPayload.addProperty("failed", failed);

			// Save
			try {
				data.setEntry("quest-" + def.id, questInfo);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public SetTaskStateResultData handleTaskCall(int taskID, String payloadStr, boolean completed, int invContainer,
				SetCommonInventoryRequestData[] requests) throws IOException {
			// Find task
			MissionData mission = getData();
			Optional<MissionData.TaskBlock> opt = Stream.of(mission.tasks).filter(t -> t.id == taskID).findFirst();
			MissionData.TaskBlock task = opt.isPresent() ? opt.get() : null;

			// Check task
			if (task == null) {
				// Invalid request
				SetTaskStateResultData resp = new SetTaskStateResultData();
				resp.success = false;
				resp.status = SetTaskStateResultData.SetTaskStateResultStatuses.MISSION_STATE_NOT_FOUND;
				return resp;
			}

			// Security checks
			if (isCompleted() && !mission.repeatable) {
				// Invalid request
				SetTaskStateResultData resp = new SetTaskStateResultData();
				resp.success = false;
				resp.status = SetTaskStateResultData.SetTaskStateResultStatuses.NON_REPEATABLE_MISSION;
				return resp;
			}
			// FIXME: implement more checks

			// Check if active
			boolean wasActive = true;
			if (!isActive()) {
				// Start quest
				startQuest();
				wasActive = false;
			}

			// Update task
			// Load payload
			JsonObject payload = questInfo.get("payload").getAsJsonObject();
			JsonObject tasksBase;
			if (payload.has("tasks"))
				tasksBase = payload.get("tasks").getAsJsonObject();
			else {
				tasksBase = new JsonObject();
				payload.add("tasks", tasksBase);
			}
			JsonObject taskPayload;
			if (tasksBase.has(Integer.toString(taskID)))
				taskPayload = tasksBase.get(Integer.toString(taskID)).getAsJsonObject();
			else {
				taskPayload = new JsonObject();
				tasksBase.add(Integer.toString(taskID), taskPayload);
			}
			taskPayload.addProperty("payload", payloadStr);
			taskPayload.addProperty("completed", completed);
			task.completed = completed ? 1 : 0;
			task.payload = payloadStr;

			// Save
			try {
				data.setEntry("quest-" + def.id, questInfo);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			// Prepare response
			SetTaskStateResultData resp = new SetTaskStateResultData();
			resp.status = SetTaskStateResultData.SetTaskStateResultStatuses.TASK_CAN_BE_DONE;
			resp.success = true;

			// Update inventories
			if (requests != null & requests.length != 0) {
				resp.inventoryUpdate = ContentWebServiceV2Processor.processCommonInventorySet(requests,
						save.getSaveData(), invContainer);
			}

			// Acceptance rewards
			if (!wasActive) {
				// Check if already completed
				boolean wasCompleted = Stream.of(getCompletedQuests(save))
						.anyMatch(t -> t.getQuestID() == getQuestID());

				// Achievements
				// TODO: achievements

				// Go through rewards
				if (def.acceptanceRewards != null) {
					for (AchievementRewardBlock reward : def.acceptanceRewards) {
						if (!reward.allowMultiple && wasCompleted)
							continue;

						// Prepare update
						if (resp.inventoryUpdate == null) {
							resp.inventoryUpdate = new InventoryUpdateResponseData();
							resp.inventoryUpdate.success = true;
						}

						// Copy
						reward = reward.copy();

						// Set amount
						if (reward.minAmount != -1 && reward.maxAmount != -1) {
							reward.amount = rnd.nextInt(reward.minAmount, reward.maxAmount + 1);
							reward.minAmount = -1;
							reward.maxAmount = -1;
						}

						// Achievements
						// TODO: achievements

						// Give rewards
						switch (reward.pointTypeID) {

						// Achievement points
						case 1:
						case 9:
						case 10:
						case 12: {
							// TODO: achievement points
							// Use amount and point type ID

							break;
						}

						// Coins
						case 2: {
							// Add coins
							if (resp.inventoryUpdate.currencyUpdate == null) {
								resp.inventoryUpdate.currencyUpdate = new InventoryUpdateResponseData.CurrencyUpdateBlock();
								resp.inventoryUpdate.currencyUpdate.userID = save.getSaveID();
								resp.inventoryUpdate.currencyUpdate.coinCount = 0;
								resp.inventoryUpdate.currencyUpdate.gemCount = 0;
							}
							resp.inventoryUpdate.currencyUpdate.coinCount += reward.amount;

							// Update inventory
							AccountDataContainer currency = data.getChildContainer("currency");
							int currentC = 300;
							if (currency.entryExists("coins"))
								currentC = currency.getEntry("coins").getAsInt();
							currency.setEntry("coins", new JsonPrimitive(currentC + reward.amount));
							break;
						}

						// Gems
						case 5: {
							// Add gems
							if (resp.inventoryUpdate.currencyUpdate == null) {
								resp.inventoryUpdate.currencyUpdate = new InventoryUpdateResponseData.CurrencyUpdateBlock();
								resp.inventoryUpdate.currencyUpdate.userID = save.getSaveID();
								resp.inventoryUpdate.currencyUpdate.coinCount = 0;
								resp.inventoryUpdate.currencyUpdate.gemCount = 0;
							}
							resp.inventoryUpdate.currencyUpdate.gemCount += reward.amount;

							// Update inventory
							AccountDataContainer currencyAccWide = save.getAccount().getAccountData()
									.getChildContainer("currency");
							int currentG = 0;
							if (currencyAccWide.entryExists("gems"))
								currentG = currencyAccWide.getEntry("gems").getAsInt();
							currencyAccWide.setEntry("gems", new JsonPrimitive(currentG + reward.amount));
							break;
						}

						// Item
						case 6: {
							// Add item
							PlayerInventory inv = save.getInventory();
							PlayerInventoryContainer cont = inv.getContainer(invContainer);
							PlayerInventoryItem itm = cont.findFirst(reward.itemID);
							if (itm != null) {
								itm.add(reward.amount);
							} else {
								itm = cont.createItem(reward.itemID, reward.amount);
							}

							// Set data
							reward.uniqueRewardItemID = itm.getUniqueID();

							// Add item
							ItemBlock block = new ItemBlock();
							block.itemID = itm.getItemDefID();
							block.quantity = itm.getQuantity();
							block.uses = itm.getUses();
							block.uniqueItemID = itm.getUniqueID();

							// Add data info from item manager
							ItemInfo def = ItemManager.getInstance().getItemDefinition(block.itemID);
							if (def != null)
								block.data = def.getRawObject();

							// Add update
							ItemUpdateBlock update = new ItemUpdateBlock();
							update.itemID = reward.itemID;
							update.addedQuantity = reward.amount;
							update.itemUniqueID = itm.getUniqueID();
							resp.inventoryUpdate.updateItems = addToArray(resp.inventoryUpdate.updateItems, update);

							// Set block
							reward.rewardItem = block;
							break;
						}

						// Dragon achievement points
						case 8: {
							// TODO: achievement points
							// Use amount

							break;
						}

						}
					}
				}
			}

			// Update quests if needed
			if (completed) {
				// Check tasks
				boolean missionCompleted = isCompletedMission(mission);
				if (missionCompleted) {
					// Handle completion
					ArrayList<CompletedMissionInfoBlock> completedMissions = new ArrayList<CompletedMissionInfoBlock>();

					// Add rewards
					UserQuestInfoImpl quest = this;
					while (quest != null) {
						// Prepare object
						CompletedMissionInfoBlock i = new CompletedMissionInfoBlock();
						boolean wasCompleted = Stream.of(getCompletedQuests(save))
								.anyMatch(t -> t.getQuestID() == getQuestID());

						// Complete quest
						quest.completeQuest();
						i.missionID = quest.getQuestID();
						MissionData missionD = quest.getDef();

						// Give achievements
						// TODO: achievements

						// Go through rewards
						ArrayList<AchievementRewardBlock> rewards = new ArrayList<AchievementRewardBlock>();
						if (missionD.rewards != null) {
							for (AchievementRewardBlock reward : missionD.rewards) {
								if (!reward.allowMultiple && wasCompleted)
									continue;

								// Copy
								reward = reward.copy();

								// Set amount
								if (reward.minAmount != -1 && reward.maxAmount != -1) {
									reward.amount = rnd.nextInt(reward.minAmount, reward.maxAmount + 1);
									reward.minAmount = -1;
									reward.maxAmount = -1;
								}

								// Achievements
								// TODO: achievements

								// Give rewards
								switch (reward.pointTypeID) {

								// Achievement points
								case 1:
								case 9:
								case 10:
								case 12: {
									// TODO: achievement points
									// Use amount and point type ID

									break;
								}

								// Coins
								case 2: {
									// Update inventory
									AccountDataContainer currency = data.getChildContainer("currency");
									int currentC = 300;
									if (currency.entryExists("coins"))
										currentC = currency.getEntry("coins").getAsInt();
									currency.setEntry("coins", new JsonPrimitive(currentC + reward.amount));
									break;
								}

								// Gems
								case 5: {
									// Update inventory
									AccountDataContainer currencyAccWide = save.getAccount().getAccountData()
											.getChildContainer("currency");
									int currentG = 0;
									if (currencyAccWide.entryExists("gems"))
										currentG = currencyAccWide.getEntry("gems").getAsInt();
									currencyAccWide.setEntry("gems", new JsonPrimitive(currentG + reward.amount));
									break;
								}

								// Item
								case 6: {
									// Add item
									PlayerInventory inv = save.getInventory();
									PlayerInventoryContainer cont = inv.getContainer(invContainer);
									PlayerInventoryItem itm = cont.findFirst(reward.itemID);
									if (itm != null) {
										itm.add(reward.amount);
									} else {
										itm = cont.createItem(reward.itemID, reward.amount);
									}

									// Set data
									reward.uniqueRewardItemID = itm.getUniqueID();

									// Add item
									ItemBlock block = new ItemBlock();
									block.itemID = itm.getItemDefID();
									block.quantity = itm.getQuantity();
									block.uses = itm.getUses();
									block.uniqueItemID = itm.getUniqueID();

									// Add data info from item manager
									ItemInfo def = ItemManager.getInstance().getItemDefinition(block.itemID);
									if (def != null)
										block.data = def.getRawObject();

									// Set block
									reward.rewardItem = block;
									break;
								}

								// Dragon achievement points
								case 8: {
									// TODO: achievement points
									// Use amount

									break;
								}

								}

								// Add reward
								rewards.add(reward);
							}
						}

						// Add block
						i.rewards = rewards.toArray(t -> new AchievementRewardBlock[t]);

						// Add object
						completedMissions.add(i);

						// Get parent
						if (quest.def.parentQuestID != -1) {
							MissionData parent = allQuests.get(quest.def.parentQuestID);
							if (parent != null) {
								quest = new UserQuestInfoImpl(parent, save);
								if (!isCompletedMission(quest.getData()))
									break;
							} else
								break;
						} else
							break;
					}

					// Set response
					resp.completedMissions = completedMissions.toArray(t -> new CompletedMissionInfoBlock[t]);
				}
			}

			return resp;
		}

		private ItemUpdateBlock[] addToArray(ItemUpdateBlock[] updateItems, ItemUpdateBlock update) {
			int i = 0;
			ItemUpdateBlock[] newA = new ItemUpdateBlock[updateItems.length + 1];
			for (i = 0; i < updateItems.length; i++)
				newA[i] = updateItems[i];
			newA[i] = update;
			return newA;
		}

		private boolean isCompletedMission(MissionData mission) {
			// Check completed tasks
			int taskCount = mission.tasks.length;
			int completedTasks = (int) Stream.of(mission.tasks).filter(t -> t.completed > 0).count();
			if (completedTasks >= taskCount) {
				// Tasks completed
				// Check child quests
				if (mission.childMissions != null) {
					for (MissionData ch : mission.childMissions) {
						// Check child quest mission
						if (!isCompletedMission(ch))
							return false;
					}
				}

				// This quest has been completed
				return true;
			}
			return false;
		}

		@Override
		public void startQuest() {
			// Check status
			if (status == UserQuestStatus.ACTIVE)
				throw new IllegalArgumentException("Quest is not inactive");

			// Update status
			status = UserQuestStatus.ACTIVE;
			questInfo.addProperty("status", 1);

			try {
				// Save
				data.setEntry("quest-" + def.id, questInfo);

				// Update active quests
				JsonArray active;
				if (!data.entryExists("activequests")) {
					active = new JsonArray();
				} else
					active = data.getEntry("activequests").getAsJsonArray();
				boolean found = false;
				for (JsonElement ele : active) {
					if (ele.getAsInt() == def.id) {
						found = true;
						break;
					}
				}
				if (!found) {
					active.add(def.id);
					data.setEntry("activequests", active);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void completeQuest() {
			// Update status
			status = UserQuestStatus.COMPLETED;
			questInfo.addProperty("status", 0);

			try {
				// Save
				data.setEntry("quest-" + def.id, questInfo);

				// Update lists
				if (data.entryExists("activequests")) {
					JsonArray active = data.getEntry("activequests").getAsJsonArray();
					for (JsonElement ele : active) {
						if (ele.getAsInt() == def.id) {
							active.remove(ele);
							data.setEntry("activequests", active);
							break;
						}
					}
				}

				// Update completed quests
				JsonArray completedQuests;
				if (!data.entryExists("completedquests")) {
					completedQuests = new JsonArray();
				} else
					completedQuests = data.getEntry("completedquests").getAsJsonArray();
				boolean found = false;
				for (JsonElement ele : completedQuests) {
					if (ele.getAsInt() == def.id) {
						found = true;
						break;
					}
				}
				if (!found) {
					completedQuests.add(def.id);
					data.setEntry("completedquests", completedQuests);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

	}

	@Override
	public void startDefaultQuests(AccountSaveContainer save) {
		synchronized (save) {
			// Go through all default quests and start if needed
			for (int id : defaultActiveQuests) {
				// Find quest
				UserQuestInfo quest = getUserQuest(save, id);
				if (quest != null && quest.getStatus() == UserQuestStatus.INACTIVE) {
					// Start
					quest.startQuest();
				}
			}
		}
	}

}
