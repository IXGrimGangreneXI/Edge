package org.asf.edge.common.services.items.impl;

import java.io.IOException;

import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.xmls.items.edgespecific.ItemRegistryManifest.DefaultItemBlock;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

public class PlayerInventoryImpl extends PlayerInventory {

	private AccountDataContainer data;
	private ItemManagerImpl manager;

	private AccountObject account;

	public PlayerInventoryImpl(AccountDataContainer data, AccountObject account, ItemManagerImpl manager) {
		this.manager = manager;
		this.account = account;
		try {
			this.data = data.getChildContainer("commoninventories");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void giveDefaultItems() {
		// Add default items
		for (DefaultItemBlock itm : manager.defaultItems) {
			// Add item
			getContainer(itm.inventoryID).createItem(itm.itemID, itm.quantity, itm.uses);
		}
	}

	@Override
	public int[] getContainerIDs() {
		try {
			// Find containers
			JsonArray containerList = new JsonArray();
			if (data.entryExists("containerlist"))
				containerList = data.getEntry("containerlist").getAsJsonArray();
			else
				data.setEntry("containerlist", containerList);

			// Create list
			int[] ids = new int[containerList.size()];
			int i = 0;
			for (JsonElement ele : containerList) {
				ids[i++] = ele.getAsInt();
			}
			return ids;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public PlayerInventoryContainer getContainer(int id) {
		try {
			// Find containers
			JsonArray containerList = new JsonArray();
			if (data.entryExists("containerlist"))
				containerList = data.getEntry("containerlist").getAsJsonArray();

			// Check if present
			for (JsonElement ele : containerList) {
				if (ele.getAsInt() == id)
					return new PlayerInventoryContainerImpl(data, this, account, id);
			}

			// Add
			containerList.add(id);
			data.setEntry("containerList", containerList);
			return new PlayerInventoryContainerImpl(data, this, account, id);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
