package org.asf.edge.gameplayapi.events.items;

import org.asf.edge.gameplayapi.events.GameplayApiServerEvent;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Inventory utility load event - called when
 * {@link org.asf.edge.gameplayapi.util.InventoryUtils InventoryUtils} is
 * loaded, useful for registering custom redemption handlers
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("items.inventoryutils.load")
public class InventoryUtilsLoadEvent extends GameplayApiServerEvent {

	@Override
	public String eventPath() {
		return "items.inventoryutils.load";
	}

}
