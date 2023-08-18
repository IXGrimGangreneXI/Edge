package org.asf.edge.gameplayapi.util.inventory.defaultvalidators;

import java.util.stream.Stream;

import org.asf.edge.common.entities.items.ItemAttributeInfo;
import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.gameplayapi.util.inventory.AbstractInventorySecurityValidator;
import org.asf.edge.gameplayapi.xmls.inventories.SetCommonInventoryRequestData;

public class AttributeSecurityValidator extends AbstractInventorySecurityValidator {

	private static final String[] deniedAttributes = new String[] { "VCashRedemptionValue", "CoinRedemptionValue" };

	@Override
	public boolean isValidRequest(SetCommonInventoryRequestData request, AccountDataContainer data,
			PlayerInventory inventory, PlayerInventoryContainer inventoryContainer, PlayerInventoryItem item) {
		int defID = request.itemID;
		ItemInfo def = ItemManager.getInstance().getItemDefinition(defID);
		if (def == null)
			return false;

		// Verify attributes
		for (ItemAttributeInfo attr : def.getAttributes()) {
			if (Stream.of(deniedAttributes).anyMatch(t -> t.equalsIgnoreCase(attr.getKey())))
				return false;
		}
		return true;
	}
}
