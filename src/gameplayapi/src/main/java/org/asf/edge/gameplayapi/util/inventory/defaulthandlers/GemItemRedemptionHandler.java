package org.asf.edge.gameplayapi.util.inventory.defaulthandlers;

import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.gameplayapi.util.inventory.AbstractItemRedemptionHandler;
import org.asf.edge.gameplayapi.util.inventory.ItemRedemptionInfo;
import org.asf.edge.gameplayapi.xmls.inventories.InventoryUpdateResponseData.CurrencyUpdateBlock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GemItemRedemptionHandler extends AbstractItemRedemptionHandler {

	@Override
	public boolean canHandle(ItemInfo item) {
		ObjectNode raw = item.getRawObject();
		if (!raw.has("at"))
			return false;

		// Check def
		JsonNode node = raw.get("at");
		if (node.isArray()) {
			// Go through all nodes
			for (JsonNode n : node) {
				if (n.has("k")) {
					if (n.get("k").asText().equalsIgnoreCase("VCashRedemptionValue")) {
						return true;
					}
				}
			}
		} else if (node.has("k")) {
			// Go through single item
			if (node.get("k").asText().equalsIgnoreCase("VCashRedemptionValue")) {
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
		ObjectNode raw = item.getRawObject();

		// Check def
		JsonNode node = raw.get("at");
		if (node.isArray()) {
			// Go through all nodes
			for (JsonNode n : node) {
				if (n.has("k")) {
					if (n.get("k").asText().equalsIgnoreCase("VCashRedemptionValue")) {
						amount = n.get("v").asInt();
					}
				}
			}
		} else if (node.has("k")) {
			// Go through single item
			if (node.get("k").asText().equalsIgnoreCase("VCashRedemptionValue")) {
				amount = node.get("v").asInt();
			}
		}
		// Add gems
		currencyUpdate.gemCount += amount;
		return RedemptionResult.success();
	}

}
