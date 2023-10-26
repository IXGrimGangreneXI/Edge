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
 * Item creation event - called when items are created
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("items.create")
public class InventoryItemCreateEvent extends EventObject {

	private PlayerInventoryItem item;
	private AccountObject playerAccount;
	private AccountKvDataContainer data;
	private PlayerInventory inventory;
	private PlayerInventoryContainer inventoryContainer;

	public InventoryItemCreateEvent(PlayerInventoryItem item, AccountObject playerAccount, AccountKvDataContainer data,
			PlayerInventory inventory, PlayerInventoryContainer inventoryContainer) {
		this.item = item;
		this.playerAccount = playerAccount;
		this.data = data;
		while (data.getParent() != null)
			data = data.getParent();
		this.inventory = inventory;
		this.inventoryContainer = inventoryContainer;
	}

	@Override
	public String eventPath() {
		return "items.create";
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
