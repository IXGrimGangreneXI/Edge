package org.asf.edge.gameplayapi.services.achievements;

import org.asf.edge.common.services.AbstractService;
import org.asf.edge.common.services.ServiceManager;

/**
 * 
 * Edge rank manager - manages achievements, XP and ranks
 * 
 * @author Sky Swimmer
 *
 */
public abstract class RankManager extends AbstractService {

	/**
	 * Retrieves the active rank manager
	 * 
	 * @return RankManager instance
	 */
	public static RankManager getInstance() {
		return ServiceManager.getService(RankManager.class);
	}

	/**
	 * Called to reload achievement and rank data from disk
	 */
	public abstract void reload();
}
