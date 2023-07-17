package org.asf.edge.common.entities.achivements;

/**
 * 
 * Rank multiplier information container
 * 
 * @author Sky Swimmer
 *
 */
public class RankMultiplierInfo {

	private RankTypeID typeID;
	private int multiplierFactor;
	private long expiryTime;

	public RankMultiplierInfo(RankTypeID typeID, int multiplierFactor, long expiryTime) {
		this.typeID = typeID;
		this.multiplierFactor = multiplierFactor;
		this.expiryTime = expiryTime;
	}

	/**
	 * Retrieves the rank point type this multiplier applies to
	 * 
	 * @return RankTypeID value
	 */
	public RankTypeID getPointType() {
		return typeID;
	}

	/**
	 * Retrieves the multiplication factor
	 * 
	 * @return Rank point multiplication factor
	 */
	public int getMultiplicationFactor() {
		return multiplierFactor;
	}

	/**
	 * Retrieves the multiplier expiry timestamp
	 * 
	 * @return Multiplier expiry timestamp
	 */
	public long getExpiryTime() {
		return expiryTime;
	}

	/**
	 * Checks if the multiplier is still valid
	 * 
	 * @return True if valid, false otherwise
	 */
	public boolean isValid() {
		return System.currentTimeMillis() < expiryTime;
	}

}
