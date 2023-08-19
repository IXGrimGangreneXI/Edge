package org.asf.edge.common.services.leaderboard;

import org.asf.edge.common.services.AbstractService;
import org.asf.edge.common.services.ServiceManager;

/**
 * 
 * Leaderboard Management Service
 * 
 * @author Sky Swimmer
 *
 */
public abstract class LeaderboardManager extends AbstractService {

	/**
	 * Retrieves the active messaging service
	 * 
	 * @return LeaderboardManager instance
	 */
	public static LeaderboardManager getInstance() {
		return ServiceManager.getService(LeaderboardManager.class);
	}

	/**
	 * Registers leaderboards
	 * 
	 * @param name Leaderboard name
	 */
	public abstract void registerLeaderboard(String name);

	/**
	 * Retrieves leaderboards, registers if not present
	 * 
	 * @param name Leaderboard name
	 * @return Leaderboard instance
	 */
	public abstract Leaderboard getLeaderboard(String name);

}
