package org.asf.edge.common.services.items;

import org.asf.edge.common.account.AccountDataContainer;
import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.entities.items.ItemStoreInfo;
import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.services.AbstractService;
import org.asf.edge.common.services.ServiceManager;

/**
 * 
 * Item Manager
 * 
 * @author Sky Swimmer
 *
 */
public abstract class ItemManager extends AbstractService {

	/**
	 * Retrieves the active item manager
	 * 
	 * @return ItemManager instance
	 */
	public static ItemManager getInstance() {
		return ServiceManager.getService(ItemManager.class);
	}

	/**
	 * Retrieves all store IDs
	 * 
	 * @return Array of item store IDs
	 */
	public abstract int[] getStoreIds();

	/**
	 * Retrieves all item stores
	 * 
	 * @return Array of ItemStoreInfo instances
	 */
	public abstract ItemStoreInfo[] getAllStores();

	/**
	 * Retrieves item stores by ID
	 * 
	 * @param id Item store ID
	 * @return ItemStoreInfo instance or null
	 */
	public abstract ItemStoreInfo getStore(int id);

	/**
	 * Retrieves all item IDs
	 * 
	 * @return Array of item IDs
	 */
	public abstract int[] getItemDefinitionIds();

	/**
	 * Retrieves all item definitions
	 * 
	 * @return Array of ItemInfo instances
	 */
	public abstract ItemInfo[] getAllItemDefinitions();

	/**
	 * Retrieves items by ID
	 * 
	 * @param id Item ID
	 * @return ItemInfo instance or null
	 */
	public abstract ItemInfo getItemDefinition(int id);

	/**
	 * Retrieves the common inventory of a player data container
	 * 
	 * @param data Container to retrieve the inventory for
	 */
	public abstract PlayerInventory getCommonInventory(AccountDataContainer data);

}
