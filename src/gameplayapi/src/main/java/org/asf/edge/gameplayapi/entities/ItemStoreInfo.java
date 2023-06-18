package org.asf.edge.gameplayapi.entities;

/**
 * 
 * Item store information container
 * 
 * @author Sky Swimmer
 *
 */
public class ItemStoreInfo {
	private int id;
	private String name;
	private String description;
	private ItemInfo[] items;

	public ItemStoreInfo(int id, String name, String description, ItemInfo[] items) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.items = items;
	}

	/**
	 * Retrieves all items in this store
	 * 
	 * @return Array of ItemInfo instances
	 */
	public ItemInfo[] getItems() {
		return items;
	}

	/**
	 * Retrieves items by ID
	 * 
	 * @param id Item ID
	 * @return ItemInfo instance or null
	 */
	public ItemInfo getItem(int id) {
		for (ItemInfo nd : items)
			if (nd.getID() == id)
				return nd;
		return null;
	}

	/**
	 * Retrieves the store ID
	 * 
	 * @return Store ID
	 */
	public int getID() {
		return id;
	}

	/**
	 * Retrieves the store name
	 * 
	 * @return Store name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves the store description
	 * 
	 * @return Store description
	 */
	public String getDescription() {
		return description;
	}

}
