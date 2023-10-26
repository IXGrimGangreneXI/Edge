package org.asf.edge.common.util.inventory;

import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.xmls.inventories.SetCommonInventoryRequestData;

/**
 * 
 * Security validation handler for common inventory requests
 * 
 * @author Sky Swimmer
 *
 */
public abstract class AbstractInventorySecurityValidator {

	/**
	 * Verifies common inventory assignment requests
	 * 
	 * @param request            Request to verify
	 * @param data               Account data object
	 * @param inventory          Player inventory
	 * @param inventoryContainer Inventory container
	 * @param item               Inventory item instance (may be null if not present
	 *                           in inventory, if a item of the requested ID is
	 *                           already present this will hold the item instance)
	 * @return True if valid, false if invalid
	 */
	public abstract boolean isValidRequest(SetCommonInventoryRequestData request, AccountKvDataContainer data,
			PlayerInventory inventory, PlayerInventoryContainer inventoryContainer, PlayerInventoryItem item);

}
