package org.asf.edge.common.services.items;

import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.entities.items.ItemSaleInfo;
import org.asf.edge.common.entities.items.ItemStoreInfo;
import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.nexus.common.services.AbstractService;
import org.asf.nexus.common.services.ServiceManager;

import com.fasterxml.jackson.databind.node.ObjectNode;

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
	 * Registers item definitions
	 * 
	 * @param item Item definition to register
	 */
	public abstract void registerItemDefinition(ItemInfo item);

	/**
	 * Updates item definitions, merges in data from the given objectnode, this does
	 * NOT replace the def, only replaces what rawData specifies
	 * 
	 * @param id      Item ID
	 * @param rawData Data to merge into the item definition
	 */
	public abstract void updateItemDefinition(int id, ObjectNode rawData);

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
	public abstract PlayerInventory getCommonInventory(AccountKvDataContainer data);

	/**
	 * Registers sales
	 * 
	 * @param sale Sale object to register
	 */
	public abstract void registerSale(ItemSaleInfo sale);

	/**
	 * Unregisters sales
	 * 
	 * @param sale Sale object to remove
	 */
	public abstract void unregisterSale(ItemSaleInfo sale);

	/**
	 * Retrieves all known sales
	 * 
	 * @return Array of ItemSaleInfo instances
	 */
	public abstract ItemSaleInfo[] getSales();

	/**
	 * Retrieves all active sales
	 * 
	 * @return Array of ItemSaleInfo instances
	 */
	public abstract ItemSaleInfo[] getActiveSales();

	/**
	 * Retrieves all upcoming sales
	 * 
	 * @return Array of ItemSaleInfo instances
	 */
	public abstract ItemSaleInfo[] getUpcomingSales();

	/**
	 * Called to reload items and stores from disk
	 */
	public abstract void reload();

}
