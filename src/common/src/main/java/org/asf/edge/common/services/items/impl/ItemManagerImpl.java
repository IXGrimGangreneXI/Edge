package org.asf.edge.common.services.items.impl;

import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.common.entities.items.*;
import org.asf.edge.common.entities.tables.items.ItemSaleRow;
import org.asf.edge.common.entities.tables.items.PopularItemRow;
import org.asf.edge.common.events.items.ItemManagerLoadEvent;
import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.services.commondata.CommonKvDataContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.services.commondata.CommonDataTableContainer;
import org.asf.edge.common.services.config.ConfigProviderService;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.util.RandomSelectorUtil;
import org.asf.edge.common.xmls.items.ItemDefData;
import org.asf.edge.common.xmls.items.ItemStoreDefinitionData;
import org.asf.edge.common.xmls.items.edgespecific.ItemRegistryManifest;
import org.asf.edge.common.xmls.items.edgespecific.ItemRegistryManifest.DefaultItemBlock;
import org.asf.edge.modules.IEdgeModule;
import org.asf.edge.modules.ModuleManager;
import org.asf.nexus.events.EventBus;
import org.asf.nexus.tables.DataFilter;
import org.asf.nexus.tables.DataTable;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ItemManagerImpl extends ItemManager {

	private Logger logger;
	private HashMap<Integer, ItemInfo> itemDefs = new HashMap<Integer, ItemInfo>();
	private HashMap<Integer, ItemStoreInfo> storeDefs = new HashMap<Integer, ItemStoreInfo>();

	private ArrayList<ItemSaleInfo> currentRandomSales = new ArrayList<ItemSaleInfo>();
	private ArrayList<ItemSaleInfo> upcomingRandomSales = new ArrayList<ItemSaleInfo>();

	private ArrayList<ItemSaleInfo> sales = new ArrayList<ItemSaleInfo>();
	private RandomSaleConfig randomSaleConfig;

	private long lastReloadTime;

	public DefaultItemBlock[] defaultItems;

	private Random rnd = new Random();

	@Override
	public void initService() {
		logger = LogManager.getLogger("ItemManager");

		try {
			// Start reload watchdog
			CommonKvDataContainer cont = CommonDataManager.getInstance().getKeyValueContainer("ITEMMANAGER");
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

		// Update active sales
		logger.info("Updating sales...");
		try {
			// Find all categories
			ArrayList<Integer> categoryIds = new ArrayList<Integer>();
			for (ItemInfo itm : getAllItemDefinitions()) {
				ItemCategoryInfo[] cats = itm.getCategories();
				for (ItemCategoryInfo cat : cats) {
					if (!categoryIds.contains(cat.getCategoryID()))
						categoryIds.add(cat.getCategoryID());
				}
			}

			// Update sales
			CommonDataTableContainer<ItemSaleRow> contC = CommonDataManager.getInstance()
					.getDataTable("ITEMSALES_CURRENT", ItemSaleRow.class);
			CommonDataTableContainer<ItemSaleRow> contN = CommonDataManager.getInstance().getDataTable("ITEMSALES_NEXT",
					ItemSaleRow.class);
			ArrayList<ItemSaleInfo> currentRandomSales = new ArrayList<ItemSaleInfo>();
			ArrayList<ItemSaleInfo> upcomingRandomSales = new ArrayList<ItemSaleInfo>();
			for (int cat : categoryIds) {
				// Add to list
				ItemSaleRow current = contC.getFirstRow(new DataFilter(Map.of("categoryID", cat)));
				if (current != null) {
					// Add current
					currentRandomSales.add(new ItemSaleInfo("RANDOM SALE " + cat, current.startTime.getTime(),
							current.endTime.getTime(), current.modifier, new int[0], current.itemIDs,
							current.memberOnly));

					// Load next
					ItemSaleRow next = contN.getFirstRow(new DataFilter(Map.of("categoryID", cat)));
					upcomingRandomSales.add(new ItemSaleInfo("RANDOM SALE " + cat, next.startTime.getTime(),
							next.endTime.getTime(), next.modifier, new int[0], next.itemIDs, next.memberOnly));
				}
			}

			// Save sales to memory
			this.currentRandomSales = currentRandomSales;
			this.upcomingRandomSales = upcomingRandomSales;
		} catch (Exception e) {
			logger.error("Failed to load current random sales", e);
		}

		try {
			// Container controller
			CommonKvDataContainer contInfo = CommonDataManager.getInstance()
					.getKeyValueContainer("ITEMMANAGER_CONTROLLER");

			// Item sales
			DataTable<ItemSaleRow> contC = CommonDataManager.getInstance().getDataTable("ITEMSALES_CURRENT",
					ItemSaleRow.class);
			DataTable<ItemSaleRow> contN = CommonDataManager.getInstance().getDataTable("ITEMSALES_NEXT",
					ItemSaleRow.class);

			// Popular items
			CommonDataTableContainer<PopularItemRow> contPopularItemsLast = CommonDataManager.getInstance()
					.getDataTable("POPULARITEMS_LAST", PopularItemRow.class);

			// Start random sale selector
			AsyncTaskManager.runAsync(() -> {
				while (true) {
					// Check if a update is needed
					Calendar cal = Calendar.getInstance();
					Calendar calNext = Calendar.getInstance();
					boolean requiresUpdate = false;
					try {
						int val = -1;
						if (!contInfo.entryExists("lastupdate_itemsales")) {
							requiresUpdate = true;
						} else {
							val = contInfo.getEntry("lastupdate_itemsales").getAsInt();
						}

						// Check interval
						switch (randomSaleConfig.refreshInterval) {
						case DAILY:
							if (cal.get(Calendar.DAY_OF_WEEK) != val) {
								requiresUpdate = true;
								contInfo.setEntry("lastupdate_itemsales",
										new JsonPrimitive(cal.get(Calendar.DAY_OF_WEEK)));
								cal.set(Calendar.HOUR_OF_DAY, 0);
								calNext.set(Calendar.HOUR_OF_DAY, 0);
								calNext.add(Calendar.DAY_OF_WEEK, 1);
							}
							break;
						case MONTHLY:
							if (cal.get(Calendar.MONTH) != val) {
								requiresUpdate = true;
								contInfo.setEntry("lastupdate_itemsales", new JsonPrimitive(cal.get(Calendar.MONTH)));
								cal.set(Calendar.DAY_OF_MONTH, 0);
								calNext.set(Calendar.DAY_OF_MONTH, 0);
								calNext.add(Calendar.MONTH, 1);
							}
							break;
						case WEEKLY:
							if (cal.get(Calendar.WEEK_OF_YEAR) != val) {
								requiresUpdate = true;
								contInfo.setEntry("lastupdate_itemsales",
										new JsonPrimitive(cal.get(Calendar.WEEK_OF_YEAR)));
								cal.set(Calendar.DAY_OF_WEEK, 0);
								calNext.set(Calendar.DAY_OF_WEEK, 0);
								calNext.add(Calendar.WEEK_OF_YEAR, 1);
							}
							break;
						case YEARLY:
							if (cal.get(Calendar.YEAR) != val) {
								requiresUpdate = true;
								contInfo.setEntry("lastupdate_itemsales", new JsonPrimitive(cal.get(Calendar.YEAR)));
								cal.set(Calendar.MONTH, 0);
								calNext.set(Calendar.MONTH, 0);
								calNext.add(Calendar.YEAR, 1);
							}
							break;

						}

						// Update if needed
						if (requiresUpdate) {
							// Find all categories
							ArrayList<Integer> categoryIds = new ArrayList<Integer>();
							for (ItemInfo itm : getAllItemDefinitions()) {
								ItemCategoryInfo[] cats = itm.getCategories();
								for (ItemCategoryInfo cat : cats) {
									if (!categoryIds.contains(cat.getCategoryID()))
										categoryIds.add(cat.getCategoryID());
								}
							}

							// Compute sale duration
							cal.set(Calendar.HOUR_OF_DAY, 0);
							cal.set(Calendar.MINUTE, 0);
							cal.set(Calendar.SECOND, 0);
							calNext.set(Calendar.HOUR_OF_DAY, 0);
							calNext.set(Calendar.MINUTE, 0);
							calNext.set(Calendar.SECOND, 0);
							Date saleStart = cal.getTime();
							cal.add(Calendar.DAY_OF_MONTH, randomSaleConfig.expiryLength);
							Date saleEnd = cal.getTime();
							Date saleStartNext = calNext.getTime();
							calNext.add(Calendar.DAY_OF_MONTH, randomSaleConfig.expiryLength);
							Date saleEndNext = calNext.getTime();

							// Generate sale list
							Map<Integer, ItemSaleRow> salesNext = generateSales(saleStartNext, saleEndNext, contC,
									contN, contPopularItemsLast, categoryIds);
							Map<Integer, ItemSaleRow> salesCurrent = null;

							// Save sale information for each category and update active sales
							ArrayList<ItemSaleInfo> currentRandomSales = new ArrayList<ItemSaleInfo>();
							ArrayList<ItemSaleInfo> upcomingRandomSales = new ArrayList<ItemSaleInfo>();
							for (int cat : categoryIds) {
								if (!salesNext.containsKey(cat))
									continue;

								// Check if current is present
								ItemSaleRow saleNext = contN.getFirstRow(new DataFilter(Map.of("categoryID", cat)));
								if (saleNext == null) {
									// Save new sale set
									if (salesCurrent == null)
										salesCurrent = generateSales(saleStart, saleEnd, contC, contN,
												contPopularItemsLast, categoryIds);

									// Save next
									contN.setRows(salesNext.get(cat));

									// Set current
									contC.setRows(salesCurrent.get(cat));

									// Add to lists
									ItemSaleRow next = salesNext.get(cat);
									ItemSaleRow current = salesCurrent.get(cat);
									currentRandomSales.add(new ItemSaleInfo(
											"RANDOM SALE " + current.categoryID + " " + current.startTime.getTime(),
											current.startTime.getTime(), current.endTime.getTime(), current.modifier,
											new int[0], current.itemIDs, current.memberOnly));
									upcomingRandomSales.add(new ItemSaleInfo(
											"RANDOM SALE " + next.categoryID + " " + next.startTime.getTime(),
											next.startTime.getTime(), next.endTime.getTime(), next.modifier, new int[0],
											next.itemIDs, next.memberOnly));
								} else {
									// Load next into current
									contC.setRows(saleNext);

									// Save next
									contN.setRows(salesNext.get(cat));

									// Add to lists
									ItemSaleRow next = salesNext.get(cat);
									ItemSaleRow current = saleNext;
									currentRandomSales.add(new ItemSaleInfo(
											"RANDOM SALE " + current.categoryID + " " + current.startTime.getTime(),
											current.startTime.getTime(), current.endTime.getTime(), current.modifier,
											new int[0], current.itemIDs, current.memberOnly));
									upcomingRandomSales.add(new ItemSaleInfo(
											"RANDOM SALE " + next.categoryID + " " + next.startTime.getTime(),
											next.startTime.getTime(), next.endTime.getTime(), next.modifier, new int[0],
											next.itemIDs, next.memberOnly));
								}
							}

							// Save sales to memory
							this.currentRandomSales = currentRandomSales;
							this.upcomingRandomSales = upcomingRandomSales;
						}
					} catch (IOException e) {
						logger.error("Failed to update sales", e);
					}

					// Wait a bit
					try {
						Thread.sleep(30000);
					} catch (InterruptedException e) {
					}
				}
			});
		} catch (IllegalArgumentException e) {
		}

		// Init popular items
		logger.info("Initializing popular item manager...");
		initPopularItemManager();
	}

	private void initPopularItemManager() {
		// Refresh popular items in the background
		CommonKvDataContainer contInfo = CommonDataManager.getInstance().getKeyValueContainer("ITEMMANAGER_CONTROLLER");
		CommonDataTableContainer<PopularItemRow> contPopularItemsLast = CommonDataManager.getInstance()
				.getDataTable("POPULARITEMS_LAST", PopularItemRow.class);
		CommonDataTableContainer<PopularItemRow> contPopularItemsCurrent = CommonDataManager.getInstance()
				.getDataTable("POPULARITEMS_CURRENT", PopularItemRow.class);
		AsyncTaskManager.runAsync(() -> {
			while (true) {
				try {
					// Check if a refresh should be done, refreshes are weekly
					boolean requiresRefresh = false;
					if (!contInfo.entryExists("lastupdate_popularitems") || (System.currentTimeMillis()
							- contInfo.getEntry("lastupdate_popularitems").getAsLong()) > (7 * 24 * 60 * 60 * 1000)) {
						// Refresh needed
						requiresRefresh = true;
					}

					if (requiresRefresh) {
						// Set last update
						contInfo.setEntry("lastupdate_popularitems", new JsonPrimitive(System.currentTimeMillis()));

						// Create list of known updated items
						ArrayList<Integer> updatedItems = new ArrayList<Integer>();

						// Update
						PopularItemRow[] items = contPopularItemsCurrent.getAllRows();
						for (PopularItemRow item : items) {
							// Update or create
							contPopularItemsLast.setRows(item);
							updatedItems.add(item.itemID);
						}

						// Go through last items, remove those that no longer are present
						PopularItemRow[] lastItems = contPopularItemsLast.getAllRows();
						for (PopularItemRow item : lastItems) {
							if (!updatedItems.contains(item.itemID)) {
								// Remove
								contPopularItemsLast.removeRows(item);
							}
						}

						// Clear
						contPopularItemsCurrent.removeRows();
					}
				} catch (IOException e) {
					// Error
					LogManager.getLogger("ItemManager")
							.error("Failed to check if popular items need to be refreshed due to a database error.", e);
				}

				// Wait 30 seconds
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					break;
				}
			}
		});
	}

	private Map<Integer, ItemSaleRow> generateSales(Date saleStart, Date saleEnd, DataTable<ItemSaleRow> contC,
			DataTable<ItemSaleRow> contN, DataTable<PopularItemRow> contPopularItemsLast,
			ArrayList<Integer> categoryIds) throws IOException {
		// Create sales
		HashMap<Integer, ItemSaleRow> sales = new HashMap<Integer, ItemSaleRow>();
		for (int category : categoryIds) {
			// Load items for category
			HashMap<Integer, Integer> itemWeights = new HashMap<Integer, Integer>();

			// Select random sales for each store
			for (ItemStoreInfo store : getAllStores()) {
				// Load items
				for (ItemInfo itm : store.getItems()) {
					// Check category
					if (!Stream.of(itm.getCategories()).anyMatch(t -> t.getCategoryID() == category))
						continue;

					// Load popular item info
					int popularity = 0;
					PopularItemRow popularItem = contPopularItemsLast
							.getFirstRow(new DataFilter(Map.of("itemID", itm.getID())));
					if (popularItem != null)
						popularity = popularItem.popularity;

					// Add item
					int weight = 50 - (popularity / 50);
					if (weight <= 0)
						weight = 1;
					itemWeights.put(itm.getID(), weight);
				}
			}

			// Select items
			if (itemWeights.size() > 0) {
				// Calculate amount
				int amount = (itemWeights.size() / randomSaleConfig.saleCountFactor);
				if (amount < 1)
					amount = 1;
				else if (amount > randomSaleConfig.maximumSalesPerCategory)
					amount = randomSaleConfig.maximumSalesPerCategory;

				// Randomly select the item IDs
				ArrayList<Integer> itemIDs = new ArrayList<Integer>();
				for (int i = 0; i < amount; i++) {
					int id = RandomSelectorUtil.selectWeighted(itemWeights);
					while (itemIDs.contains(id))
						id = RandomSelectorUtil.selectWeighted(itemWeights);
					itemIDs.add(id);
				}

				// Select modifier
				float modifier = rnd.nextFloat(randomSaleConfig.randomMinimalModifier,
						randomSaleConfig.randomMaximalModifier);

				// Round the modifier
				DecimalFormatSymbols format = DecimalFormatSymbols.getInstance();
				format.setDecimalSeparator('.');
				modifier = Float.parseFloat(new DecimalFormat("0.000", format).format(modifier));

				// Create sale object
				int[] itemIdArr = new int[itemIDs.size()];
				for (int i = 0; i < itemIdArr.length; i++)
					itemIdArr[i] = itemIDs.get(i);
				ItemSaleRow sale = new ItemSaleRow();
				sale.categoryID = category;
				sale.startTime = saleStart;
				sale.endTime = saleEnd;
				sale.modifier = modifier;
				sale.memberOnly = false;
				sale.itemIDs = itemIdArr;
				sales.put(category, sale);
			}
		}
		return sales;
	}

	private void loadData() {
		// TODO: overhaul from here
		logger = logger;

		// Prepare
		HashMap<Integer, ItemInfo> itemDefs = new HashMap<Integer, ItemInfo>();
		HashMap<Integer, ItemStoreInfo> storeDefs = new HashMap<Integer, ItemStoreInfo>();
		ArrayList<ItemSaleInfo> sales = new ArrayList<ItemSaleInfo>();
		DefaultItemBlock[] defaultItems;

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
			mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
			ItemRegistryManifest reg = mapper.reader().readValue(data, ItemRegistryManifest.class);

			// Load default items
			defaultItems = reg.defaultItems.defaultItems;

			// Load items
			for (ObjectNode def : reg.itemDefs) {
				// Register item
				ItemInfo itm = new ItemInfo(def.get("id").asInt(), def.get("itn").asText(), def.get("d").asText(),
						mapper.convertValue(def, ItemDefData.class));
				itemDefs.put(itm.getID(), itm);
				logger.debug("Registered item: " + itm.getID() + ": " + itm.getName());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// Load store data
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
			mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
			ItemStoreDefinitionData[] stores = mapper.reader().readValue(data, ItemStoreDefinitionData[].class);

			// Load stores
			for (ItemStoreDefinitionData store : stores) {
				// Load store
				logger.debug("Loading store: " + store.storeID + " (" + store.storeName + ")");
				ItemInfo[] items = new ItemInfo[store.items.length];
				for (int i = 0; i < items.length; i++) {
					// Register item
					if (!itemDefs.containsKey(store.items[i].id)) {
						items[i] = new ItemInfo(store.items[i].id, store.items[i].name, store.items[i].description,
								store.items[i]);
						itemDefs.put(items[i].getID(), items[i]);
						logger.debug("Registered item: " + items[i].getID() + ": " + items[i].getName() + " to store "
								+ store.storeID);
					} else {
						items[i] = itemDefs.get(store.items[i].id);
						logger.debug("Registered item: " + items[i].getID() + ": " + items[i].getName() + " to store "
								+ store.storeID);
					}
				}
				storeDefs.put(store.storeID,
						new ItemStoreInfo(store.storeID, store.storeName, store.storeDescription, items));
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

		// Load sales
		logger.info("Loading item sales...");
		try {
			SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
			if (!ConfigProviderService.getInstance().configExists("server", "salesettings")) {
				Calendar cal = Calendar.getInstance();
				cal.set(Calendar.HOUR_OF_DAY, 12);
				cal.set(Calendar.MINUTE, 00);
				cal.set(Calendar.SECOND, 00);
				Date start = cal.getTime();
				cal = Calendar.getInstance();
				cal.set(Calendar.HOUR_OF_DAY, 12);
				cal.set(Calendar.MINUTE, 00);
				cal.set(Calendar.SECOND, 00);
				cal.add(Calendar.MONTH, 1);
				Date end = cal.getTime();

				JsonObject settings = new JsonObject();
				JsonObject randomSales = new JsonObject();
				randomSales.addProperty("__COMMENT1__",
						"The following settings control the interval and expiry for sales");
				randomSales.addProperty("__COMMENT2__",
						"The interval can be either daily, weekly, monthly or yearly, however make sure the expiry isnt too high else things can get weird");
				randomSales.addProperty("enabled", true);
				randomSales.addProperty("saleTimeDays", 20);
				randomSales.addProperty("refreshInterval", "monthly");
				randomSales.addProperty("__COMMENT3__",
						"The modifiers below control how much the random sales discount");
				randomSales.addProperty("randomMinimalModifier", 0.1f);
				randomSales.addProperty("randomMaximalModifier", 0.3f);
				randomSales.addProperty("__COMMENT4__",
						"The sale count factor is used to determine how many items should be on sale per category");
				randomSales.addProperty("saleCountFactor", 10);
				randomSales.addProperty("maximumSalesPerCategory", 12);
				settings.add("randomSales", randomSales);
				JsonArray s = new JsonArray();
				JsonObject s1 = new JsonObject();
				s1.addProperty("name", "Example sale, eggs and hatch tickets at 95% discount");
				s1.addProperty("start", fmt.format(start));
				s1.addProperty("end", fmt.format(end));
				s1.addProperty("modifier", 0.95f);
				JsonArray cats = new JsonArray();
				cats.add(456);
				cats.add(545);
				cats.add(546);
				cats.add(547);
				cats.add(548);
				cats.add(549);
				cats.add(550);
				cats.add(551);
				s1.add("categories", cats);
				JsonArray itemIDs = new JsonArray();
				itemIDs.add(18601);
				s1.add("itemIDs", itemIDs);
				s1.addProperty("memberOnly", false);
				s.add(s1);
				settings.add("sales", s);
				ConfigProviderService.getInstance().saveConfig("server", "salesettings", settings);
			}

			// Load config
			JsonObject saleConf = ConfigProviderService.getInstance().loadConfig("server", "salesettings");
			RandomSaleConfig config = new RandomSaleConfig();

			// Read properties
			JsonObject randomSales = saleConf.get("randomSales").getAsJsonObject();
			config.enabled = randomSales.get("enabled").getAsBoolean();
			config.expiryLength = randomSales.get("saleTimeDays").getAsInt();
			String interval = randomSales.get("refreshInterval").getAsString();
			if (interval.equalsIgnoreCase("monthly"))
				config.refreshInterval = RandomSaleInterval.MONTHLY;
			else if (interval.equalsIgnoreCase("yearly"))
				config.refreshInterval = RandomSaleInterval.YEARLY;
			else if (interval.equalsIgnoreCase("weekly"))
				config.refreshInterval = RandomSaleInterval.WEEKLY;
			else if (interval.equalsIgnoreCase("daily"))
				config.refreshInterval = RandomSaleInterval.DAILY;
			else
				throw new IOException("Invalid refreshInterval value: " + interval);
			config.randomMinimalModifier = randomSales.get("randomMinimalModifier").getAsFloat();
			config.randomMaximalModifier = randomSales.get("randomMaximalModifier").getAsFloat();
			config.saleCountFactor = randomSales.get("saleCountFactor").getAsInt();
			config.maximumSalesPerCategory = randomSales.get("maximumSalesPerCategory").getAsInt();

			// Load sales
			int saleID = Integer.MIN_VALUE;
			for (JsonElement objE : saleConf.get("sales").getAsJsonArray()) {
				if (saleID == 0) {
					throw new IOException("Too many sales registered, hit the upper limit for user sales");
				}
				saleID++;
				JsonObject sale = objE.getAsJsonObject();

				// Create item and category ID arrays
				int[] categories = new int[sale.get("categories").getAsJsonArray().size()];
				int[] itemIds = new int[sale.get("itemIDs").getAsJsonArray().size()];
				int in = 0;
				for (JsonElement ele : sale.get("categories").getAsJsonArray()) {
					categories[in++] = ele.getAsInt();
				}
				in = 0;
				for (JsonElement ele : sale.get("itemIDs").getAsJsonArray()) {
					itemIds[in++] = ele.getAsInt();
				}

				// Create sale object
				Date start = fmt.parse(sale.get("start").getAsString());
				Date end = fmt.parse(sale.get("end").getAsString());
				ItemSaleInfo i = new ItemSaleInfo(sale.get("name").getAsString(), start.getTime(), end.getTime(),
						sale.get("modifier").getAsFloat(), categories, itemIds, sale.get("memberOnly").getAsBoolean());
				sales.add(i);
				logger.debug("Registered sale: " + i.getName());
			}

			// Update
			randomSaleConfig = config;
		} catch (IOException | ParseException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			logger.error("Failed to configure sales", e);
		}

		// Apply
		this.defaultItems = defaultItems;
		this.itemDefs = itemDefs;
		this.storeDefs = storeDefs;
		this.sales = sales;

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
				mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
				ObjectNode def = mapper.reader().readValue(new String(strm.readAllBytes(), "UTF-8"), ObjectNode.class);
				strm.close();

				// Define if needed
				if (!itemDefs.containsKey(def.get("id").asInt())) {
					// Register
					ItemInfo itm = new ItemInfo(def.get("id").asInt(), def.get("itn").asText(), def.get("d").asText(),
							mapper.convertValue(def, ItemDefData.class));
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
					ItemDefData obj = itm.getRawObject();
					mapper.updateValue(obj, def);
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
					mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
					ObjectNode def = mapper.reader().readValue(new String(strm.readAllBytes(), "UTF-8"),
							ObjectNode.class);
					strm.close();

					// Define if needed
					if (!itemDefs.containsKey(def.get("id").asInt())) {
						// Register
						ItemInfo itm = new ItemInfo(def.get("id").asInt(), def.get("itn").asText(),
								def.get("d").asText(), mapper.convertValue(def, ItemDefData.class));
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
						ItemDefData obj = itm.getRawObject();
						mapper.updateValue(obj, def);
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
					// Verify
					if (trans.has("cost") && !trans.has("currency"))
						throw new IllegalArgumentException(
								"No 'currency' field present in transformer item! Item ID: " + id);
					else if (trans.has("cost") && trans.has("currency")
							&& !trans.get("currency").getAsString().equals("gems")
							&& !trans.get("currency").getAsString().equals("coins"))
						throw new IllegalArgumentException(
								"Invalid currency type in transformer item, expected either 'gems' or 'coins'");

					// Apply
					ObjectMapper mapper = new ObjectMapper();
					ObjectNode newNode = mapper.convertValue(def.getRawObject(), ObjectNode.class);
					if (trans.has("cost")) {
						newNode.set("ct", new IntNode(-1));
						newNode.set("ct2", new IntNode(0));
						if (trans.get("currency").getAsString().equals("coins"))
							newNode.set("ct", new IntNode(trans.get("cost").getAsInt()));
						else {
							newNode.set("ct2", new IntNode(trans.get("cost").getAsInt()));
							if (newNode.get("ct2").asInt() == 0)
								newNode.set("ct", new IntNode(0));
						}
					}

					// Create new def
					ItemInfo newDef = new ItemInfo(def.getID(), def.getDescription(), def.getDescription(),
							mapper.convertValue(newNode, ItemDefData.class));
					items.put(id, newDef);
				} else {
					// Remove
					items.remove(id);
				}
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
	public PlayerInventory getCommonInventory(AccountKvDataContainer data) {
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
		ObjectMapper mapper = new ObjectMapper();
		ItemDefData obj = itm.getRawObject();
		try {
			mapper.updateValue(obj, rawData);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		}
		itm.reloadDef();
		logger.debug("Updated item: " + itm.getID() + ": " + itm.getName());
	}

	@Override
	public void reload() {
		// Trigger a reload on all servers
		lastReloadTime = System.currentTimeMillis();
		try {
			CommonDataManager.getInstance().getKeyValueContainer("ITEMMANAGER").setEntry("lastreload",
					new JsonPrimitive(lastReloadTime));
		} catch (IOException e) {
		}
		logger.info("Reloading item manager...");
		loadData();
	}

	@Override
	public void registerSale(ItemSaleInfo sale) {
		sales.add(sale);
		logger.debug("Registered sale: " + sale.getName());
	}

	@Override
	public void unregisterSale(ItemSaleInfo sale) {
		sales.remove(sale);
		logger.debug("Removed sale: " + sale.getName());
	}

	@Override
	public ItemSaleInfo[] getSales() {
		ArrayList<ItemSaleInfo> sales = new ArrayList<ItemSaleInfo>(this.sales);
		sales.addAll(currentRandomSales);
		sales.addAll(upcomingRandomSales);
		return sales.toArray(t -> new ItemSaleInfo[t]);
	}

	@Override
	public ItemSaleInfo[] getActiveSales() {
		ArrayList<ItemSaleInfo> sales = new ArrayList<ItemSaleInfo>(this.sales);
		sales.addAll(currentRandomSales);
		return sales.stream().filter(t -> t.isActive()).toArray(t -> new ItemSaleInfo[t]);
	}

	@Override
	public ItemSaleInfo[] getUpcomingSales() {
		ArrayList<ItemSaleInfo> sales = new ArrayList<ItemSaleInfo>(this.sales);
		sales.addAll(upcomingRandomSales);
		return sales.stream().filter(t -> t.isUpcoming()).toArray(t -> new ItemSaleInfo[t]);
	}

}
