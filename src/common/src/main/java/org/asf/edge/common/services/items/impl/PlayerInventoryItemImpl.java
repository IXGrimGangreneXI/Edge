package org.asf.edge.common.services.items.impl;

import java.io.IOException;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.asf.edge.common.entities.achivements.RankMultiplierInfo;
import org.asf.edge.common.entities.achivements.RankTypeID;
import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.events.items.InventoryItemDeleteEvent;
import org.asf.edge.common.events.items.InventoryItemQuantityUpdateEvent;
import org.asf.edge.common.events.items.InventoryItemUsesUpdateEvent;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.achievements.AchievementManager;
import org.asf.edge.modules.eventbus.EventBus;

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
			JsonElement ele = data.getChildContainer("d-" + defID).getEntry("u-" + uniqueID);
			if (ele == null)
				return;

			// Load item object
			JsonObject itm = ele.getAsJsonObject();
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
			itm.addProperty("quantity", quantity);
			itm.addProperty("uses", uses);
			data.getChildContainer("d-" + defID).setEntry("u-" + uniqueID, itm);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void runItem() {
		// Run multipliers
		if (getItemDef().hasAttribute("RewardMultiplier")
				&& getItemDef().getAttribute("RewardMultiplier").getValue().equalsIgnoreCase("true")
				&& data.getSave() != null) {
			// Load
			int factor = Integer.parseInt(getItemDef().getAttribute("MultiplierFactor").getValue());
			int rewardType = Integer.parseInt(getItemDef().getAttribute("MultiplierRewardType").getValue());
			int effectTime = Integer.parseInt(getItemDef().getAttribute("MultiplierEffectTime").getValue());

			// Apply
			AchievementManager.getInstance().addUserRankMultiplier(data.getSave(), new RankMultiplierInfo(
					RankTypeID.getByTypeID(rewardType), factor, System.currentTimeMillis() + (effectTime * 1000)));
		} else {
			// Warn
			LogManager.getLogger("ItemManager").warn(
					"Item " + getItemDefID() + " was used but no known methods of item data execution were possible!");
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
	public boolean useItem(int uses) {
		// Check
		int usesLeft = getUses();
		if (usesLeft == -1) {
			usesLeft = 1;
		}
		if (uses > usesLeft)
			return false; // Invalid

		// Use the item
		for (int i = 0; i < uses; i++) {
			// Use the item
			runItem();

			// Remove item if no uses are left
			usesLeft--;
			setUses(usesLeft);
			if (usesLeft <= 0) {
				remove(1);
				if (quantity <= 0)
					break;
			}
		}

		// Return
		return true;
	}

	@Override
	public void delete() {
		try {
			// Remove item
			AccountDataContainer cont = data.getChildContainer("d-" + defID);
			cont.deleteEntry("u-" + uniqueID);
			if (Stream.of(cont.getEntryKeys()).filter(t -> t.startsWith("u-")).count() <= 0)
				cont.deleteContainer();

			// Remove pointer
			data.deleteEntry("u-" + uniqueID);

			// Dispatch event
			EventBus.getInstance().dispatchEvent(new InventoryItemDeleteEvent(this, account, data, inv, this.cont));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
