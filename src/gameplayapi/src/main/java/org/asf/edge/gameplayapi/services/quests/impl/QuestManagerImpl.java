package org.asf.edge.gameplayapi.services.quests.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.commondata.CommonDataContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
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
import org.asf.edge.modules.IEdgeModule;
import org.asf.edge.modules.ModuleManager;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonArray;

public class QuestManagerImpl extends QuestManager {

	private Logger logger;
	private HashMap<Integer, MissionData> quests = new HashMap<Integer, MissionData>();
	private HashMap<Integer, MissionData> allQuests = new HashMap<Integer, MissionData>();
	private static Random rnd = new Random();

	private long lastReloadTime;
	private long lastQuestUpdateTime;
	private String questDataVersion;
	private String lastQuestUpdateVersion;

	@Override
	public void initService() {
		logger = LogManager.getLogger("QuestManager");

		// Start reload watchdog
		CommonDataContainer cont = CommonDataManager.getInstance().getContainer("QUESTMANAGER");
		try {
			if (!cont.entryExists("lastreload")) {
				lastReloadTime = System.currentTimeMillis();
				cont.setEntry("lastreload", new JsonPrimitive(lastReloadTime));
			} else
				lastReloadTime = cont.getEntry("lastreload").getAsLong();
		} catch (IOException e) {
		}
		AsyncTaskManager.runAsync(() -> {
			while (true) {
				// Check reload
				try {
					long reload = cont.getEntry("lastreload").getAsLong();
					if (reload > lastReloadTime) {
						// Trigger reload
						lastReloadTime = reload;
						loadQuests();
					}
				} catch (IOException e) {
				}
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
				}
			}
		});

		// Load
		loadQuests();

