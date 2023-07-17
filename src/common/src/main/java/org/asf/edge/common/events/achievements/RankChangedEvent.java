package org.asf.edge.common.events.achievements;

import org.asf.edge.common.entities.achivements.EntityRankInfo;
import org.asf.edge.common.entities.achivements.RankInfo;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.achievements.AchievementManager;
import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Rank changed event - called when XP changes of a player, dragon or clan
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("achievements.ranks.changed")
public class RankChangedEvent extends EventObject {

	private AchievementManager achievementManager;

	private AccountSaveContainer save;
	private EntityRankInfo rankInfo;

	private RankInfo lastRank;
	private RankInfo newRank;

	private int lastTotalScore;
	private int newTotalScore;

	public RankChangedEvent(AchievementManager achievementManager, AccountSaveContainer save, EntityRankInfo rankInfo,
			RankInfo lastRank, RankInfo newRank, int lastScore, int newScore) {
		this.achievementManager = achievementManager;
		this.save = save;

		this.rankInfo = rankInfo;
		this.lastRank = lastRank;
		this.newRank = newRank;
		this.lastTotalScore = lastScore;
		this.newTotalScore = newScore;
	}

	@Override
	public String eventPath() {
		return "achievements.ranks.changed";
	}

	/**
	 * Retrieves the achievement manager instance
	 * 
	 * @return AchievementManager instance
	 */
	public AchievementManager getAchievementManager() {
		return achievementManager;
	}

	/**
	 * Retrieves the account save container
	 * 
	 * @return AccountSaveContainer instance
	 */
	public AccountSaveContainer getSave() {
		return save;
	}

	/**
	 * Retrieves the account object instance
	 * 
	 * @return AccountObject instance
	 */
	public AccountObject getAccount() {
		return save.getAccount();
	}

	/**
	 * Retrieves the entity rank info object
	 * 
	 * @return EntityRankInfo instance
	 */
	public EntityRankInfo getEntityRank() {
		return rankInfo;
	}

	/**
	 * Checks if the rank changed
	 * 
	 * @return True if changed, false otherwise
	 */
	public boolean hasRankChanged() {
		if (lastRank == null && newRank != null)
			return true;
		else if (lastRank != null && newRank == null)
			return true;
		else if (lastRank.getID() != newRank.getID())
			return true;
		return false;
	}

	/**
	 * Retrieves the last rank
	 * 
	 * @return RankInfo instance or null
	 */
	public RankInfo getLastRank() {
		return lastRank;
	}

	/**
	 * Retrieves the current rank
	 * 
	 * @return RankInfo instance or null
	 */
	public RankInfo getNewRank() {
		return newRank;
	}

	/**
	 * Retrieves the last total score
	 * 
	 * @return Last total score
	 */
	public int getLastTotalScore() {
		return lastTotalScore;
	}

	/**
	 * Retrieves the current total score
	 * 
	 * @return Current total score
	 */
	public int getNewTotalScore() {
		return newTotalScore;
	}

}
