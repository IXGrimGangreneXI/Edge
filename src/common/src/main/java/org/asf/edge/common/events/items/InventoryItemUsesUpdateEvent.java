package org.asf.edge.common.events.items;

import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Item update event - called when items are updated
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("items.update.uses")
public class InventoryItemUsesUpdateEvent extends EventObject {

	private PlayerInventoryItem item;
	private AccountObject playerAccount;
	private AccountKvDataContainer data;
	private PlayerInventory inventory;
	private PlayerInventoryContainer inventoryContainer;

	private int oldUses;
	private int newUses;

	public InventoryItemUsesUpdateEvent(PlayerInventoryItem item, AccountObject playerAccount,
			AccountKvDataContainer data, PlayerInventory inventory, PlayerInventoryContainer inventoryContainer,
			int oldUses, int newUses) {
		this.item = item;
		this.playerAccount = playerAccount;
		this.data = data;
		while (data.getParent() != null)
			data = data.getParent();
		this.inventory = inventory;
		this.inventoryContainer = inventoryContainer;
		this.oldUses = oldUses;
		this.newUses = newUses;
	}

	@Override
	public String eventPath() {
		return "items.update.uses";
	}

	/**
	 * Retrieves the old item use count
	 * 
	 * @return Old item use count
	 */
	public int getOldUseCount() {
		return oldUses;
	}

	/**
	 * Retrieves the new item use count
	 * 
	 * @return Current item use count
	 */
	public int getNewUseCount() {
		return newUses;
	}

	/**
	 * Retrieves the inventory item
	 * 
	 * @return PlayerInventoryItem instance
	 */
	public PlayerInventoryItem getItem() {
		return item;
	}

	/**
	 * Retrieves the account that was changed
	 * 
	 * @return AccountObject instance
	 */
	public AccountObject getAccount() {
		return playerAccount;
	}

	/**
	 * Retrieves the save data container that was changed
	 * 
	 * @return AccountDataContainer instance
	 */
	public AccountKvDataContainer getSaveData() {
		return data;
	}

	/**
	 * Retrieves the inventory that was changed
	 * 
	 * @return PlayerInventory instance
	 */
	public PlayerInventory getPlayerInventory() {
		return inventory;
	}

	/**
	 * Retrieves the inventory container that was changed
	 * 
	 * @return PlayerInventoryContainer instance
	 */
	public PlayerInventoryContainer getInventoryContainer() {
		return inventoryContainer;
	}

}
