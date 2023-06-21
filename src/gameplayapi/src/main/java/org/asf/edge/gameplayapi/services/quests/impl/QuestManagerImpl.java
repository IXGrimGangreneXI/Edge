package org.asf.edge.gameplayapi.services.quests.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.account.AccountDataContainer;
import org.asf.edge.common.account.AccountSaveContainer;
import org.asf.edge.gameplayapi.entities.quests.UserQuestInfo;
import org.asf.edge.gameplayapi.entities.quests.UserQuestStatus;
import org.asf.edge.gameplayapi.services.quests.QuestManager;
import org.asf.edge.gameplayapi.xmls.inventories.CommonInventoryRequestData;
import org.asf.edge.gameplayapi.xmls.quests.MissionData;
import org.asf.edge.gameplayapi.xmls.quests.SetTaskStateResultData;
import org.asf.edge.gameplayapi.xmls.quests.edgespecific.QuestRegistryManifest;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class QuestManagerImpl extends QuestManager {

	private Logger logger;
	private ArrayList<Integer> defaultActiveQuests = new ArrayList<Integer>();
	private ArrayList<Integer> defaultUnlockedQuests = new ArrayList<Integer>();
	private HashMap<Integer, MissionData> quests = new HashMap<Integer, MissionData>();
	private HashMap<Integer, MissionData> allQuests = new HashMap<Integer, MissionData>();

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
				CommonInventoryRequestData[] requests) {
			// TODO: common inventory requests

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
			if (!isActive()) {
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

			// Update quests if needed
			if (completed) {
				// Check tasks
				boolean missionCompleted = isCompletedMission(mission);
				if (missionCompleted) {
					// TODO
					missionCompleted = missionCompleted;
				}
			}

			return resp;
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
			if (status != UserQuestStatus.INACTIVE)
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
			// Check status
			if (status == UserQuestStatus.COMPLETED)
				throw new IllegalArgumentException("Quest is already completed");

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
