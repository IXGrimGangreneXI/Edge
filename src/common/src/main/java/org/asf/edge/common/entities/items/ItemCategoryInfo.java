package org.asf.edge.common.entities.items;

/**
 * 
 * Item category information
 * 
 * @author Sky Swimmer
 *
 */
public class ItemCategoryInfo {
	private int id;
	private String categoryName;
	private String iconName;

	public ItemCategoryInfo(int id, String categoryName, String iconName) {
		this.id = id;
		this.categoryName = categoryName;
		this.iconName = iconName;
	}

	/**
	 * Retrieves the category ID
	 * 
	 * @return Category ID
	 */
	public int getCategoryID() {
		return id;
	}

	/**
	 * Retrieves the category name
	 * 
	 * @return Category name
	 */
	public String getCategoryName() {
		return categoryName;
	}

	/**
	 * Retrieves the category icon name
	 * 
	 * @return Category icon name or null
	 */
	public String getCategoryIconName() {
		return iconName;
	}

}
