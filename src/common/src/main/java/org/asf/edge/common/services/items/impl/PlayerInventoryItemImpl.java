package org.asf.edge.common.services.items.impl;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.events.items.InventoryItemDeleteEvent;
import org.asf.edge.common.events.items.InventoryItemQuantityUpdateEvent;
import org.asf.edge.common.events.items.InventoryItemUsesUpdateEvent;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.modules.eventbus.EventBus;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class PlayerInventoryItemImpl extends PlayerInventoryItem {

	private AccountDataContainer data;
	private AccountObject account;
	private PlayerInventory inv;
	private PlayerInventoryContainer cont;

	private long lastUpdate = System.currentTimeMillis();

	private int defID;
	private int uniqueID;
	private int quantity;
	private int uses;

	public PlayerInventoryItemImpl(AccountDataContainer data, int uniqueID, int defID, int quantity, int uses,
			AccountObject account, PlayerInventory inv, PlayerInventoryContainer cont) {
		this.data = data;
		this.defID = defID;
		this.uniqueID = uniqueID;
		this.quantity = quantity;
		this.uses = uses;
		this.account = account;
		this.inv = inv;
		this.cont = cont;
	}

	private void updateInfo() {
		try {
			// Check
			if (System.currentTimeMillis() < lastUpdate + 10000)
				return; // Lets leave it to caching, to prevent overloading the database
			lastUpdate = System.currentTimeMillis();

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
			// Save
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
		int oldQuant = this.quantity;
		this.quantity = quantity;
		if (this.quantity <= 0)
			delete();
		else {
			// Dispatch event
			EventBus.getInstance().dispatchEvent(
					new InventoryItemQuantityUpdateEvent(this, account, data, inv, cont, oldQuant, quantity));

			// Write update
			writeUpdate();
		}
	}

	@Override
	public void setUses(int uses) {
		int oldUses = this.uses;

		// Update
		this.uses = uses;

		// Dispatch event
		EventBus.getInstance()
				.dispatchEvent(new InventoryItemUsesUpdateEvent(this, account, data, inv, cont, oldUses, uses));

		// Write update
		writeUpdate();
	}

	@Override
	public void delete() {
		try {
			// Remove item
			data.deleteEntry("item-" + uniqueID);

			// Dispatch event
			EventBus.getInstance().dispatchEvent(new InventoryItemDeleteEvent(this, account, data, inv, cont));

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
