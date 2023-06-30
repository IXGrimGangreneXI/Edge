package org.asf.edge.common.services.items.impl;

import org.asf.edge.common.entities.items.*;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.xmls.items.ItemStoreDefinitionData;
import org.asf.edge.common.xmls.items.edgespecific.ItemRegistryManifest;
import org.asf.edge.common.xmls.items.edgespecific.ItemRegistryManifest.DefaultItemBlock;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ItemManagerImpl extends ItemManager {

	private Logger logger;
	private HashMap<Integer, ItemInfo> itemDefs = new HashMap<Integer, ItemInfo>();
	private HashMap<Integer, ItemStoreInfo> storeDefs = new HashMap<Integer, ItemStoreInfo>();

	public DefaultItemBlock[] defaultItems;

	@Override
	public void initService() {
		logger = LogManager.getLogger("ItemManager");

		// Load stores
		logger.info("Loading item store data...");
		try {
			// Load XML
			InputStream strm = getClass().getClassLoader().getResourceAsStream("itemdata/itemstores.xml");
			String data = new String(strm.readAllBytes(), "UTF-8");
			strm.close();

			// Load into map
			XmlMapper mapper = new XmlMapper();
			ItemStoreDefinitionData[] stores = mapper.reader().readValue(data, ItemStoreDefinitionData[].class);

			// Load stores
			for (ItemStoreDefinitionData store : stores) {
				// Load store
				logger.debug("Loading store: " + store.storeID + " (" + store.storeName + ")");
				ItemInfo[] items = new ItemInfo[store.items.length];
				for (int i = 0; i < items.length; i++) {
					// Register item
					items[i] = new ItemInfo(store.items[i].get("id").asInt(), store.items[i].get("itn").asText(),
							store.items[i].get("d").asText(), store.items[i]);
					itemDefs.put(items[i].getID(), items[i]);
					logger.debug("Registered item: " + items[i].getID() + ": " + items[i].getName());
				}
				storeDefs.put(store.storeID,
						new ItemStoreInfo(store.storeID, store.storeName, store.storeDescription, items));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// Load item defs
		logger.info("Loading item defs...");
		try {
			// Load XML
			InputStream strm = getClass().getClassLoader().getResourceAsStream("itemdata/itemdefs.xml");
			String data = new String(strm.readAllBytes(), "UTF-8");
			strm.close();

			// Load object
			XmlMapper mapper = new XmlMapper();
			ItemRegistryManifest reg = mapper.reader().readValue(data, ItemRegistryManifest.class);

			// Load default items
			defaultItems = reg.defaultItems.defaultItems;

			// Load items
			for (ObjectNode def : reg.itemDefs) {
				// Register item
				ItemInfo itm = new ItemInfo(def.get("id").asInt(), def.get("itn").asText(), def.get("d").asText(), def);
				itemDefs.put(itm.getID(), itm);
				logger.debug("Registered item: " + itm.getID() + ": " + itm.getName());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int[] getStoreIds() {
		int[] ids = new int[storeDefs.size()];
		Integer[] idI = storeDefs.keySet().toArray(t -> new Integer[t]);
		for (int i = 0; i < ids.length; i++)
			ids[i] = idI[i];
		return ids;
	}

	@Override
	public ItemStoreInfo[] getAllStores() {
		return storeDefs.values().toArray(t -> new ItemStoreInfo[t]);
	}

	@Override
	public ItemStoreInfo getStore(int id) {
		return storeDefs.get(id);
	}

	@Override
	public int[] getItemDefinitionIds() {
		int[] ids = new int[itemDefs.size()];
		Integer[] idI = itemDefs.keySet().toArray(t -> new Integer[t]);
		for (int i = 0; i < ids.length; i++)
			ids[i] = idI[i];
		return ids;
	}

	@Override
	public ItemInfo[] getAllItemDefinitions() {
		return itemDefs.values().toArray(t -> new ItemInfo[t]);
	}

	@Override
	public ItemInfo getItemDefinition(int id) {
		return itemDefs.get(id);
	}

	@Override
	public PlayerInventory getCommonInventory(AccountDataContainer data) {
		return new PlayerInventoryImpl(data, this);
	}

}
