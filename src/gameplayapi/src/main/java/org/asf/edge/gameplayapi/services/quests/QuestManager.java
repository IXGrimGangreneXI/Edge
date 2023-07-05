package org.asf.edge.gameplayapi.services.quests;

import org.asf.edge.common.services.AbstractService;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.gameplayapi.entities.quests.UserQuestInfo;
import org.asf.edge.gameplayapi.xmls.quests.MissionData;

/**
 * 
 * EDGE Quest/mission Manager
 * 
 * @author Sky Swimmer
 *
 */
public abstract class QuestManager extends AbstractService {

	/**
	 * Retrieves the active quest manager
	 * 
	 * @return QuestManager instance
	 */
	public static QuestManager getInstance() {
		return ServiceManager.getService(QuestManager.class);
	}

	/**
	 * Retrieves all quest IDs
	 * 
	 * @return Array of quest IDs
	 */
	public abstract int[] getQuestIDs();

	/**
	 * Retrieves all quest IDs including their child quests
	 * 
	 * @return Array of quest IDs
	 */
	public abstract int[] getAllQuestIDs();

	/**
	 * Retrieves quest definitions by ID
	 * 
	 * @param id Quest ID
	 * @return MissionData instance or null
	 */
	public abstract MissionData getQuestDef(int id);

	/**
	 * Retrieves all quest definitions
	 * 
	 * @return Array of MissionData instances
	 */
	public abstract MissionData[] getQuestDefs();

	/**
	 * Retrieves all quest definitions including their child quests
	 * 
	 * @return Array of MissionData instances
	 */
	public abstract MissionData[] getAllQuestDefs();

	/**
	 * Retrieves quest information containers for a specific user
	 * 
	 * @param save The save to use to retrieve quest information
	 * @param id   Quest ID
	 * @return UserQuestInfo or null
	 */
	public abstract UserQuestInfo getUserQuest(AccountSaveContainer save, int id);

	/**
	 * Retrieves an array of completed quests
	 * 
	 * @param save The save to use to retrieve quest information
	 * @return Array of UserQuestInfo instances
	 */
	public abstract UserQuestInfo[] getCompletedQuests(AccountSaveContainer save);

	/**
	 * Retrieves an array of quests active in the world
	 * 
	 * @param save The save to use to retrieve quest information
	 * @return Array of UserQuestInfo instances
	 */
	public abstract UserQuestInfo[] getActiveQuests(AccountSaveContainer save);

	/**
	 * Retrieves an array of quests that are not active
	 * 
	 * @param save The save to use to retrieve quest information
	 * @return Array of UserQuestInfo instances
	 */
	public abstract UserQuestInfo[] getUpcomingQuests(AccountSaveContainer save);

	/**
	 * Recomputes active/upcoming quests for a save container, <b>THIS IS A
	 * INTENSIVE PROCESS, ONLY DO THIS AFTER QUESTS COMPLETE IN A ASYNC TASK</b>
	 * 
	 * @param save Save container to recompute quests for
	 */
	public abstract void recomputeQuests(AccountSaveContainer save);

	/**
	 * Called to reload quests from disk
	 */
	public abstract void reload();

}
