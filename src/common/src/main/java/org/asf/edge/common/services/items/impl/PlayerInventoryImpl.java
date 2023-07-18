package org.asf.edge.common.services.items.impl;

import java.io.IOException;
import java.util.ArrayList;

import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.xmls.items.edgespecific.ItemRegistryManifest.DefaultItemBlock;

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
			ArrayList<Integer> containers = new ArrayList<Integer>();
			for (String key : data.getChildContainers())
				if (key.startsWith("c-")) {
					int id = Integer.parseInt(key.substring("c-".length()));
					containers.add(id);
				}
			int[] ids = new int[containers.size()];
			for (int i = 0; i < containers.size(); i++)
				ids[i] = containers.get(i);
			return ids;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public PlayerInventoryContainer getContainer(int id) {
		return new PlayerInventoryContainerImpl(data, this, account, id);
	}

}
