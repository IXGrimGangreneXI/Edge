package org.asf.edge.common.services.items.impl;

import java.io.IOException;
import java.util.Random;

import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.events.items.InventoryItemCreateEvent;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.modules.eventbus.EventBus;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class PlayerInventoryContainerImpl extends PlayerInventoryContainer {

	private static Random rnd = new Random();
	private AccountDataContainer data;
	private PlayerInventory inv;
	private AccountObject account;
	private int id;

	public PlayerInventoryContainerImpl(AccountDataContainer data, PlayerInventory inv, AccountObject account, int id) {
		this.id = id;
		this.account = account;
		this.inv = inv;
		try {
			this.data = data.getChildContainer(Integer.toString(id));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int[] getItemUniqueIDs() {
		try {
			// Find items
			JsonElement e = data.getEntry("itemlist");
			if (e == null) {
				e = new JsonArray();
				data.setEntry("itemlist", e);
			}
			JsonArray lst = e.getAsJsonArray();

			// Create list
			int[] ids = new int[lst.size()];
			int i = 0;
			for (JsonElement ele : lst)
				ids[i++] = ele.getAsInt();
			return ids;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public PlayerInventoryItem getItem(int uniqueID) {
		try {
			// Find item
			JsonElement ele = data.getEntry("item-" + uniqueID);
			if (ele == null)
				return null;

			// Load item object
			JsonObject itm = ele.getAsJsonObject();
			int defID = itm.get("id").getAsInt();
			int quantity = itm.get("quantity").getAsInt();
			int uses = itm.get("uses").getAsInt();
			return new PlayerInventoryItemImpl(data, uniqueID, defID, quantity, uses, account, inv, this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public PlayerInventoryItem createItem(int defID, int quantity, int uses) {
		try {
			// Load item list and add to it
			JsonElement e = data.getEntry("itemlist");
			if (e == null)
				e = new JsonArray();
			JsonArray lst = e.getAsJsonArray();
			if (lst.size() >= Integer.MAX_VALUE - 1)
				throw new IOException("Too many items in inventory");

			// Generate item id
			int uniqueID = rnd.nextInt(0, 1000000000);
			while (data.entryExists("item-" + uniqueID))
				uniqueID = rnd.nextInt(0, 1000000000);

			// Add to liast
			lst.add(uniqueID);
			data.setEntry("itemlist", lst);

			// Write item
			JsonObject itm = new JsonObject();
			itm.addProperty("id", defID);
			itm.addProperty("quantity", quantity);
			itm.addProperty("uses", uses);
			data.setEntry("item-" + uniqueID, itm);

			// Dispatch event
			PlayerInventoryItemImpl i = new PlayerInventoryItemImpl(data, uniqueID, defID, quantity, uses, account, inv,
					this);
			EventBus.getInstance().dispatchEvent(new InventoryItemCreateEvent(i, account, data, inv, this));

			// Return
			return i;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getContainerId() {
		return id;
	}

}
