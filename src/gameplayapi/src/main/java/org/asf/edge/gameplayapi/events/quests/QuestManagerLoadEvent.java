package org.asf.edge.gameplayapi.events.quests;

import org.asf.edge.gameplayapi.events.GameplayApiServerEvent;
import org.asf.edge.gameplayapi.services.quests.QuestManager;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Quest manager load event - called after the quest manager loads or reloads
 * quest definitions
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("quests.questmanager.load")
public class QuestManagerLoadEvent extends GameplayApiServerEvent {

	private QuestManager questManager;

	public QuestManagerLoadEvent(QuestManager questManager) {
		this.questManager = questManager;
	}

	@Override
	public String eventPath() {
		return "quests.questmanager.load";
	}

	/**
	 * Retrieves the quest manager instance
	 * 
	 * @return QuestManager instance
	 */
	public QuestManager getQuestManager() {
		return questManager;
	}

}
