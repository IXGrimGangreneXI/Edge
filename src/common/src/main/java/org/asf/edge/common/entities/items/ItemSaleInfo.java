package org.asf.edge.common.entities.items;

/**
 * 
 * Item sale information container
 * 
 * @author Sky Swimmer
 *
 */
public class ItemSaleInfo {

	private String name;
	private long startTime;
	private long endTime;

	private float modifier;
	private int[] categories;
	private int[] itemIds;

	private boolean memberOnly;

	public ItemSaleInfo(String name, long startTime, long endTime, float modifier, int[] categories, int[] itemIds,
			boolean memberOnly) {
		this.name = name;
		this.startTime = startTime;
		this.endTime = endTime;
		this.modifier = modifier;
		this.categories = categories;
		this.itemIds = itemIds;
		this.memberOnly = memberOnly;
	}

	/**
	 * Retrieves the sale name
	 * 
	 * @return Sale name string
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves the start timestamp
	 * 
	 * @return Sale start timestamp
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * Retrieves the end timestamp
	 * 
	 * @return Sale end timestamp
	 */
	public long getEndTime() {
		return endTime;
	}

	/**
	 * Retrieves the sale modifier
	 * 
	 * @return Sale modifier
	 */
	public float getSaleModifier() {
		return modifier;
	}

	/**
	 * Retrieves sale categories
	 * 
	 * @return Array of category IDs
	 */
	public int[] getCategories() {
		if (categories == null)
			return new int[0];
		return categories;
	}

	/**
	 * Retrieves sale items
	 * 
	 * @return Array of item IDs
	 */
	public int[] getItemIDs() {
		if (itemIds == null)
			return new int[0];
		return itemIds;
	}

	/**
	 * Checks if the sale is member-only
	 * 
	 * @return True if member-only, false otherwise
	 */
	public boolean isMemberOnly() {
		return memberOnly;
	}

	/**
	 * Checks if the sale is active
	 * 
	 * @return True if active, false otherwise
	 */
	public boolean isActive() {
		return System.currentTimeMillis() >= startTime && System.currentTimeMillis() < endTime;
	}

	/**
	 * Checks if the sale is upcoming
	 * 
	 * @return True if upcoming, false otherwise
	 */
	public boolean isUpcoming() {
		return System.currentTimeMillis() < startTime && System.currentTimeMillis() < endTime;
	}

	/**
	 * Checks if the sale has expired
	 * 
	 * @return True if expired, false otherwise
	 */
	public boolean hasExpired() {
		return System.currentTimeMillis() >= endTime;
	}

}
