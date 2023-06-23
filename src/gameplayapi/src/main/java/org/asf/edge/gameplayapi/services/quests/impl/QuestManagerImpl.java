package org.asf.edge.gameplayapi.services.quests.impl;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
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
import org.asf.edge.gameplayapi.http.handlers.gameplayapi.ContentWebServiceV2Processor;
import org.asf.edge.gameplayapi.services.quests.QuestManager;
import org.asf.edge.gameplayapi.xmls.inventories.SetCommonInventoryRequestData;
import org.asf.edge.gameplayapi.xmls.inventories.CommonInventoryData.ItemBlock;
import org.asf.edge.gameplayapi.xmls.quests.MissionData;
import org.asf.edge.gameplayapi.xmls.quests.MissionData.AchievementRewardBlock;
import org.asf.edge.gameplayapi.xmls.quests.MissionData.MissionRulesBlock;
import org.asf.edge.gameplayapi.xmls.quests.MissionData.TaskBlock;
import org.asf.edge.gameplayapi.xmls.quests.MissionData.MissionRulesBlock.CriteriaBlock.RuleInfoBlock.RuleInfoTypes;
import org.asf.edge.gameplayapi.xmls.quests.MissionData.MissionRulesBlock.PrerequisiteInfoBlock;
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
		// Create list
		ArrayList<UserQuestInfo> quests = new ArrayList<UserQuestInfo>();

		// Find quests
		for (MissionData mission : this.quests.values()) {
			UserQuestInfo q = getUserQuest(save, mission.id);
			if ((mission.repeatable || !q.isCompleted()) && q.isActive())
				quests.add(q);
		}

		// Return
		return quests.toArray(t -> new UserQuestInfo[t]);
	}

	@Override
	public UserQuestInfo[] getUpcomingQuests(AccountSaveContainer save) {
		// Create list
		ArrayList<UserQuestInfo> quests = new ArrayList<UserQuestInfo>();

		// Find quests
		for (MissionData mission : this.quests.values()) {
			UserQuestInfo q = getUserQuest(save, mission.id);
			if (!q.isCompleted() && !q.isActive())
				quests.add(q);
		}

		// Return
		return quests.toArray(t -> new UserQuestInfo[t]);
	}

	private class UserQuestInfoImpl extends UserQuestInfo {

		private MissionData def;
		private AccountDataContainer data;
		private AccountSaveContainer save;
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
					if (!questInfo.has("accepted"))
						questInfo.addProperty("accepted", false);
					if (!questInfo.has("started"))
						questInfo.addProperty("started", false);
					if (!questInfo.has("completed"))
						questInfo.addProperty("completed", false);
				} else {
					// Not found
					questInfo = new JsonObject();
					questInfo.addProperty("completed", false);
					questInfo.addProperty("accepted", false);
					questInfo.addProperty("started", false);
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
						if (taskPayload.has("completed"))
							task.completed = taskPayload.get("completed").getAsBoolean() ? 1 : 0;
						if (taskPayload.has("payload"))
							task.payload = taskPayload.get("payload").getAsString();
					}
				}
			}

			// Set criteria
			if (data.missionRules != null && data.missionRules.criteria != null
					&& data.missionRules.criteria.rules != null) {
				for (MissionData.MissionRulesBlock.CriteriaBlock.RuleInfoBlock rule : data.missionRules.criteria.rules) {
					if (rule.type == RuleInfoTypes.MISSION) {
						// Mission completion
						int missionID = rule.id;
						if (missionID == data.id) {
							// Set completion state
							rule.complete = data.completed;
						} else {
							// Find mission
							UserQuestInfo i = getUserQuest(save, missionID);
							if (i != null)
								rule.complete = i.isCompleted() ? 1 : 0;
						}
					} else {
						// Task completion
						UserQuestInfo i = this;
						int missionID = rule.missionID;
						if (missionID != data.id)
							i = getUserQuest(save, missionID);
						if (i != null) {
							// Check mission
							MissionData d = data;
							if (missionID != data.id)
								d = i.getData();

							// Find task
							Optional<TaskBlock> opt = Stream.of(d.tasks).filter(t -> t.id == rule.id).findFirst();
							if (opt.isPresent()) {
								rule.complete = opt.get().completed;
							}
						}
					}
				}
			}

			// Return
			return data;
		}

		@Override
		public void acceptQuest() {
			// Update info

			// Start quest
			startQuest();

			// Load payload
			JsonObject payload = questInfo.get("payload").getAsJsonObject();
			JsonObject questPayload;
			if (payload.has("quest"))
				questPayload = payload.get("quest").getAsJsonObject();
			else {
				questPayload = new JsonObject();
				payload.add("quest", questPayload);
			}
			questPayload.addProperty("accepted", true);
			questInfo.addProperty("accepted", true);

			// Save
			try {
				data.setEntry("quest-" + def.id, questInfo);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			// Acceptance rewards

			// Achievements
			// TODO: achievements

			// Go through rewards
			try {
				if (def.acceptanceRewards != null) {
					for (AchievementRewardBlock reward : def.acceptanceRewards) {
						if (!reward.allowMultiple && isCompleted())
							continue;

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
							PlayerInventoryContainer cont = inv.getContainer(1);
							PlayerInventoryItem itm = cont.findFirst(reward.itemID);
							if (itm != null) {
								itm.add(reward.amount);
							} else {
								itm = cont.createItem(reward.itemID, reward.amount);
							}

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
			if (!questInfo.get("started").getAsBoolean()) {
				// Start quest
				startQuest();
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
			if (invContainer == -1)
				invContainer = 1;

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
						boolean wasCompleted = quest.isCompleted();

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

								// Set ID
								reward.entityID = UUID.fromString(save.getSaveID());

								// Set amount
								if (reward.minAmount != -1 && reward.maxAmount != -1) {
									reward.amount = rnd.nextInt(reward.minAmount, reward.maxAmount + 1);
									reward.minAmount = 0;
									reward.maxAmount = 0;
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
									reward.achievementID = 0;
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
								if (!quest.isCompleted() && !isCompletedMission(quest.getData()))
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

		private boolean isCompletedMission(MissionData mission) {
			// Check completed tasks
			int taskCount = mission.tasks.length;
			int completedTasks = (int) Stream.of(mission.tasks).filter(t -> t.completed > 0).count();
			if (completedTasks >= taskCount) {
				// The following checks are disabled as disabling them may fix a bug, if it
				// causes issues instead they should be re-enabled

//				// Tasks completed
//				// Check child quests
//				if (mission.childMissions != null) {
//					for (MissionData ch : mission.childMissions) {
//						// Check child quest mission
//						if (!isCompletedMission(ch))
//							return false;
//					}
//				}

				// This quest has been completed
				return true;
			}
			return false;
		}

		@Override
		public void startQuest() {
			// Check status
			if (questInfo.get("started").getAsBoolean())
				throw new IllegalArgumentException("Quest is already started");

			// Update status
			questInfo.addProperty("started", true);

			try {
				// Save
				data.setEntry("quest-" + def.id, questInfo);

				// Check
				if (def.parentQuestID > 0)
					return;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void completeQuest() {
			// Update status
			questInfo.addProperty("completed", true);
			questInfo.addProperty("started", false);

			try {
				// Save
				data.setEntry("quest-" + def.id, questInfo);

				// Check
				if (def.parentQuestID > 0)
					return;

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

		@Override
		public boolean isActive() {
			// Check prerequisites
			if (def.missionRules != null) {
				if (def.missionRules.prerequisites != null) {
					for (PrerequisiteInfoBlock req : def.missionRules.prerequisites) {
						if (!req.clientRule) {
							// Check type
							switch (req.type) {

							// Accept rule
							case MissionRulesBlock.PrerequisiteInfoBlock.PrerequisiteRuleTypes.ACCEPT: {
								// Check if the mission must be accepted
								if (req.value.equalsIgnoreCase("true")) {
									if (!questInfo.get("accepted").getAsBoolean())
										return false;
								}

								break;
							}

							// Mission rule
							case MissionRulesBlock.PrerequisiteInfoBlock.PrerequisiteRuleTypes.MISSION: {
								// Check if the mission is complete
								int missionID = Integer.parseInt(req.value);
								UserQuestInfo quest = getUserQuest(save, missionID);
								if (quest == null)
									return false;
								else if (!quest.isCompleted())
									return false;

								break;
							}

							// Date range rule
							case MissionRulesBlock.PrerequisiteInfoBlock.PrerequisiteRuleTypes.DATERANGE: {
								// Parse
								String[] dStrs = req.value.split(",");
								if (dStrs.length == 2) {
									String startDate = dStrs[0];
									String endDate = dStrs[1];

									try {
										// Parse dates
										SimpleDateFormat fmt = new SimpleDateFormat("MM'-'dd'-'yyyy HH':'mm':'ss");
										Date start = fmt.parse(startDate);
										Date end = fmt.parse(endDate);

										// Check
										Date now = new Date(System.currentTimeMillis());
										if (start.before(now) || end.after(now)) {
											return false;
										}
									} catch (ParseException e) {
										try {
											// Parse dates
											SimpleDateFormat fmt = new SimpleDateFormat("MM'-'dd'-'yyyy");
											Date start = fmt.parse(startDate);
											Date end = fmt.parse(endDate);

											// Check
											Date now = new Date(System.currentTimeMillis());
											if (start.before(now) || end.after(now)) {
												return false;
											}
										} catch (ParseException e2) {
											try {
												// Parse dates
												SimpleDateFormat fmt = new SimpleDateFormat("dd'/'MM'/'yyyy");
												Date start = fmt.parse(startDate);
												Date end = fmt.parse(endDate);

												// Check
												Date now = new Date(System.currentTimeMillis());
												if (start.before(now) || end.after(now)) {
													return false;
												}
											} catch (ParseException e3) {
												throw new RuntimeException(e);
											}
										}
									}
								}

								break;
							}

							// Event rule
							case MissionRulesBlock.PrerequisiteInfoBlock.PrerequisiteRuleTypes.EVENT: {
								// TODO
								return false;
							}

							// Item rule
							case MissionRulesBlock.PrerequisiteInfoBlock.PrerequisiteRuleTypes.ITEM: {
								// Check item
								int itmId = Integer.parseInt(req.value);
								Optional<PlayerInventoryItem> opt = Stream.of(save.getInventory().getContainers())
										.map(t -> t.findFirst(itmId)).filter(t -> t != null).findFirst();
								if (opt.isPresent()) {
									// Check quantity
									if (req.quantity > opt.get().getQuantity())
										return false;
								} else
									return false;

								break;
							}

							// Member rule
							case MissionRulesBlock.PrerequisiteInfoBlock.PrerequisiteRuleTypes.MEMBER: {
								// Check member only
								if (req.value.equalsIgnoreCase("true")) {
									// TODO: membership check
									return false;
								}

								break;
							}

							// Rank rule
							case MissionRulesBlock.PrerequisiteInfoBlock.PrerequisiteRuleTypes.RANK: {
								// Check rank
								String[] idsStrs = req.value.split(",");
								int achievementID = 0;
								int minimal = 0;
								int maximum = 0;
								if (idsStrs.length >= 1)
									achievementID = Integer.parseInt(idsStrs[0]);
								if (idsStrs.length >= 2)
									minimal = Integer.parseInt(idsStrs[1]);
								if (idsStrs.length >= 3)
									maximum = Integer.parseInt(idsStrs[2]);

								// Check achievement rank
								// TODO
								return false;
							}

							}
						}
					}
				}
			}

			// Check parent
			if (def.parentQuestID > 0) {
				UserQuestInfo quest = getUserQuest(save, def.parentQuestID);
				if (quest != null)
					return quest.isActive();
			}
			return true;
		}

		@Override
		public boolean isCompleted() {
			return questInfo.get("completed").getAsBoolean();
		}

		@Override
		public boolean isStarted() {
			return questInfo.get("started").getAsBoolean();
		}

	}

}
