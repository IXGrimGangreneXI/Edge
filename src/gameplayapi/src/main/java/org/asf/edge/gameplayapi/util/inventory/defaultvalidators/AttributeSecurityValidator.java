package org.asf.edge.gameplayapi.util.inventory.defaultvalidators;

import java.util.ArrayList;
import java.util.stream.Stream;

import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.gameplayapi.util.inventory.AbstractInventorySecurityValidator;
import org.asf.edge.gameplayapi.xmls.inventories.SetCommonInventoryRequestData;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
		Attribute[] attrs = getAttributes(def);
		for (Attribute attr : attrs) {
			if (Stream.of(deniedAttributes).anyMatch(t -> t.equalsIgnoreCase(attr.key)))
				return false;
		}
		return true;
	}

	public static class Attribute {
		public String key;
		public JsonNode value;
	}

	private Attribute[] getAttributes(ItemInfo def) {
		ObjectNode raw = def.getRawObject();
		if (!raw.has("at"))
			return new Attribute[0];

		// Go through attributes
		ArrayList<Attribute> attrs = new ArrayList<Attribute>();
		JsonNode node = raw.get("at");
		if (node.isArray()) {
			// Go through all nodes
			for (JsonNode n : node) {
				if (n.has("k") && n.has("v")) {
					Attribute attr = new Attribute();
					attr.key = n.get("k").asText();
					attr.value = n.get("v");
					attrs.add(attr);
				}
			}
		} else if (node.has("k") && node.has("v")) {
			// Go through single item
			Attribute attr = new Attribute();
			attr.key = node.get("k").asText();
			attr.value = node.get("v");
			attrs.add(attr);
		}
		return attrs.toArray(t -> new Attribute[t]);
	}

}
