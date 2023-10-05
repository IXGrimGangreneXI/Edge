package org.asf.edge.gameplayapi.util.inventory.defaultvalidators;

import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.xmls.items.relation.ItemRelationData;
import org.asf.edge.gameplayapi.util.inventory.AbstractInventorySecurityValidator;
import org.asf.edge.gameplayapi.xmls.inventories.SetCommonInventoryRequestData;

public class MysteryBoxSecurityValidator extends AbstractInventorySecurityValidator {

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
		if (!isMysteryBox(def))
			return true;
		return false;
	}

	private static boolean isMysteryBox(ItemInfo def) {
		// Go through relations
		for (ItemRelationData n : def.getRawObject().relations) {
			if (isMysteryItem(n)) {
				// Its a mystery box
				return true;
			}
		}

		// Not a mystery box
		return false;
	}

	private static boolean isMysteryItem(ItemRelationData rel) {
		// Check type
		return rel.type.equalsIgnoreCase("Prize");
	}

}
