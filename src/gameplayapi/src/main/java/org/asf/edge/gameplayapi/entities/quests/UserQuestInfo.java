package org.asf.edge.gameplayapi.entities.quests;

import org.asf.edge.gameplayapi.xmls.inventories.CommonInventoryRequestData;
import org.asf.edge.gameplayapi.xmls.quests.MissionData;
import org.asf.edge.gameplayapi.xmls.quests.SetTaskStateResultData;

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
	 * Changes the 'accepted' field of the quest
	 * 
	 * @param accepted New state for the 'accepted' field
	 */
	public abstract void setAcceptedField(boolean accepted);

	/**
	 * Changes the 'failed' field of a task
	 * 
	 * @param taskID Task ID
	 * @param failed New state for the 'failed' field
	 */
	public abstract void setTaskFailedField(int taskID, boolean failed);

	/**
	 * Called to handle task state calls
	 * 
	 * @param taskID       Task ID
	 * @param payload      New value for the 'payload' field
	 * @param completed    True if the task has been completed, false otherwise
	 * @param invContainer Inventory container ID
	 * @param requests     Inventory requests (may be empty)
	 * @return SetTaskStateResultData instance
	 */
	public abstract SetTaskStateResultData handleTaskCall(int taskID, String payload, boolean completed,
			int invContainer, CommonInventoryRequestData[] requests);

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
