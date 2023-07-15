package org.asf.edge.gameplayapi.services.achievements;

import org.asf.edge.common.services.AbstractService;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.gameplayapi.entities.achivements.RankInfo;
import org.asf.edge.gameplayapi.xmls.achievements.UserRankData;

/**
 * 
 * Edge achievement manager - manages achievements, XP and ranks
 * 
 * @author Sky Swimmer
 *
 */
public abstract class AchievementManager extends AbstractService {

	/**
	 * Retrieves the active achievement manager
	 * 
	 * @return AchievementManager instance
	 */
	public static AchievementManager getInstance() {
		return ServiceManager.getService(AchievementManager.class);
	}

	/**
	 * Retrieves rank definitions
	 * 
	 * @return Array of RankInfo instances
	 */
	public abstract RankInfo[] getRankDefinitions();

	/**
	 * Retrieves rank definitions
	 * 
	 * @param id Rank ID
	 * @return RankInfo instance or null
	 */
	public abstract RankInfo getRankDefinition(int id);

	/**
	 * Registers rank definitions
	 * 
	 * @param id  Rank ID
	 * @param def Rank definition
	 */
	public abstract void registerRankDefinition(int id, UserRankData def);

	/**
	 * Updates rank definitions
	 * 
	 * @param id  Rank ID
	 * @param def Updated definition
	 */
	public abstract void updateRankDefinition(int id, UserRankData def);

	/**
	 * Called to reload achievement and rank data from disk
	 */
	public abstract void reload();
}
