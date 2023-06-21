package org.asf.edge.gameplayapi.services.quests.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.account.AccountDataContainer;
import org.asf.edge.common.account.AccountSaveContainer;
import org.asf.edge.gameplayapi.entities.quests.UserQuestInfo;
import org.asf.edge.gameplayapi.entities.quests.UserQuestStatus;
import org.asf.edge.gameplayapi.services.quests.QuestManager;
import org.asf.edge.gameplayapi.xmls.quests.MissionData;
import org.asf.edge.gameplayapi.xmls.quests.edgespecific.QuestRegistryManifest;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.JsonObject;

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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserQuestInfo[] getActiveQuests(AccountSaveContainer save) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserQuestInfo[] getUnlockedQuests(AccountSaveContainer save) {
		// TODO Auto-generated method stub
		return null;
	}

	private class UserQuestInfoImpl extends UserQuestInfo {

		private MissionData def;
		private AccountSaveContainer save;
		private UserQuestStatus status;
		private JsonObject questInfo;

		public UserQuestInfoImpl(MissionData def, AccountSaveContainer save) {
			this.def = def;
			this.save = save;

			// Load object
			try {
				AccountDataContainer data = save.getSaveData().getChildContainer("quests");
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
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public UserQuestStatus getStatus() {
			return status;
		}

		@Override
		public void startQuest() {
			// TODO Auto-generated method stub

		}

		@Override
		public void completeQuest() {
			// TODO Auto-generated method stub

		}

	}

}
