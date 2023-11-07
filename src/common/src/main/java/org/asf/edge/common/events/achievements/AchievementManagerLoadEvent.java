package org.asf.edge.common.events.achievements;

import org.asf.edge.common.services.achievements.AchievementManager;
import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Achievement manager load event - called after the achievement manager loads
 * or reloads achievement and rank definitions
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("achievements.achievementmanager.load")
public class AchievementManagerLoadEvent extends EventObject {

	private AchievementManager achievementManager;

	public AchievementManagerLoadEvent(AchievementManager achievementManager) {
		this.achievementManager = achievementManager;
	}

	@Override
	public String eventPath() {
		return "achievements.achievementmanager.load";
	}

	/**
	 * Retrieves the achievement manager instance
	 * 
	 * @return AchievementManager instance
	 */
	public AchievementManager getAchievementManager() {
		return achievementManager;
	}

}
