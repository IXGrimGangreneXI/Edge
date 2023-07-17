package org.asf.edge.common.entities.achivements;

import org.asf.edge.common.services.achievements.AchievementManager;

/**
 * 
 * Entity rank info container
 * 
 * @author Sky Swimmer
 *
 */
public abstract class EntityRankInfo {

	/**
	 * Retrieves the entity ID string
	 * 
	 * @return Entity ID string, either a dragon ID or user save ID depending on
	 *         point type
	 */
	public abstract String getEntityID();

	/**
	 * Retrieves the rank type ID
	 * 
	 * @return RankTypeID value
	 */
	public abstract RankTypeID getTypeID();

	/**
	 * Retrieves the total score
	 * 
	 * @return Rank total score value
	 */
	public abstract int getTotalScore();

	/**
	 * Retrieves the rank ID
	 * 
	 * @return Rank ID
	 */
	public int getRankID() {
		RankInfo r = getRank();
		if (r == null)
			return -1;
		return r.getID();
	}

	/**
	 * Retrieves the rank info object
	 * 
	 * @return RankInfo instance or null if invalid
	 */
	public RankInfo getRank() {
		// Find rank
		RankInfo match = null;
		for (RankInfo rank : AchievementManager.getInstance()
				.getRankDefinitionsByPointType(getTypeID().getPointTypeID())) {
			// Check
			if (getTotalScore() >= rank.getValue()) {
				// Check current
				if (match == null || match.getValue() < rank.getValue())
					match = rank;
			}
		}

		// Return
		return match;
	}

	/**
	 * Reassigns the rank points
	 * 
	 * @param value New score
	 */
	public abstract void setTotalScore(int value);

	/**
	 * Adds points to the rank
	 * 
	 * @param value Points to add
	 * @return Actual amount of points that were added
	 */
	public abstract int addPoints(int value);

}
