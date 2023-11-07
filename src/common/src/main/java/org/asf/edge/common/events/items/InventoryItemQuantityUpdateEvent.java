package org.asf.edge.common.events.items;

import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Item update event - called when items are updated
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("items.update.quantity")
public class InventoryItemQuantityUpdateEvent extends EventObject {

	private PlayerInventoryItem item;
	private AccountObject playerAccount;
	private AccountKvDataContainer data;
	private PlayerInventory inventory;
	private PlayerInventoryContainer inventoryContainer;

	private int oldQuantity;
	private int newQuantity;

	public InventoryItemQuantityUpdateEvent(PlayerInventoryItem item, AccountObject playerAccount,
			AccountKvDataContainer data, PlayerInventory inventory, PlayerInventoryContainer inventoryContainer,
			int oldQuantity, int newQuantity) {
		this.item = item;
		this.playerAccount = playerAccount;
		this.data = data;
		while (data.getParent() != null)
			data = data.getParent();
		this.inventory = inventory;
		this.inventoryContainer = inventoryContainer;
		this.oldQuantity = oldQuantity;
		this.newQuantity = newQuantity;
	}

	@Override
	public String eventPath() {
		return "items.update.quantity";
	}

	/**
	 * Retrieves the old quantity value
	 * 
	 * @return Old item quantity
	 */
	public int getOldQuantity() {
		return oldQuantity;
	}

	/**
	 * Retrieves the new quantity value
	 * 
	 * @return Current item quantity
	 */
	public int getNewQuantity() {
		return newQuantity;
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
