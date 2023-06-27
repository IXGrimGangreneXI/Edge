package org.asf.edge.common.services.items.impl;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.services.accounts.AccountDataContainer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class PlayerInventoryItemImpl extends PlayerInventoryItem {

	private AccountDataContainer data;

	private int defID;
	private int uniqueID;
	private int quantity;
	private int uses;

	public PlayerInventoryItemImpl(AccountDataContainer data, int uniqueID, int defID, int quantity, int uses) {
		this.data = data;
		this.defID = defID;
		this.uniqueID = uniqueID;
		this.quantity = quantity;
		this.uses = uses;
	}

	private void updateInfo() {
		try {
			// Find item
			JsonElement ele = data.getEntry("item-" + uniqueID);
			if (ele == null)
				return;

			// Load item object
			JsonObject itm = ele.getAsJsonObject();
			defID = itm.get("id").getAsInt();
			quantity = itm.get("quantity").getAsInt();
			uses = itm.get("uses").getAsInt();
		} catch (IOException e) {
			// Log error
			LogManager.getLogger("ItemManager").error("Failed to refresh data of inventory item " + uniqueID, e);
		}
	}

	private void writeUpdate() {
		try {
			JsonObject itm = new JsonObject();
			itm.addProperty("id", defID);
			itm.addProperty("quantity", quantity);
			itm.addProperty("uses", uses);
			data.setEntry("item-" + uniqueID, itm);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getUniqueID() {
		updateInfo();
		return uniqueID;
	}

	@Override
	public int getItemDefID() {
		updateInfo();
		return defID;
	}

	@Override
	public int getUses() {
		updateInfo();
		return uses;
	}

	@Override
	public int getQuantity() {
		updateInfo();
		return quantity;
	}

	@Override
	public void setQuantity(int quantity) {
		// Update
		this.quantity = quantity;
		if (this.quantity <= 0)
			delete();
		else
			writeUpdate();
	}

	@Override
	public void setUses(int uses) {
		// Update
		this.uses = uses;
		writeUpdate();
	}

	@Override
	public void delete() {
		try {
			// Remove item
			data.deleteEntry("item-" + uniqueID);

			// Update item list
			JsonElement e = data.getEntry("itemlist");
			if (e != null) {
				JsonArray lst = e.getAsJsonArray();
				for (JsonElement ele : lst) {
					if (ele.getAsInt() == uniqueID) {
						// Remove
						lst.remove(ele);

						// Save
						data.setEntry("itemlist", lst);
						break;
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
