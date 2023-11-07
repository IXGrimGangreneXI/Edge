package org.asf.edge.gameplayapi.events.quests;

import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.gameplayapi.entities.quests.UserQuestInfo;
import org.asf.edge.gameplayapi.events.GameplayApiServerEvent;
import org.asf.edge.gameplayapi.services.quests.QuestManager;
import org.asf.edge.gameplayapi.xmls.quests.MissionData;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Quest task completion event - called when quest tasks are completed
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("quests.questmanager.quest.task.completed")
public class QuestTaskCompletionEvent extends GameplayApiServerEvent {

	private UserQuestInfo quest;
	private AccountSaveContainer save;
	private QuestManager questManager;
	private MissionData.TaskBlock task;
	private String payload;

	public QuestTaskCompletionEvent(UserQuestInfo quest, MissionData.TaskBlock task, String payload,
			AccountSaveContainer save, QuestManager questManager) {
		this.task = task;
		this.payload = payload;
		this.quest = quest;
		this.save = save;
		this.questManager = questManager;
	}

	@Override
	public String eventPath() {
		return "quests.questmanager.quest.task.completed";
	}

	/**
	 * Retrieves the quest manager instance
	 * 
	 * @return QuestManager instance
	 */
	public QuestManager getQuestManager() {
		return questManager;
	}

	/**
	 * Retrieves the quest instance
	 * 
	 * @return UserQuestInfo instance
	 */
	public UserQuestInfo getQuest() {
		return quest;
	}

	/**
	 * Retrieves the account save container
	 * 
	 * @return AccountSaveContainer instance
	 */
	public AccountSaveContainer getAccountSave() {
		return save;
	}

	/**
	 * Retrieves the account object
	 * 
	 * @return AccountObject instance
	 */
	public AccountObject getAccount() {
		return save.getAccount();
	}

	/**
	 * Retrieves the quest task object
	 * 
	 * @return TaskBlock instance
	 */
	public MissionData.TaskBlock getTask() {
		return task;
	}

	/**
	 * Retrieves the task update payload
	 * 
	 * @return Task update payload string
	 */
	public String getTaskUpdatePayload() {
		return payload;
	}

}
