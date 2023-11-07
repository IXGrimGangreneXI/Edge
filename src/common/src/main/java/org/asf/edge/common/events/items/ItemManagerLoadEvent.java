package org.asf.edge.common.events.items;

import org.asf.edge.common.services.items.ItemManager;
import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Item manager load event - called after the item manager has loaded or
 * reloaded item and store definitions
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("items.itemmanager.load")
public class ItemManagerLoadEvent extends EventObject {

	private ItemManager itemManager;

	@Override
	public String eventPath() {
		return "items.itemmanager.load";
	}

	public ItemManagerLoadEvent(ItemManager itemManager) {
		this.itemManager = itemManager;
	}

	/**
	 * Retrieves the item manager
	 * 
	 * @return ItemManager instance
	 */
	public ItemManager getItemManager() {
		return itemManager;
	}

}
