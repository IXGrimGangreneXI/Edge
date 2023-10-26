package org.asf.edge.common.util.inventory.defaultvalidators;

import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.util.inventory.AbstractInventorySecurityValidator;
import org.asf.edge.common.xmls.items.relation.ItemRelationData;
import org.asf.edge.common.xmls.inventories.SetCommonInventoryRequestData;

public class BundleSecurityValidator extends AbstractInventorySecurityValidator {

	@Override
	public boolean isValidRequest(SetCommonInventoryRequestData request, AccountKvDataContainer data,
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
		// Go through relations
		for (ItemRelationData n : def.getRawObject().relations) {
			if (isBundleItem(n)) {
				// Its a bundle
				return true;
			}
		}

		// Not a bundle
		return false;
	}

	private static boolean isBundleItem(ItemRelationData rel) {
		// Check type
		return rel.type.equalsIgnoreCase("Bundle");
	}

}
