package org.asf.edge.common.services.items.impl;

import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.common.entities.items.*;
import org.asf.edge.common.events.items.ItemManagerLoadEvent;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.commondata.CommonDataContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.xmls.items.ItemStoreDefinitionData;
import org.asf.edge.common.xmls.items.edgespecific.ItemRegistryManifest;
import org.asf.edge.common.xmls.items.edgespecific.ItemRegistryManifest.DefaultItemBlock;
import org.asf.edge.modules.IEdgeModule;
import org.asf.edge.modules.ModuleManager;
import org.asf.edge.modules.eventbus.EventBus;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ItemManagerImpl extends ItemManager {

	private Logger logger;
	private HashMap<Integer, ItemInfo> itemDefs = new HashMap<Integer, ItemInfo>();
	private HashMap<Integer, ItemStoreInfo> storeDefs = new HashMap<Integer, ItemStoreInfo>();

	private long lastReloadTime;

	public DefaultItemBlock[] defaultItems;

	@Override
	public void initService() {
		logger = LogManager.getLogger("ItemManager");

		try {
			// Start reload watchdog
			CommonDataContainer cont = CommonDataManager.getInstance().getContainer("ITEMMANAGER");
			try {
				if (!cont.entryExists("lastreload")) {
					lastReloadTime = System.currentTimeMillis();
					cont.setEntry("lastreload", new JsonPrimitive(lastReloadTime));
				} else
					lastReloadTime = cont.getEntry("lastreload").getAsLong();
			} catch (IOException e) {
			}
			AsyncTaskManager.runAsync(() -> {
				while (true) {
					// Check reload
					try {
						long reload = cont.getEntry("lastreload").getAsLong();
						if (reload > lastReloadTime) {
							// Trigger reload
							lastReloadTime = reload;
							loadData();
						}
					} catch (IOException e) {
					}
					try {
						Thread.sleep(30000);
					} catch (InterruptedException e) {
					}
				}
			});

		} catch (IllegalArgumentException e) {
		}

		// Load stores
		loadData();
	}

	private void loadData() {
		// Prepare
		HashMap<Integer, ItemInfo> itemDefs = new HashMap<Integer, ItemInfo>();
		HashMap<Integer, ItemStoreInfo> storeDefs = new HashMap<Integer, ItemStoreInfo>();
		DefaultItemBlock[] defaultItems;
		logger.info("Loading item store data...");

		try {
			// Load XML
			InputStream strm = getClass().getClassLoader().getResourceAsStream("itemdata/itemstores.xml");
			String data = new String(strm.readAllBytes(), "UTF-8");
			strm.close();
			data = data.replace("http://media.jumpstart.com/", "RS_DATA/");
			data = data.replace("https://media.jumpstart.com/", "RS_DATA/");
			data = data.replace("http://media.schoolofdragons.com/", "RS_DATA/");
			data = data.replace("https://media.schoolofdragons.com/", "RS_DATA/");

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
			data = data.replace("http://media.jumpstart.com/", "RS_DATA/");
			data = data.replace("https://media.jumpstart.com/", "RS_DATA/");
			data = data.replace("http://media.schoolofdragons.com/", "RS_DATA/");
			data = data.replace("https://media.schoolofdragons.com/", "RS_DATA/");

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

		// Load item transformers
		logger.info("Loading item transformers...");
		loadTransformers(getClass(), itemDefs);

		// Load module transformers
		for (IEdgeModule module : ModuleManager.getLoadedModules()) {
			loadTransformers(module.getClass(), itemDefs);
		}

		// Load all transformers from disk
		File transformersItems = new File("itemtransformers");
		if (transformersItems.exists()) {
			for (File transformer : transformersItems.listFiles(t -> t.getName().endsWith(".xml") || t.isDirectory())) {
				loadItemTransformer(transformer, itemDefs);
			}
		}

		// Load item transformers
		logger.info("Loading shop transformers...");
		loadTransformersShops(getClass(), storeDefs, itemDefs);

		// Load module transformers
		for (IEdgeModule module : ModuleManager.getLoadedModules()) {
			loadTransformersShops(module.getClass(), storeDefs, itemDefs);
		}

		// Load all transformers from disk
		File transformersShops = new File("shoptransformers");
		if (transformersShops.exists()) {
			for (File transformer : transformersShops
					.listFiles(t -> t.getName().endsWith(".json") || t.isDirectory())) {
				loadShopTransformer(transformer, storeDefs, itemDefs);
			}
		}

		// Apply
		this.defaultItems = defaultItems;
		this.itemDefs = itemDefs;
		this.storeDefs = storeDefs;

		// Fire event
		logger.info("Dispatching load event...");
		EventBus.getInstance().dispatchEvent(new ItemManagerLoadEvent(this));
	}

	private void loadItemTransformer(File transformer, HashMap<Integer, ItemInfo> itemDefs) {
		if (transformer.isFile()) {
			logger.debug("Loading transformer: '" + transformer.getPath() + "'...");
			try {
				// Find the transformer
				InputStream strm = new FileInputStream(transformer);

				// Load transformer
				XmlMapper mapper = new XmlMapper();
				ObjectNode def = mapper.reader().readValue(new String(strm.readAllBytes(), "UTF-8"), ObjectNode.class);
				strm.close();

				// Define if needed
				if (!itemDefs.containsKey(def.get("id").asInt())) {
					// Register
					ItemInfo itm = new ItemInfo(def.get("id").asInt(), def.get("itn").asText(), def.get("d").asText(),
							def);
					if (!itemDefs.containsKey(itm.getID())) {
						itemDefs.put(itm.getID(), itm);
						logger.debug("Registered item: " + itm.getID() + ": " + itm.getName());
					}
				} else {
					// Update
					ItemInfo itm = itemDefs.get(def.get("id").asInt());
					if (itm == null)
						throw new IllegalArgumentException("Item definition not found: " + def.get("id").asInt());

					// Update it
					ObjectNode obj = itm.getRawObject();
					Iterator<String> names = def.fieldNames();
					while (names.hasNext()) {
						String key = names.next();

						// Update field
						if (obj.has(key))
							obj.remove(key);
						obj.set(key, def.get(key));
					}
					itm.reloadDef();
					logger.debug("Updated item: " + itm.getID() + ": " + itm.getName());
				}
			} catch (Exception e) {
				logger.error("Transformer failed to load: " + transformer.getPath(), e);
			}
		} else {
			logger.debug("Loading transformers from " + transformer.getPath() + "...");
			for (File tr : transformer.listFiles(t -> t.getName().endsWith(".xml") || t.isDirectory())) {
				loadItemTransformer(tr, itemDefs);
			}
		}
	}

	private void loadTransformers(Class<?> cls, HashMap<Integer, ItemInfo> itemDefs) {
		URL source = cls.getProtectionDomain().getCodeSource().getLocation();

		// Generate a base URL
		String baseURL = "";
		String fileName = "";
		try {
			File sourceFile = new File(source.toURI());
			fileName = sourceFile.getName();
			if (sourceFile.isDirectory()) {
				baseURL = source + (source.toString().endsWith("/") ? "" : "/");
			} else {
				baseURL = "jar:" + source + "!/";
			}
		} catch (Exception e) {
			return;
		}

		try {
			// Find the transformer document
			logger.debug("Loading transformers from " + fileName + "...");
			InputStream strm = new URL(baseURL + "itemtransformers/index.json").openStream();
			JsonArray index = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonArray();
			strm.close();

			// Load all transformers
			for (JsonElement ele : index) {
				logger.debug("Loading transformer: 'itemtransformers/" + ele.getAsString() + ".xml'...");
				try {
					// Find the transformer
					strm = new URL(baseURL + "itemtransformers/" + ele.getAsString() + ".xml").openStream();

					// Load transformer
					XmlMapper mapper = new XmlMapper();
					ObjectNode def = mapper.reader().readValue(new String(strm.readAllBytes(), "UTF-8"),
							ObjectNode.class);
					strm.close();

					// Define if needed
					if (!itemDefs.containsKey(def.get("id").asInt())) {
						// Register
						ItemInfo itm = new ItemInfo(def.get("id").asInt(), def.get("itn").asText(),
								def.get("d").asText(), def);
						if (!itemDefs.containsKey(itm.getID())) {
							itemDefs.put(itm.getID(), itm);
							logger.debug("Registered item: " + itm.getID() + ": " + itm.getName());
						}
					} else {
						// Update
						ItemInfo itm = itemDefs.get(def.get("id").asInt());
						if (itm == null)
							throw new IllegalArgumentException("Item definition not found: " + def.get("id").asInt());

						// Update it
						ObjectNode obj = itm.getRawObject();
						Iterator<String> names = def.fieldNames();
						while (names.hasNext()) {
							String key = names.next();

							// Update field
							if (obj.has(key))
								obj.remove(key);
							obj.set(key, def.get(key));
						}
						itm.reloadDef();
						logger.debug("Updated item: " + itm.getID() + ": " + itm.getName());
					}
				} catch (Exception e) {
					logger.error("Transformer failed to load: " + ele.getAsString() + " (" + fileName + ")", e);
				}
			}
		} catch (Exception e) {
			if (e instanceof FileNotFoundException)
				return;
			throw new RuntimeException(e);
		}
	}

	private void loadShopTransformer(File transformer, HashMap<Integer, ItemStoreInfo> storeDefs,
			HashMap<Integer, ItemInfo> itemDefs) {
		if (transformer.isFile()) {
			logger.debug("Loading transformer: '" + transformer.getPath() + "'...");
			try {
				// Find the transformer
				InputStream strm = new FileInputStream(transformer);

				// Load transformer
				JsonObject tr = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject();
				strm.close();

				// Run it
				loadShopTransformer(tr, storeDefs, itemDefs);
			} catch (Exception e) {
				logger.error("Transformer failed to load: " + transformer.getPath(), e);
			}
		} else {
			logger.debug("Loading transformers from " + transformer.getPath() + "...");
			for (File tr : transformer.listFiles(t -> t.getName().endsWith(".json") || t.isDirectory())) {
				loadShopTransformer(tr, storeDefs, itemDefs);
			}
		}
	}

	private void loadTransformersShops(Class<?> cls, HashMap<Integer, ItemStoreInfo> storeDefs,
			HashMap<Integer, ItemInfo> itemDefs) {
		URL source = cls.getProtectionDomain().getCodeSource().getLocation();

		// Generate a base URL
		String baseURL = "";
		String fileName = "";
		try {
			File sourceFile = new File(source.toURI());
			fileName = sourceFile.getName();
			if (sourceFile.isDirectory()) {
				baseURL = source + (source.toString().endsWith("/") ? "" : "/");
			} else {
				baseURL = "jar:" + source + "!/";
			}
		} catch (Exception e) {
			return;
		}

		try {
			// Find the transformer document
			logger.debug("Loading transformers from " + fileName + "...");
			InputStream strm = new URL(baseURL + "shoptransformers/index.json").openStream();
			JsonArray index = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonArray();
			strm.close();

			// Load all transformers
			for (JsonElement ele : index) {
				logger.debug("Loading transformer: 'shoptransformers/" + ele.getAsString() + ".json'...");
				try {
					// Load transformer
					strm = new URL(baseURL + "shoptransformers/" + ele.getAsString() + ".json").openStream();
					JsonObject transformer = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8"))
							.getAsJsonObject();
					strm.close();

					// Run it
					loadShopTransformer(transformer, storeDefs, itemDefs);
				} catch (Exception e) {
					logger.error("Transformer failed to load: " + ele.getAsString() + " (" + fileName + ")", e);
				}
			}
		} catch (Exception e) {
			if (e instanceof FileNotFoundException)
				return;
			throw new RuntimeException(e);
		}
	}

	private void loadShopTransformer(JsonObject transformer, HashMap<Integer, ItemStoreInfo> storeDefs,
			HashMap<Integer, ItemInfo> itemDefs) {
		// Check validity
		if (!transformer.has("mode"))
			throw new IllegalArgumentException("No 'mode' field present in transformer!");
		if (!transformer.get("mode").getAsString().equals("merge")
				&& !transformer.get("mode").getAsString().equals("define")
				&& !transformer.get("mode").getAsString().equals("replace"))
			throw new IllegalArgumentException(
					"Invalid transformation mode, expected either 'merge', 'define' or 'replace'");
		if (!transformer.has("id"))
			throw new IllegalArgumentException("No 'id' field present in transformer!");

		// Find shop
		String mode = transformer.get("mode").getAsString();
		ItemStoreInfo shop = storeDefs.get(transformer.get("id").getAsInt());
		if (shop == null && mode.equals("merge"))
			throw new IllegalArgumentException("Store '" + transformer.get("id").getAsInt() + "' was not found");
		else if (shop != null && mode.equals("define"))
			throw new IllegalArgumentException("Store '" + transformer.get("id").getAsInt() + "' already exists");

		// Load information into memory
		String descr = "";
		String name = "";
		if (shop != null)
			descr = shop.getDescription();
		if (shop != null)
			name = shop.getName();
		if (transformer.has("name"))
			name = transformer.get("name").getAsString();
		if (transformer.has("description"))
			descr = transformer.get("description").getAsString();
		HashMap<String, ItemInfo> items = new HashMap<String, ItemInfo>();
		if (shop != null && !mode.equals("replace")) {
			for (ItemInfo itm : shop.getItems()) {
				items.put(Integer.toString(itm.getID()), itm);
			}
		}

		// Apply transformer
		if (transformer.has("items")) {
			JsonObject obj = transformer.get("items").getAsJsonObject();
			for (String id : obj.keySet()) {
				// Load item
				ItemInfo def;
				if (items.containsKey(id)) {
					def = items.get(id);
				} else {
					// Define
					int defID = Integer.parseInt(id);
					def = itemDefs.get(defID);
					if (def == null)
						throw new IllegalArgumentException("Item definition '" + id + "' does not exists");
				}

				// Check transformer
				JsonObject trans = obj.get(id).getAsJsonObject();
				if (!trans.has("mode"))
					throw new IllegalArgumentException("No 'mode' field present in transformer item! Item ID: " + id);
				if (!trans.get("mode").getAsString().equals("update")
						&& !trans.get("mode").getAsString().equals("define")
						&& !trans.get("mode").getAsString().equals("remove"))
					throw new IllegalArgumentException(
							"Invalid transformation mode in transformer item, expected either 'update', 'define' or 'remove'");
				String md = trans.get("mode").getAsString();

				// Check mode
				if (md.equals("define") && items.containsKey(id))
					throw new IllegalArgumentException(
							"Unable to define item that already exists in shop item list! Item ID: " + id);
				else if (md.equals("remove") && !items.containsKey(id))
					throw new IllegalArgumentException(
							"Unable to remove item that does not exists in shop item list! Item ID: " + id);

				// Check type, if this is not a removal request, check fields
				if (!md.equals("remove")) {
					if (!trans.has("cost"))
						throw new IllegalArgumentException(
								"No 'cost' field present in transformer item! Item ID: " + id);
					if (!trans.has("currency"))
						throw new IllegalArgumentException(
								"No 'currency' field present in transformer item! Item ID: " + id);
					if (!trans.get("currency").getAsString().equals("gems")
							&& !trans.get("currency").getAsString().equals("coins"))
						throw new IllegalArgumentException(
								"Invalid currency type in transformer item, expected either 'gems' or 'coins'");
				}

				// Apply
				ObjectNode newNode = def.getRawObject().deepCopy();
				newNode.set("ct", new IntNode(-1));
				newNode.set("ct2", new IntNode(0));
				if (trans.get("currency").getAsString().equals("coins"))
					newNode.set("ct", new IntNode(trans.get("cost").getAsInt()));
				else
					newNode.set("ct2", new IntNode(trans.get("cost").getAsInt()));

				// Create new def
				ItemInfo newDef = new ItemInfo(def.getID(), def.getDescription(), def.getDescription(), newNode);
				items.put(id, newDef);
			}
		}

		// Save
		storeDefs.put(shop.getID(),
				new ItemStoreInfo(shop.getID(), name, descr, items.values().toArray(t -> new ItemInfo[t])));
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
		return new PlayerInventoryImpl(data, data.getAccount(), this);
	}

	@Override
	public void registerItemDefinition(ItemInfo item) {
		if (!itemDefs.containsKey(item.getID())) {
			itemDefs.put(item.getID(), item);
			logger.debug("Registered item: " + item.getID() + ": " + item.getName());
		}
	}

	@Override
	public void updateItemDefinition(int id, ObjectNode rawData) {
		// Find def
		ItemInfo itm = getItemDefinition(id);
		if (itm == null)
			throw new IllegalArgumentException("Item definition not found: " + id);

		// Update it
		ObjectNode obj = itm.getRawObject();
		Iterator<String> names = rawData.fieldNames();
		while (names.hasNext()) {
			String key = names.next();

			// Update field
			if (obj.has(key))
				obj.remove(key);
			obj.set(key, rawData.get(key));
		}
		itm.reloadDef();
		logger.debug("Updated item: " + itm.getID() + ": " + itm.getName());
	}

	@Override
	public void reload() {
		// Trigger a reload on all servers
		lastReloadTime = System.currentTimeMillis();
		try {
			CommonDataManager.getInstance().getContainer("ITEMMANAGER").setEntry("lastreload",
					new JsonPrimitive(lastReloadTime));
		} catch (IOException e) {
		}
		logger.info("Reloading item manager...");
		loadData();
	}

}
