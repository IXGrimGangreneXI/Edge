package org.asf.edge.gameplayapi.entities.quests;

import java.io.IOException;

import org.asf.edge.gameplayapi.xmls.inventories.SetCommonInventoryRequestData;
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
	 * Starts the quest
	 * 
	 * @throws IllegalArgumentException If the quest is already started
	 */
	public abstract void startQuest();

	/**
	 * Completes the quest
	 */
	public abstract void completeQuest();

	/**
	 * Accepts and starts the quest
	 */
	public abstract void acceptQuest();

	/**
	 * Resets the quest, wipes progression and marks it unfinished
	 */
	public abstract void resetQuest();

	/**
	 * Called to handle task state calls
	 * 
	 * @param taskID       Task ID
	 * @param payload      New value for the 'payload' field
	 * @param completed    True if the task has been completed, false otherwise
	 * @param invContainer Inventory container ID
	 * @param requests     Inventory requests (may be empty)
	 * @return SetTaskStateResultData instance
	 * @throws IOException If updating fails
	 */
	public abstract SetTaskStateResultData handleTaskCall(int taskID, String payload, boolean completed,
			int invContainer, SetCommonInventoryRequestData[] requests) throws IOException;

	/**
	 * Checks if the quest is active
	 * 
	 * @return True if active, false otherwise
	 */
	public abstract boolean isActive();

	/**
	 * Checks if the quest is completed
	 * 
	 * @return True if completed, false otherwise
	 */
	public abstract boolean isCompleted();

	/**
	 * Checks if the quest has been started
	 * 
	 * @return True if started, false otherwise
	 */
	public abstract boolean isStarted();

}
