package org.asf.edge.gameplayapi.util.inventory.defaulthandlers;

import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.xmls.items.ItemDefData;
import org.asf.edge.common.xmls.items.attributes.ItemAttributeData;
import org.asf.edge.gameplayapi.util.inventory.AbstractItemRedemptionHandler;
import org.asf.edge.gameplayapi.util.inventory.ItemRedemptionInfo;
import org.asf.edge.gameplayapi.xmls.inventories.InventoryUpdateResponseData.CurrencyUpdateBlock;

public class CoinItemRedemptionHandler extends AbstractItemRedemptionHandler {

	@Override
	public boolean canHandle(ItemInfo item) {
		ItemDefData raw = item.getRawObject();

		// Go through attributes
		for (ItemAttributeData n : raw.attributes) {
			if (n.key.equalsIgnoreCase("CoinRedemptionValue")) {
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
			if (n.key.equalsIgnoreCase("CoinRedemptionValue")) {
				amount = Integer.parseInt(n.value);
			}
		}

		// Add coins
		currencyUpdate.coinCount += amount * req.quantity;
		return RedemptionResult.success();
	}
}
