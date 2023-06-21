package org.asf.edge.gameplayapi.entities.quests;

import org.asf.edge.gameplayapi.xmls.quests.MissionData;

/**
 * 
 * User quest information container
 * 
 * @author Sky Swimmer
 *
 */
public abstract class UserQuestInfo {

	/**
	 * Retrieves the quest ID
	 * 
	 * @return Quest ID
	 */
	public abstract int getQuestID();

	/**
	 * Retrieves the quest definition
	 * 
	 * @return MissionData instance
	 */
	public abstract MissionData getDef();

	/**
	 * Retrieves the quest data object (retrieves the modified def object containing
	 * progress)
	 * 
	 * @return MissionData instance
	 */
	public abstract MissionData getData();

	/**
	 * Retrieves the quest status
	 * 
	 * @return UserQuestStatus value
	 */
	public abstract UserQuestStatus getStatus();

	/**
	 * Starts the quest
	 * 
	 * @throws IllegalArgumentException If the quest is not inactive
	 */
	public abstract void startQuest();

	/**
	 * Completes the quest
	 * 
	 * @throws IllegalArgumentException If the quest is already completed
	 */
	public abstract void completeQuest();

	/**
	 * Checks if the quest is active
	 * 
	 * @return True if active, false otherwise
	 */
	public boolean isActive() {
		return getStatus() == UserQuestStatus.ACTIVE;
	}

	/**
	 * Checks if the quest is completed
	 * 
	 * @return True if completed, false otherwise
	 */
	public boolean isCompleted() {
		return getStatus() == UserQuestStatus.COMPLETED;
	}

}
