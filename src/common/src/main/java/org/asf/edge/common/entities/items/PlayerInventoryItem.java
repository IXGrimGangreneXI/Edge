package org.asf.edge.common.entities.items;

import org.asf.edge.common.services.items.ItemManager;

/**
 * 
 * Inventory Item Container
 * 
 * @author Sky Swimmer
 *
 */
public abstract class PlayerInventoryItem {

	/**
	 * Retrieves the item unique ID
	 * 
	 * @return Item unique ID
	 */
	public abstract int getUniqueID();

	/**
	 * Retrieves the item definition ID
	 * 
	 * @return Item definition ID
	 */
	public abstract int getItemDefID();

	/**
	 * Retrieves the item uses
	 * 
	 * @return Item uses counter
	 */
	public abstract int getUses();

	/**
	 * Retrieves item quantity
	 * 
	 * @return Item quantity
	 */
	public abstract int getQuantity();

	/**
	 * Deletes the item
	 */
	public abstract void delete();

	/**
	 * Reassigns the quantity
	 * 
	 * @param quantity New quantity
	 */
	public abstract void setQuantity(int quantity);

	/**
	 * Reassigns the uses counter
	 * 
	 * @param uses New uses count
	 */
	public abstract void setUses(int uses);

	/**
	 * Adds to the item quantity
	 * 
	 * @param amount Amount to add
	 */
	public void add(int amount) {
		setQuantity(amount + getQuantity());
	}

	/**
	 * Removes from the item quantity
	 * 
	 * @param amount Amount to remove
	 */
	public void remove(int amount) {
		int newQuant = getQuantity() - amount;
		if (newQuant <= 0)
			delete();
		else
			setQuantity(amount);
	}

	/**
	 * Retrieves the item definition
	 * 
	 * @return ItemInfo instance
	 */
	public ItemInfo getItemDef() {
		return ItemManager.getInstance().getItemDefinition(getItemDefID());
	}

}
