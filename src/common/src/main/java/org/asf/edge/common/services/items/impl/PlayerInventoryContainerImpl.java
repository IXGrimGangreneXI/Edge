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
			data.runForChildContainers(ent -> {
				if (ent.startsWith("d-")) {
					int defID = Integer.parseInt(ent.substring(2));

					// Find items
					try {
						AccountDataContainer cont = data.getChildContainer("d-" + defID);
						cont.runForEntries((ent2, value) -> {
							if (ent2.startsWith("u-")) {
								// Parse item ID
								int uniqueID = Integer.parseInt(ent2.substring(2));

								// Check
								try {
									if (!data.entryExists("u-" + uniqueID)) {
										// Add so it doesnt break down
										data.setEntry("u-" + uniqueID, new JsonPrimitive(defID));
									}
									if (cont.getEntry("u-" + uniqueID) == null) {
										// Item damaged
										JsonObject newI = new JsonObject();
										newI.addProperty("quantity", 1);
										newI.addProperty("uses", -1);
										cont.setEntry("u-" + uniqueID, newI);
									}
								} catch (IOException e) {
									throw new RuntimeException(e);
								}

								// Add
								ids.add(uniqueID);
							}
							return true;
						});
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
				return true;
			});
			int[] i = new int[ids.size()];
			for (int i2 = 0; i2 < i.length; i2++)
				i[i2] = ids.get(i2);
			return i;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public PlayerInventoryItem[] getItems() {
		try {
			// Create list
			ArrayList<PlayerInventoryItem> itms = new ArrayList<PlayerInventoryItem>();
			for (String ent : data.getChildContainers()) {
				if (ent.startsWith("d-")) {
					int defID = Integer.parseInt(ent.substring(2));

					// Find items
					AccountDataContainer cont = data.getChildContainer("d-" + defID);
					cont.runForEntries((ent2, value) -> {
						try {
							if (ent2.startsWith("u-")) {
								// Parse item ID
								int uniqueID = Integer.parseInt(ent2.substring(2));

								// Check
								if (!data.entryExists("u-" + uniqueID)) {
									// Add so it doesnt break down
									data.setEntry("u-" + uniqueID, new JsonPrimitive(defID));
								}
								if (cont.getEntry("u-" + uniqueID) == null) {
									// Item damaged
									JsonObject newI = new JsonObject();
									newI.addProperty("quantity", 1);
									newI.addProperty("uses", -1);
									cont.setEntry("u-" + uniqueID, newI);
								}

								// Load item object
								JsonObject itmO = value.getAsJsonObject();
								if (itmO.get("quantity") == null) {
									// Item damaged
									itmO.addProperty("quantity", 1);
									cont.setEntry("u-" + uniqueID, itmO);
								}
								int quantity = itmO.get("quantity").getAsInt();
								int uses = itmO.get("uses").getAsInt();
								itms.add(new PlayerInventoryItemImpl(data, uniqueID, defID, quantity, uses, account,
										inv, this));
							}
							return true;
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					});
				}
			}
			return itms.toArray(t -> new PlayerInventoryItem[t]);
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
			if (itm.get("quantity") == null) {
				// Item damaged
				itm.addProperty("quantity", 1);
				data.getChildContainer("d-" + defID).setEntry("u-" + uniqueID, itm);
			}
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
			itm.add("attributes", new JsonObject());
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
			item.runForEntries((itm, value) -> {
				if (itm.startsWith("u-")) {
					try {
						// Add item ID
						int uniqueID = Integer.parseInt(itm.substring(2));

						// Load item object
						JsonObject itmO = value.getAsJsonObject();
						if (itmO.get("quantity") == null) {
							// Item damaged
							itmO.addProperty("quantity", 1);
							data.getChildContainer("d-" + defID).setEntry("u-" + uniqueID, itmO);
						}
						int quantity = itmO.get("quantity").getAsInt();
						int uses = itmO.get("uses").getAsInt();
						items.add(
								new PlayerInventoryItemImpl(data, uniqueID, defID, quantity, uses, account, inv, this));
					} catch (IOException e) {
					}
					return true;
				}
				return false;
			});
			return items.toArray(t -> new PlayerInventoryItem[t]);
		} catch (IOException e) {
			return new PlayerInventoryItem[0];
		}
	}

	private class ItmRes {
		public int uniqueID;
	}

	@Override
	public PlayerInventoryItem findFirst(int defID) {
		// Find item
		try {
			ItmRes r = new ItmRes();
			AccountDataContainer item = data.getChildContainer("d-" + defID);
			JsonElement ele = item.findEntry((itm, value) -> {
				if (itm.startsWith("u-")) {
					r.uniqueID = Integer.parseInt(itm.substring(2));
					return true;
				}
				return false;
			});
			if (ele != null) {
				// Load item object
				JsonObject itmO = ele.getAsJsonObject();
				if (itmO.get("quantity") == null) {
					// Item damaged
					itmO.addProperty("quantity", 1);
					data.getChildContainer("d-" + defID).setEntry("u-" + r.uniqueID, itmO);
				}
				int quantity = itmO.get("quantity").getAsInt();
				int uses = itmO.get("uses").getAsInt();
				return new PlayerInventoryItemImpl(data, r.uniqueID, defID, quantity, uses, account, inv, this);
			}
			return null;
		} catch (IOException e) {
			return null;
		}
	}

}
