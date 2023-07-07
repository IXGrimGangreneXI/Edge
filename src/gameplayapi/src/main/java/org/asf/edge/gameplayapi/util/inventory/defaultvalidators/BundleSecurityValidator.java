package org.asf.edge.gameplayapi.util.inventory.defaultvalidators;

import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.gameplayapi.util.inventory.AbstractInventorySecurityValidator;
import org.asf.edge.gameplayapi.xmls.inventories.SetCommonInventoryRequestData;

import com.fasterxml.jackson.databind.JsonNode;

public class BundleSecurityValidator extends AbstractInventorySecurityValidator {

	@Override
	public boolean isValidRequest(SetCommonInventoryRequestData request, AccountDataContainer data,
			PlayerInventory inventory, PlayerInventoryContainer inventoryContainer, PlayerInventoryItem item) {
		int defID = request.itemID;
		ItemInfo def = ItemManager.getInstance().getItemDefinition(defID);
		if (def == null)
			return false;

		// Check quantity
		if (request.quantity < 0) {
			// Allow removal
			return true;
		}

		// Check box
		if (!isBundle(def))
			return true;
		return false;
	}

	private static boolean isBundle(ItemInfo def) {
		// Check item def
		if (!def.getRawObject().has("r"))
			return false;
		JsonNode node = def.getRawObject().get("r");
		if (node.isArray()) {
			// Go through all nodes
			for (JsonNode n : node) {
				if (isBundleItem(n)) {
					// Its a bundle
					return true;
				}
			}

			// Not a bundle
			return false;
		}

		// Go through single node
		return isBundleItem(node);
	}

	private static boolean isBundleItem(JsonNode node) {
		// Check type
		if (node.has("t")) {
			return node.get("t").asText().equalsIgnoreCase("Bundle");
		}
		return false;
	}

}
