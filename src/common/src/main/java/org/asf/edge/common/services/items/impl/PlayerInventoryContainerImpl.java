package org.asf.edge.common.services.items.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.events.items.InventoryItemCreateEvent;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.modules.eventbus.EventBus;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

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
			this.data = data.getChildContainer("c-" + Integer.toString(id));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int[] getItemUniqueIDs() {
		try {
			// Create list
			ArrayList<Integer> ids = new ArrayList<Integer>();
			for (String ent : data.getChildContainers()) {
				if (ent.startsWith("d-")) {
					int defID = Integer.parseInt(ent.substring(2));

					// Find items
					for (String ent2 : data.getChildContainer("d-" + defID).getEntryKeys()) {
						if (ent2.startsWith("u-")) {
							// Add item ID
							int uniqueID = Integer.parseInt(ent2.substring(2));
							ids.add(uniqueID);
						}
					}
				}
			}
			int[] i = new int[ids.size()];
			for (int i2 = 0; i2 < i.length; i2++)
				i[i2] = ids.get(i2);
			return i;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public PlayerInventoryItem getItem(int uniqueID) {
		try {
			// Find item
			JsonElement ele = data.getEntry("u-" + uniqueID);
			if (ele == null)
				return null;
			int defID = ele.getAsInt();

			// Find item
			ele = data.getChildContainer("d-" + defID).getEntry("u-" + uniqueID);
			if (ele == null)
				return null;

			// Load item object
			JsonObject itm = ele.getAsJsonObject();
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
			AccountDataContainer item = data.getChildContainer("d-" + defID);
			if (item.getEntryKeys().length >= Integer.MAX_VALUE - 1)
				throw new IOException("Too many items in inventory");

			// Generate item id
			int uniqueID = rnd.nextInt(0, 1000000000);
			while (data.entryExists("u-" + uniqueID))
				uniqueID = rnd.nextInt(0, 1000000000);

			// Write item
			JsonObject itm = new JsonObject();
			itm.addProperty("quantity", quantity);
			itm.addProperty("uses", uses);
			item.setEntry("u-" + uniqueID, itm);

			// Write def ID
			data.setEntry("u-" + uniqueID, new JsonPrimitive(defID));

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

	@Override
	public PlayerInventoryItem[] find(int defID) {
		// Find item
		try {
			ArrayList<PlayerInventoryItem> items = new ArrayList<PlayerInventoryItem>();
			AccountDataContainer item = data.getChildContainer("d-" + defID);
			for (String itm : item.getEntryKeys()) {
				if (itm.startsWith("u-")) {
					// Add item ID
					int uniqueID = Integer.parseInt(itm.substring(2));

					// Find item
					JsonElement ele = data.getChildContainer("d-" + defID).getEntry("u-" + uniqueID);
					if (ele == null)
						continue;

					// Load item object
					JsonObject itmO = ele.getAsJsonObject();
					int quantity = itmO.get("quantity").getAsInt();
					int uses = itmO.get("uses").getAsInt();
					items.add(new PlayerInventoryItemImpl(data, uniqueID, defID, quantity, uses, account, inv, this));
				}
			}
			return items.toArray(t -> new PlayerInventoryItem[t]);
		} catch (IOException e) {
			return new PlayerInventoryItem[0];
		}
	}

	@Override
	public PlayerInventoryItem findFirst(int defID) {
		// Find item
		try {
			AccountDataContainer item = data.getChildContainer("d-" + defID);
			for (String itm : item.getEntryKeys()) {
				if (itm.startsWith("u-")) {
					// Add item ID
					int uniqueID = Integer.parseInt(itm.substring(2));

					// Find item
					JsonElement ele = data.getChildContainer("d-" + defID).getEntry("u-" + uniqueID);
					if (ele == null)
						continue;

					// Load item object
					JsonObject itmO = ele.getAsJsonObject();
					int quantity = itmO.get("quantity").getAsInt();
					int uses = itmO.get("uses").getAsInt();
					return new PlayerInventoryItemImpl(data, uniqueID, defID, quantity, uses, account, inv, this);
				}
			}
			return null;
		} catch (IOException e) {
			return null;
		}
	}

}
