package org.asf.edge.common.util.inventory.defaulthandlers;

import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.util.inventory.AbstractItemRedemptionHandler;
import org.asf.edge.common.util.inventory.ItemRedemptionInfo;
import org.asf.edge.common.xmls.items.ItemDefData;
import org.asf.edge.common.xmls.items.attributes.ItemAttributeData;
import org.asf.edge.common.xmls.inventories.InventoryUpdateResponseData.CurrencyUpdateBlock;

public class GemItemRedemptionHandler extends AbstractItemRedemptionHandler {

	@Override
	public boolean canHandle(ItemInfo item) {
		ItemDefData raw = item.getRawObject();

		// Go through attributes
		for (ItemAttributeData n : raw.attributes) {
			if (n.key.equalsIgnoreCase("VCashRedemptionValue")) {
				return true;
			}
		}

		return false;
	}

	@Override
	public RedemptionResult handleRedemption(ItemInfo item, ItemRedemptionInfo req, AccountObject account,
			AccountSaveContainer save, CurrencyUpdateBlock currencyUpdate) {
		// Find amount
		int amount = 0;
		ItemDefData raw = item.getRawObject();

		// Go through attributes
		for (ItemAttributeData n : raw.attributes) {
			if (n.key.equalsIgnoreCase("VCashRedemptionValue")) {
				amount = Integer.parseInt(n.value);
			}
		}

		// Add gems
		currencyUpdate.gemCount += amount * req.quantity;
		return RedemptionResult.success();
	}

}
