package org.asf.edge.gameplayapi.entities.achivements;

import org.asf.edge.gameplayapi.xmls.achievements.UserRankData;

/**
 * 
 * Rank definition information
 * 
 * @author Sky Swimmer
 *
 */
public class RankInfo {

	private UserRankData raw;

	private int rankID;
	private String rankName;
	private String rankDescription;
	private int value;
	private int pointTypeID;
	private int globalRankID;

	public RankInfo(UserRankData raw) {
		this.raw = raw;
		reload();
	}

	public void reload() {
		this.rankID = raw.rankID;
		this.rankName = raw.rankName;
		this.rankDescription = raw.rankDescription;
		this.value = raw.value;
		this.pointTypeID = raw.pointTypeID;
		this.globalRankID = raw.globalRankID;
	}

	/**
	 * Retrieves the rank ID
	 * 
	 * @return Rank ID
	 */
	public int getID() {
		return rankID;
	}

	/**
	 * Retrieves the rank global ID
	 * 
	 * @return Rank global ID
	 */
	public int getGlobalID() {
		return globalRankID;
	}

	/**
	 * Retrieves the rank point type ID
	 * 
	 * @return Rank point type ID
	 */
	public int getPointTypeID() {
		return pointTypeID;
	}

	/**
	 * Retrieves the rank name
	 * 
	 * @return Rank name
	 */
	public String getName() {
		return rankName;
	}

	/**
	 * Retrieves the rank description
	 * 
	 * @return Rank description
	 */
	public String getDescription() {
		return rankDescription;
	}

	/**
	 * Retrieves the point value needed to reach the rank
	 * 
	 * @return Rank point value
	 */
	public int getValue() {
		return value;
	}

	/**
	 * Retrieves the raw object of the rank definition
	 * 
	 * @return UserRankData instance
	 */
	public UserRankData getRawObject() {
		return raw;
	}

}
