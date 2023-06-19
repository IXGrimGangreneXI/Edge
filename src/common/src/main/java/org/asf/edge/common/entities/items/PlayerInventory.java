package org.asf.edge.common.entities.items;

/**
 * 
 * Player Inventory Type
 * 
 * @author Sky Swimmer
 *
 */
public abstract class PlayerInventory {

	/**
	 * Adds the default items to the player inventory
	 */
	public abstract void giveDefaultItems();

	/**
	 * Retrieves all known container IDs
	 * 
	 * @return Array of container IDs
	 */
	public abstract int[] getContainerIDs();

	/**
	 * Retrieves containers
	 * 
	 * @param id Container ID
	 * @return PlayerInventoryContainer instance
	 */
	public abstract PlayerInventoryContainer getContainer(int id);

	/**
	 * Retrieves all containers
	 * 
	 * @return Array of PlayerInventoryContainer instances
	 */
	public PlayerInventoryContainer[] getContainers() {
		int[] ids = getContainerIDs();
		PlayerInventoryContainer[] invs = new PlayerInventoryContainer[ids.length];
		for (int i = 0; i < invs.length; i++)
			invs[i] = getContainer(ids[i]);
		return invs;
	}

}
