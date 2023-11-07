package org.asf.edge.gameplayapi.events.quests;

import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.gameplayapi.entities.quests.UserQuestInfo;
import org.asf.edge.gameplayapi.events.GameplayApiServerEvent;
import org.asf.edge.gameplayapi.services.quests.QuestManager;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Quest reset event - called when quests are reseted
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("quests.questmanager.quest.reset")
public class QuestResetEvent extends GameplayApiServerEvent {

	private UserQuestInfo quest;
	private AccountSaveContainer save;
	private QuestManager questManager;

	public QuestResetEvent(UserQuestInfo quest, AccountSaveContainer save, QuestManager questManager) {
		this.quest = quest;
		this.save = save;
		this.questManager = questManager;
	}

	@Override
	public String eventPath() {
		return "quests.questmanager.quest.reset";
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

}
