package org.asf.edge.common.util.inventory.defaulthandlers;

import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.util.inventory.AbstractItemRedemptionHandler;
import org.asf.edge.common.util.inventory.ItemRedemptionInfo;
import org.asf.edge.common.xmls.inventories.InventoryUpdateResponseData.CurrencyUpdateBlock;
import org.asf.edge.common.xmls.inventories.InventoryUpdateResponseData.ItemUpdateBlock;

public class DefaultItemRedemptionHandler extends AbstractItemRedemptionHandler {

	@Override
	public boolean canHandle(ItemInfo item) {
		return true;
	}

	@Override
	public RedemptionResult handleRedemption(ItemInfo item, ItemRedemptionInfo req, AccountObject account,
			AccountSaveContainer save, CurrencyUpdateBlock currencyUpdate) {
		PlayerInventoryContainer cont = save.getInventory().getContainer(req.containerID);

		// Find inventory
		PlayerInventoryItem itm = cont.findFirst(req.defID);

		// Check
		if (itm == null)
			itm = cont.createItem(req.defID, 0);

		// Update
		int newQuant = itm.getQuantity() + req.quantity;
		itm.setQuantity(newQuant);

		// Add update
		ItemUpdateBlock update = null;
		if (newQuant > 0 || req.quantity == 0) {
			ItemUpdateBlock b = new ItemUpdateBlock();
			b.itemID = itm.getItemDefID();
			b.itemUniqueID = itm.getUniqueID();
			b.quantity = req.quantity;
			update = b;
		}

		// Return
		return RedemptionResult.success(update);
	}

}