		// Load update time
		if (!new File("questversion.json").exists()) {
			try {
				Files.writeString(Path.of("questversion.json"), "{\n    "
						+ "\"__COMMENT1__\": \"this file controls the quest version, each time quest data is updated this file should also be updated to hold a new version ID\",\n    "
						+ "\"__COMMENT2__\": \"you MUST update this file manually otherwise quests wont be recomputed after user content updates\",\n    "
						+ "\"version\": \"" + System.currentTimeMillis() + "\"\n" + "}\n");
			} catch (IOException e) {
			}
		}
		try {
			lastQuestUpdateVersion = JsonParser.parseString(Files.readString(Path.of("questversion.json")))
					.getAsJsonObject().get("version").getAsString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// Start quest date check watchdog
		try {
			if (!cont.entryExists("lastupdate")) {
				lastQuestUpdateTime = System.currentTimeMillis();
				cont.setEntry("lastupdate", new JsonPrimitive(lastQuestUpdateTime));
			} else
				lastQuestUpdateTime = cont.getEntry("lastupdate").getAsLong();
		} catch (IOException e) {
		}
		AsyncTaskManager.runAsync(() -> {
			while (true) {
				// Check quest defs
				try {
					// Check all quest defs
					JsonArray allDateActiveDefsLast = new JsonArray();
					if (cont.entryExists("activedefs"))
						allDateActiveDefsLast = cont.getEntry("activedefs").getAsJsonArray();
					ArrayList<Integer> defsActiveLast = new ArrayList<Integer>();
					ArrayList<Integer> defsActiveCurrent = new ArrayList<Integer>();
					for (JsonElement el : allDateActiveDefsLast)
						defsActiveLast.add(el.getAsInt());

					// Load current defs
					for (MissionData def : this.getAllQuestDefs()) {
						boolean active = true;

						// Check prerequisites
						if (def.missionRules != null) {
							if (def.missionRules.prerequisites != null) {
								for (PrerequisiteInfoBlock req : def.missionRules.prerequisites) {
									if (!req.clientRule) {
										// Check type
										switch (req.type) {

										// Date range rule
										case MissionRulesBlock.PrerequisiteInfoBlock.PrerequisiteRuleTypes.DATERANGE: {
											// Parse
											String[] dStrs = req.value.split(",");
											if (dStrs.length == 2) {
												String startDate = dStrs[0];
												String endDate = dStrs[1];

												try {
													// Parse dates
													SimpleDateFormat fmt = new SimpleDateFormat(
															"MM'-'dd'-'yyyy HH':'mm':'ss");
													Date start = fmt.parse(startDate);
													Date end = fmt.parse(endDate);

													// Check
													Date now = new Date(System.currentTimeMillis());
													if (start.before(now) || end.after(now)) {
														active = false;
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
															active = false;
														}
													} catch (ParseException e2) {
														try {
															// Parse dates
															SimpleDateFormat fmt = new SimpleDateFormat(
																	"dd'/'MM'/'yyyy");
															Date start = fmt.parse(startDate);
															Date end = fmt.parse(endDate);

															// Check
															Date now = new Date(System.currentTimeMillis());
															if (start.before(now) || end.after(now)) {
																active = false;
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
											active = false;
											break;
										}

										}
									}
								}
							}
						}

						// Check
						if (active)
							defsActiveCurrent.add(def.id);
					}

					// Check def lists
					boolean changed = false;
					if (defsActiveCurrent.size() != defsActiveLast.size()) {
						changed = true;
					} else {
						// Go through the lists
						for (int id : defsActiveCurrent) {
							if (!defsActiveLast.contains(id)) {
								changed = true;
								break;
							}
						}
						for (int id : defsActiveLast) {
							if (!defsActiveCurrent.contains(id)) {
								changed = true;
								break;
							}
						}
					}

					// If changed, update
					if (changed) {
						// Create new def list
						JsonArray newDefs = new JsonArray();
						for (int id : defsActiveCurrent)
							newDefs.add(id);
						cont.setEntry("activedefs", newDefs);

						// Update time
						lastQuestUpdateTime = System.currentTimeMillis();
						cont.setEntry("lastupdate", new JsonPrimitive(lastQuestUpdateTime));
					}
				} catch (IOException e) {
				}
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
				}
			}
		});
	}

	private void loadQuests() {
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
			questDataVersion = questReg.questDataVersion;

			// Load quests
			logger.info("Loading quest definitions...");
			HashMap<Integer, MissionData> quests = new HashMap<Integer, MissionData>();
			for (MissionData quest : questReg.defaultQuestDefs.questDefs) {
				// Load quest
				quests.put(quest.id, quest);
				scanQuest(quest);
			}

			// Load quest transformers
			logger.info("Loading quest transformers...");
			loadTransformers(getClass(), quests);

			// Load module transformers
			for (IEdgeModule module : ModuleManager.getLoadedModules()) {
				loadTransformers(module.getClass(), quests);
			}

			// Load all transformers from disk
			File transformersQuests = new File("questtransformers");
			if (transformersQuests.exists()) {
				for (File transformer : transformersQuests
						.listFiles(t -> t.getName().endsWith(".xml") || t.isDirectory())) {
					loadTransformer(transformer, quests);
				}
			}

			// Apply
			this.quests = quests;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void applyTransformer(MissionData def, HashMap<Integer, MissionData> quests) {
		// Find def, if not present, define a new one
		MissionData old = quests.get(def.id);
		if (old == null) {
			// Define new
			quests.put(def.id, def);
			scanQuest(def);
		} else {
			// Transform
			if (def.staticData != null)
				old.staticData = def.staticData;
			if (def.acceptanceAchievementID != 0)
				old.acceptanceAchievementID = def.acceptanceAchievementID;
			if (def.acceptanceRewards != null && def.acceptanceRewards.length != 0)
				old.acceptanceRewards = def.acceptanceRewards;
			if (def.achievementID != 0)
				old.achievementID = def.achievementID;
			if (def.childMissions != null && def.childMissions.length != 0)
				old.childMissions = def.childMissions;
			if (def.groupID != 0)
				old.groupID = def.groupID;
			if (def.missionRules != null)
				old.missionRules = def.missionRules;
			if (def.name != null)
				old.name = def.name;
			if (def.name != null)
				old.name = def.name;
			if (def.repeatable != null)
				old.repeatable = def.repeatable;
			if (def.tasks != null && def.tasks.length != 0)
				old.tasks = def.tasks;
			if (def.rewards != null && def.rewards.length != 0)
				old.rewards = def.rewards;
			if (def.version != 0)
				old.version = def.version;
			updateQuest(old);
		}
	}

	private void loadTransformer(File transformer, HashMap<Integer, MissionData> quests) {
		if (transformer.isFile()) {
			logger.debug("Loading transformer: '" + transformer.getPath() + "'...");
			try {
				// Find the transformer
				InputStream strm = new FileInputStream(transformer);

				// Load transformer
				XmlMapper mapper = new XmlMapper();
				MissionData def = mapper.reader().readValue(new String(strm.readAllBytes(), "UTF-8"),
						MissionData.class);
				strm.close();
				applyTransformer(def, quests);
			} catch (Exception e) {
				logger.error("Transformer failed to load: " + transformer.getPath(), e);
			}
		} else {
			logger.debug("Loading transformers from " + transformer.getPath() + "...");
			for (File tr : transformer.listFiles(t -> t.getName().endsWith(".xml") || t.isDirectory())) {
				loadTransformer(tr, quests);
			}
		}
	}

	private void loadTransformers(Class<?> cls, HashMap<Integer, MissionData> quests) {
		URL source = cls.getProtectionDomain().getCodeSource().getLocation();

		// Generate a base URL
		String baseURL = "";
		String fileName = "";
		try {
			File sourceFile = new File(source.toURI());
			fileName = sourceFile.getName();
			if (sourceFile.isDirectory()) {
				baseURL = source + (source.toString().endsWith("/") ? "" : "/");
			} else {
				baseURL = "jar:" + source + "!/";
			}
		} catch (Exception e) {
			return;
		}

		try {
			// Find the transformer document
			logger.debug("Loading transformers from " + fileName + "...");
			InputStream strm = new URL(baseURL + "questtransformers/index.json").openStream();
			JsonArray index = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonArray();
			strm.close();

			// Load all transformers
			for (JsonElement ele : index) {
				logger.debug("Loading transformer: 'questtransformers/" + ele.getAsString() + ".xml'...");
				try {
					// Find the transformer
					strm = new URL(baseURL + "questtransformers/" + ele.getAsString() + ".xml").openStream();

					// Load transformer
					XmlMapper mapper = new XmlMapper();
					MissionData def = mapper.reader().readValue(new String(strm.readAllBytes(), "UTF-8"),
							MissionData.class);
					strm.close();
					applyTransformer(def, quests);
				} catch (Exception e) {
					logger.error("Transformer failed to load: " + ele.getAsString() + " (" + fileName + ")", e);
				}
			}
		} catch (Exception e) {
			if (e instanceof FileNotFoundException)
				return;
			throw new RuntimeException(e);
		}
	}

	private void scanQuest(MissionData quest) {
		allQuests.put(quest.id, quest);
		logger.debug("Registered quest definition: " + quest.id + ": " + quest.name);
		if (quest.childMissions != null) {
			for (MissionData cQuest : quest.childMissions) {
				cQuest.parentQuestID = quest.id;
				scanQuest(cQuest);
			}
		}
	}

	private void updateQuest(MissionData quest) {
		allQuests.put(quest.id, quest);
		logger.debug("Updated quest definition: " + quest.id + ": " + quest.name);
		if (quest.childMissions != null) {
			for (MissionData cQuest : quest.childMissions) {
				cQuest.parentQuestID = quest.id;
				updateQuest(cQuest);
			}
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
			if (!data.entryExists("activequests") || !data.entryExists("lastupdate")
					|| data.getEntry("lastupdate").getAsLong() != lastQuestUpdateTime
					|| !data.entryExists("lastupdate_serverdata")
					|| !data.getEntry("lastupdate_serverdata").getAsString().equals(lastQuestUpdateVersion)
					|| !data.entryExists("lastupdate_serverver")
					|| !data.getEntry("lastupdate_serverver").getAsString().equals(questDataVersion)) {
				recomputeActiveQuests(save);
				return getActiveQuests(save);
			}

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
	public UserQuestInfo[] getUpcomingQuests(AccountSaveContainer save) {
		try {
			// Load active quests
			AccountDataContainer data = save.getSaveData().getChildContainer("quests");
			if (!data.entryExists("activequests") || !data.entryExists("lastupdate")
					|| data.getEntry("lastupdate").getAsLong() != lastQuestUpdateTime
					|| !data.entryExists("lastupdate_serverdata")
					|| !data.getEntry("lastupdate_serverdata").getAsString().equals(lastQuestUpdateVersion)
					|| !data.entryExists("lastupdate_serverver")
					|| !data.getEntry("lastupdate_serverver").getAsString().equals(questDataVersion)) {
				recomputeUpcomingQuests(save);
				return getUpcomingQuests(save);
			}

			// Create list
			ArrayList<UserQuestInfo> quests = new ArrayList<UserQuestInfo>();
			JsonArray upcomingQuests = data.getEntry("upcomingquests").getAsJsonArray();
			for (JsonElement ele : upcomingQuests) {
				// Find quest
				quests.add(getUserQuest(save, ele.getAsInt()));
			}

			// Return
			return quests.toArray(t -> new UserQuestInfo[t]);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void recomputeActiveQuests(AccountSaveContainer save) {
		try {
			// Load data container and prepare lists
			AccountDataContainer data = save.getSaveData().getChildContainer("quests");
			JsonArray active = new JsonArray();
			ArrayList<Integer> activeQuests = new ArrayList<Integer>();

			// Find active quests
			for (MissionData mission : this.quests.values()) {
				UserQuestInfo q = getUserQuest(save, mission.id);
				if (((mission.repeatable != null && mission.repeatable.equalsIgnoreCase("true")) || !q.isCompleted())
						&& q.isActive()) {
					active.add(q.getQuestID());
					activeQuests.add(q.getQuestID());
				}
			}

			// Save
			data.setEntry("activequests", active);
			data.setEntry("lastupdate", new JsonPrimitive(lastQuestUpdateTime));
			data.setEntry("lastupdate_serverdata", new JsonPrimitive(lastQuestUpdateVersion));
			data.setEntry("lastupdate_serverver", new JsonPrimitive(questDataVersion));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void recomputeUpcomingQuests(AccountSaveContainer save) {
		try {
			// Load data container and prepare lists
			AccountDataContainer data = save.getSaveData().getChildContainer("quests");
			JsonArray upcoming = new JsonArray();

			// Find upcoming quests
			for (MissionData mission : this.quests.values()) {
				UserQuestInfo q = getUserQuest(save, mission.id);
				if (!q.isCompleted() && !q.isActive()) {
					upcoming.add(q.getQuestID());
				}
			}

			// Save
			data.setEntry("upcomingquests", upcoming);
			data.setEntry("lastupdate", new JsonPrimitive(lastQuestUpdateTime));
			data.setEntry("lastupdate_serverdata", new JsonPrimitive(lastQuestUpdateVersion));
			data.setEntry("lastupdate_serverver", new JsonPrimitive(questDataVersion));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void recomputeQuests(AccountSaveContainer save) {
		try {
			// You may wonder:
			// Why arent i calling the above computation methods here?
			//
			// Well, optimization, the above ones are designed to work without one another,
			// the one here uses data gathered from computing active quests to speed up
			// computing upcoming quests instead of having to go through the database for
			// each quest again after that

			// Load data container and prepare lists
			AccountDataContainer data = save.getSaveData().getChildContainer("quests");
			JsonArray active = new JsonArray();
			JsonArray upcoming = new JsonArray();
			ArrayList<Integer> activeQuests = new ArrayList<Integer>();

			// Find active quests
			for (MissionData mission : this.quests.values()) {
				UserQuestInfo q = getUserQuest(save, mission.id);
				if (((mission.repeatable != null && mission.repeatable.equalsIgnoreCase("true")) || !q.isCompleted())
						&& q.isActive()) {
					active.add(q.getQuestID());
					activeQuests.add(q.getQuestID());
				}
			}

			// Find upcoming quests
			for (MissionData mission : this.quests.values()) {
				UserQuestInfo q = getUserQuest(save, mission.id);
				if (!activeQuests.contains(q.getQuestID()) && !q.isCompleted()) {
					upcoming.add(q.getQuestID());
				}
			}

			// Save
			data.setEntry("activequests", active);
			data.setEntry("upcomingquests", upcoming);
			data.setEntry("lastupdate", new JsonPrimitive(lastQuestUpdateTime));
			data.setEntry("lastupdate_serverdata", new JsonPrimitive(lastQuestUpdateVersion));
			data.setEntry("lastupdate_serverver", new JsonPrimitive(questDataVersion));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private class UserQuestInfoImpl extends UserQuestInfo {

		private MissionData def;
		private AccountDataContainer data;
		private AccountSaveContainer save;
		private JsonObject questInfoData;

		private Object questInfoLock = new Object();

		private void populateQuestInfoIfNeeded() {
			if (questInfoData != null)
				return;

			// Lock
			synchronized (questInfoLock) {
				if (questInfoData != null)
					return; // Another thread did it before us

				// Populate data
				try {
					data = save.getSaveData().getChildContainer("quests");
					if (data.entryExists("quest-" + def.id)) {
						// Load quest
						questInfoData = data.getEntry("quest-" + def.id).getAsJsonObject();
						if (!questInfoData.has("accepted"))
							questInfoData.addProperty("accepted", false);
						if (!questInfoData.has("started"))
							questInfoData.addProperty("started", false);
						if (!questInfoData.has("completed"))
							questInfoData.addProperty("completed", false);
					} else {
						// Not found
						questInfoData = new JsonObject();
						questInfoData.addProperty("completed", false);
						questInfoData.addProperty("accepted", false);
						questInfoData.addProperty("started", false);
						questInfoData.add("payload", new JsonObject());
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		public UserQuestInfoImpl(MissionData def, AccountSaveContainer save) {
			this.def = def;
			this.save = save;

			// Load object
			try {
				data = save.getSaveData().getChildContainer("quests");
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
			populateQuestInfoIfNeeded();

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
			JsonObject payload = questInfoData.get("payload").getAsJsonObject();
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
			populateQuestInfoIfNeeded();

			// Start quest
			startQuest();

			// Load payload
			JsonObject payload = questInfoData.get("payload").getAsJsonObject();
			JsonObject questPayload;
			if (payload.has("quest"))
				questPayload = payload.get("quest").getAsJsonObject();
			else {
				questPayload = new JsonObject();
				payload.add("quest", questPayload);
			}
			questPayload.addProperty("accepted", true);
			questInfoData.addProperty("accepted", true);

			// Save
			try {
				data.setEntry("quest-" + def.id, questInfoData);
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
							AccountDataContainer currency = save.getSaveData().getChildContainer("currency");
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

			// Recompute active quests
			AsyncTaskManager.runAsync(() -> {
				recomputeQuests(save);
			});
		}

		@Override
		public SetTaskStateResultData handleTaskCall(int taskID, String payloadStr, boolean completed, int invContainer,
				SetCommonInventoryRequestData[] requests) throws IOException {
			populateQuestInfoIfNeeded();

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
			if (isCompleted() && (mission.repeatable == null || mission.repeatable.equalsIgnoreCase("false"))) {
				// Invalid request
				SetTaskStateResultData resp = new SetTaskStateResultData();
				resp.success = false;
				resp.status = SetTaskStateResultData.SetTaskStateResultStatuses.NON_REPEATABLE_MISSION;
				return resp;
			}
			// FIXME: implement more checks

			// Check if active
			if (!questInfoData.get("started").getAsBoolean()) {
				// Start quest
				startQuest();
			}

			// Update task
			// Load payload
			JsonObject payload = questInfoData.get("payload").getAsJsonObject();
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
				data.setEntry("quest-" + def.id, questInfoData);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			// Prepare response
			SetTaskStateResultData resp = new SetTaskStateResultData();
			resp.status = SetTaskStateResultData.SetTaskStateResultStatuses.TASK_CAN_BE_DONE;
			resp.success = true;

			// Update inventories
			if (requests != null && requests.length != 0) {
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
									AccountDataContainer currency = save.getSaveData().getChildContainer("currency");
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

					// Recompute active quests
					AsyncTaskManager.runAsync(() -> {
						recomputeQuests(save);
					});
				}
			}

//			// Recompute active quests // FIXME disabled for testing, may need enabling
//			AsyncTaskManager.runAsync(() -> {
//				recomputeQuests(save);
//			});
			return resp;
		}

		private boolean isCompletedMission(MissionData mission) {
			// Check completed tasks
			if (mission.tasks != null) {
				int taskCount = mission.tasks.length;
				int completedTasks = (int) Stream.of(mission.tasks).filter(t -> t.completed > 0).count();
				if (completedTasks >= taskCount) {
					// The following checks are disabled as disabling them may fix a bug, if it
					// causes issues instead they should be re-enabled

//					// Tasks completed
//					// Check child quests
//					if (mission.childMissions != null) {
//						for (MissionData ch : mission.childMissions) {
//							// Check child quest mission
//							if (!isCompletedMission(ch))
//								return false;
//						}
//					}

					// This quest has been completed
					return true;
				}
				return false;
			} else
				return true; // No tasks
		}

		@Override
		public void startQuest() {
			populateQuestInfoIfNeeded();

			// Check status
			if (questInfoData.get("started").getAsBoolean())
				throw new IllegalArgumentException("Quest is already started");

			// Update status
			questInfoData.addProperty("started", true);

			try {
				// Save
				data.setEntry("quest-" + def.id, questInfoData);

				// Check
				if (def.parentQuestID > 0)
					return;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			// Recompute active quests
			AsyncTaskManager.runAsync(() -> {
				recomputeQuests(save);
			});
		}

		@Override
		public void completeQuest() {
			populateQuestInfoIfNeeded();

			// Update status
			questInfoData.addProperty("completed", true);
			questInfoData.addProperty("started", false);

			try {
				// Save
				data.setEntry("quest-" + def.id, questInfoData);

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

			// Recompute active quests
			AsyncTaskManager.runAsync(() -> {
				recomputeQuests(save);
			});
		}

		@Override
		public boolean isActive() {
			// Check parent
			if (def.parentQuestID > 0) {
				UserQuestInfo quest = getUserQuest(save, def.parentQuestID);
				if (quest != null)
					if (!quest.isActive())
						return false;
			}

			// Populate
			populateQuestInfoIfNeeded();

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
									if (!questInfoData.get("accepted").getAsBoolean())
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
								// FIXME: quests should be recomputed when quest items change

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
								// FIXME: quests need recomputation if this changes

								// Check member only
								if (req.value.equalsIgnoreCase("true")) {
									// TODO: membership check
									return false;
								}

								break;
							}

							// Rank rule
							case MissionRulesBlock.PrerequisiteInfoBlock.PrerequisiteRuleTypes.RANK: {
								// FIXME: quests need recomputation if this changes

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
			return true;
		}

		@Override
		public boolean isCompleted() {
			populateQuestInfoIfNeeded();
			return questInfoData.get("completed").getAsBoolean();
		}

		@Override
		public boolean isStarted() {
			populateQuestInfoIfNeeded();
			return questInfoData.get("started").getAsBoolean();
		}

		@Override
		public void resetQuest() {
			// Reset
			questInfoData = new JsonObject();
			questInfoData.addProperty("completed", false);
			questInfoData.addProperty("accepted", false);
			questInfoData.addProperty("started", false);
			questInfoData.add("payload", new JsonObject());

			// Save
			try {
				data.setEntry("quest-" + def.id, questInfoData);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			// Recompute
			AsyncTaskManager.runAsync(() -> {
				recomputeQuests(save);
			});
		}

	}

	@Override
	public void reload() {
		// Trigger a reload on all servers
		lastReloadTime = System.currentTimeMillis();
		try {
			CommonDataManager.getInstance().getContainer("QUESTMANAGER").setEntry("lastreload",
					new JsonPrimitive(lastReloadTime));
		} catch (IOException e) {
		}
		logger.info("Reloading quest manager...");
		loadQuests();
	}

}
