package org.asf.edge.common.events.items;

import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Inventory utility load event - called when
 * {@link org.asf.edge.common.util.InventoryUtil InventoryUtils} is loaded,
 * useful for registering custom redemption handlers
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("items.inventoryutils.load")
public class InventoryUtilsLoadEvent extends EventObject {

	@Override
	public String eventPath() {
		return "items.inventoryutils.load";
	}

}
