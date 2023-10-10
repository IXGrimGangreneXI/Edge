package org.asf.edge.gameplayapi.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.entities.items.ItemInfo.CostInfo;
import org.asf.edge.common.experiments.EdgeDefaultExperiments;
import org.asf.edge.common.experiments.ExperimentManager;
import org.asf.edge.common.entities.items.ItemStoreInfo;
import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.commondata.CommonDataContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.util.RandomSelectorUtil;
import org.asf.edge.common.xmls.items.ItemDefData;
import org.asf.edge.common.xmls.items.relation.ItemRelationData;
import org.asf.edge.gameplayapi.events.items.InventoryUtilsLoadEvent;
import org.asf.edge.gameplayapi.util.inventory.AbstractInventorySecurityValidator;
import org.asf.edge.gameplayapi.util.inventory.AbstractItemRedemptionHandler;
import org.asf.edge.gameplayapi.util.inventory.AbstractItemRedemptionHandler.RedemptionResult;
import org.asf.edge.gameplayapi.util.inventory.ItemRedemptionInfo;
import org.asf.edge.gameplayapi.util.inventory.defaulthandlers.CoinItemRedemptionHandler;
import org.asf.edge.gameplayapi.util.inventory.defaulthandlers.DefaultItemRedemptionHandler;
import org.asf.edge.gameplayapi.util.inventory.defaulthandlers.GemItemRedemptionHandler;
import org.asf.edge.gameplayapi.util.inventory.defaulthandlers.ProfileSlotItemRedemptionHandler;
import org.asf.edge.gameplayapi.util.inventory.defaultvalidators.AttributeSecurityValidator;
import org.asf.edge.gameplayapi.util.inventory.defaultvalidators.BundleSecurityValidator;
import org.asf.edge.gameplayapi.util.inventory.defaultvalidators.MysteryBoxSecurityValidator;
import org.asf.edge.gameplayapi.xmls.inventories.InventoryUpdateResponseData;
import org.asf.edge.gameplayapi.xmls.inventories.SetCommonInventoryRequestData;
import org.asf.edge.gameplayapi.xmls.inventories.InventoryUpdateResponseData.CurrencyUpdateBlock;
import org.asf.edge.gameplayapi.xmls.inventories.InventoryUpdateResponseData.PrizeItemInfo;
import org.asf.edge.modules.eventbus.EventBus;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.asf.edge.gameplayapi.xmls.inventories.InventoryUpdateResponseData.ItemUpdateBlock;

/**
 * 
 * Player Inventory Utilities for the gameplay API, practically a wrapper around
 * Edge's internal systems
 * 
 * @author Sky Swimmer
 *
 */
public class InventoryUtils {

	private static ArrayList<AbstractItemRedemptionHandler> handlers = new ArrayList<AbstractItemRedemptionHandler>();
	private static ArrayList<AbstractInventorySecurityValidator> securityValidators = new ArrayList<AbstractInventorySecurityValidator>();

	private static ItemManager itemManager;
	private static boolean inited;

	static {
		// Register default handlers
		registerItemRedemptionHandler(new DefaultItemRedemptionHandler());
		registerItemRedemptionHandler(new ProfileSlotItemRedemptionHandler());
		registerItemRedemptionHandler(new GemItemRedemptionHandler());
		registerItemRedemptionHandler(new CoinItemRedemptionHandler());

		// Register default security layers
		registerInventorySecurityValidator(new AttributeSecurityValidator());
		registerInventorySecurityValidator(new MysteryBoxSecurityValidator());
		registerInventorySecurityValidator(new BundleSecurityValidator());
	}

	/**
	 * Initializes the inventory utility system
	 */
	public static void init() {
		if (inited)
			return;
		inited = true;

		// Dispatch
		EventBus.getInstance().dispatchEvent(new InventoryUtilsLoadEvent());
	}

	/**
	 * Registers item redemption handlers
	 * 
	 * @param handler Handler to register
	 */
	public static void registerItemRedemptionHandler(AbstractItemRedemptionHandler handler) {
		handlers.add(0, handler);
	}

	/**
	 * Registers security validators for common inventory client requests
	 * 
	 * @param validator Validator to register
	 */
	public static void registerInventorySecurityValidator(AbstractInventorySecurityValidator validator) {
		securityValidators.add(validator);
	}

	/**
	 * Adds items to the inventory, this processes things like bundles and mystery
	 * boxes, this DOES NOT remove the requested items from the inventory on
	 * redemption, used by the shop, mystery boxes and item codes
	 * 
	 * @param items            Items to redeem
	 * @param account          Account object
	 * @param save             Save to use
	 * @param openMysteryBoxes True to automatically open mystery boxes, false to
	 *                         add them to the inventory instead
	 * @return InventoryUpdateResponseData instance
	 * @throws IOException If redeeming the items fails
	 */
	public static InventoryUpdateResponseData redeemItems(ItemRedemptionInfo[] items, AccountObject account,
			AccountSaveContainer save, boolean openMysteryBoxes) throws IOException {
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Prepare response
		InventoryUpdateResponseData resp = new InventoryUpdateResponseData();
		ArrayList<ItemUpdateBlock> itemLst = new ArrayList<ItemUpdateBlock>();
		ArrayList<PrizeItemInfo> prizeLst = new ArrayList<PrizeItemInfo>();
		CurrencyUpdateBlock currencyUpdate = new CurrencyUpdateBlock();
		currencyUpdate.userID = save.getSaveID();
		resp.success = true;

		// Load currency
		AccountDataContainer currency = save.getSaveData().getChildContainer("currency");
		int currentC = 300;
		if (currency.entryExists("coins"))
			currentC = currency.getEntry("coins").getAsInt();
		AccountDataContainer currencyAccWide = save.getAccount().getAccountData().getChildContainer("currency");
		int currentG = 0;
		if (currencyAccWide.entryExists("gems"))
			currentG = currencyAccWide.getEntry("gems").getAsInt();
		currencyUpdate.coinCount = currentC;
		currencyUpdate.gemCount = currentG;

		// Handle requests
		resp.success = processItemRedemption(items, account, save, openMysteryBoxes, itemLst, prizeLst, currencyUpdate,
				null, null, null, null);

		// Save
		if (currencyUpdate.gemCount != currentG) {
			resp.currencyUpdate = currencyUpdate;
			currencyAccWide.setEntry("gems", new JsonPrimitive(currencyUpdate.gemCount));
		}
		if (currencyUpdate.coinCount != currentC) {
			resp.currencyUpdate = currencyUpdate;
			currency.setEntry("coins", new JsonPrimitive(currencyUpdate.coinCount));
		}

		if (ExperimentManager.getInstance().isExperimentEnabled(EdgeDefaultExperiments.LEGACY_INVENTORY_SUPPORT)) {
			// Rebuild update list for 2.x support without quantity fields
			ArrayList<ItemUpdateBlock> itemLstNew = new ArrayList<ItemUpdateBlock>();
			for (ItemUpdateBlock block : itemLst) {
				if (block.quantity <= 0)
					itemLstNew.add(block);
				else {
					for (int i = 0; i < block.quantity; i++) {
						ItemUpdateBlock nB = new ItemUpdateBlock();
						nB.quantity = 1;
						nB.itemID = block.itemID;
						nB.itemUniqueID = block.itemUniqueID;
						itemLstNew.add(nB);
					}
				}
			}
			itemLst = itemLstNew;
		}

		// Set response
		resp.prizeItems = prizeLst.toArray(t -> new PrizeItemInfo[t]);
		resp.updateItems = itemLst.toArray(t -> new ItemUpdateBlock[t]);
		if (resp.prizeItems.length == 0)
			resp.prizeItems = null;
		if (resp.updateItems.length == 0)
			resp.updateItems = null;
		return resp;
	}

	/**
	 * Processes common inventory requests
	 * 
	 * @param requests  Request list
	 * @param container Container
	 * @return InventoryUpdateResponseData instance
	 */
	public static InventoryUpdateResponseData processCommonInventorySet(SetCommonInventoryRequestData[] requests,
			AccountDataContainer data, int container) {
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Prepare response
		InventoryUpdateResponseData resp = new InventoryUpdateResponseData();
		ArrayList<ItemUpdateBlock> updates = new ArrayList<ItemUpdateBlock>();
		resp.success = true;

		// Handle requests
		if (requests.length == 0) {
			resp.success = false;
		} else {
			// Verify
			PlayerInventory inv = itemManager.getCommonInventory(data);
			PlayerInventoryContainer cont = inv.getContainer(container);
			for (SetCommonInventoryRequestData request : requests) {
				if (itemManager.getItemDefinition(request.itemID) == null) {
					// Invalid
					LogManager.getLogger("ItemManager")
							.warn("Warning! Security checks did not pass for common inventory request of user '"
									+ data.getAccount().getUsername() + "', failed item ID: " + request.itemID
									+ " (quantity: " + request.quantity
									+ "), item was NOT added to inventory. (invalid item ID in request)");
					resp = new InventoryUpdateResponseData();
					resp.success = false;
					return resp;
				}
			}

			// Add
			for (SetCommonInventoryRequestData request : requests) {
				if (itemManager.getItemDefinition(request.itemID) == null) {
					// Invalid
					resp = new InventoryUpdateResponseData();
					resp.success = false;
					return resp;
				}

				// Find inventory
				PlayerInventoryItem itm = null;
				if (request.itemUniqueID != -1)
					itm = cont.getItem(request.itemUniqueID);
				if (itm == null)
					itm = cont.findFirst(request.itemID);
				// TODO: complete implementation

				// Run security checks
				boolean invalid = false;
				for (AbstractInventorySecurityValidator validator : securityValidators) {
					if (!validator.isValidRequest(request, data, inv, cont, itm)) {
						invalid = true;
						LogManager.getLogger("ItemManager")
								.warn("Warning! Security checks did not pass for common inventory request of user '"
										+ data.getAccount().getUsername() + "', failed item ID: " + request.itemID
										+ " (quantity: " + request.quantity
										+ "), item was NOT added to inventory. (failed validator: "
										+ validator.getClass().getSimpleName() + ")");
						break;
					}
				}
				if (invalid)
					continue; // Skip the request

				// Check
				int newQuant = request.quantity;
				if (itm != null || request.quantity > 0) {
					if (itm == null)
						itm = cont.createItem(request.itemID, 0);

					// Update
					newQuant = itm.getQuantity() + request.quantity;
					itm.setQuantity(newQuant);
					if (request.uses != null) {
						int uses = 0;
						if (itm.getUses() != -1)
							uses = itm.getUses();
						itm.setUses(uses + Integer.parseInt(request.uses));
					}
				}

				// Add update
				if (newQuant > 0 || request.quantity == 0) {
					ItemUpdateBlock b = new ItemUpdateBlock();
					b.itemID = itm.getItemDefID();
					b.itemUniqueID = itm.getUniqueID();
					b.quantity = request.quantity;
					updates.add(b);
				}
			}
		}

		if (ExperimentManager.getInstance().isExperimentEnabled(EdgeDefaultExperiments.LEGACY_INVENTORY_SUPPORT)) {
			// Rebuild update list for 2.x support without quantity fields
			ArrayList<ItemUpdateBlock> itemLstNew = new ArrayList<ItemUpdateBlock>();
			for (ItemUpdateBlock block : updates) {
				if (block.quantity <= 0)
					itemLstNew.add(block);
				else {
					for (int i = 0; i < block.quantity; i++) {
						ItemUpdateBlock nB = new ItemUpdateBlock();
						nB.quantity = 1;
						nB.itemID = block.itemID;
						nB.itemUniqueID = block.itemUniqueID;
						itemLstNew.add(nB);
					}
				}
			}
			updates = itemLstNew;
		}

		// Set response
		resp.updateItems = updates.toArray(t -> new ItemUpdateBlock[t]);
		return resp;
	}

	/**
	 * Purchases items
	 * 
	 * @param shopID           Shop ID
	 * @param items            Items to buy
	 * @param account          Account object
	 * @param save             Save to use
	 * @param openMysteryBoxes True to automatically open mystery boxes, false to
	 *                         add them to the inventory instead
	 * @return InventoryUpdateResponseData instance
	 * @throws IOException If purchasing the items fails
	 */
	public static InventoryUpdateResponseData purchaseItems(int shopID, ItemRedemptionInfo[] items,
			AccountObject account, AccountSaveContainer save, boolean openMysteryBoxes) throws IOException {
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Find store
		ItemStoreInfo store = ItemManager.getInstance().getStore(shopID);
		if (store != null) {
			// Create currency block
			CurrencyUpdateBlock currencyUpdate = new CurrencyUpdateBlock();
			currencyUpdate.userID = save.getSaveID();

			// Load currency
			AccountDataContainer currency = save.getSaveData().getChildContainer("currency");
			int currentC = 300;
			if (currency.entryExists("coins"))
				currentC = currency.getEntry("coins").getAsInt();
			AccountDataContainer currencyAccWide = save.getAccount().getAccountData().getChildContainer("currency");
			int currentG = 0;
			if (currencyAccWide.entryExists("gems"))
				currentG = currencyAccWide.getEntry("gems").getAsInt();
			currencyUpdate.coinCount = currentC;
			currencyUpdate.gemCount = currentG;

			// Validate items
			int costGemsTotal = 0;
			int costCoinsTotal = 0;
			for (ItemRedemptionInfo itm : items) {
				if (store.getItem(itm.defID) == null) {
					// Invalid item ID
					InventoryUpdateResponseData fail = new InventoryUpdateResponseData();
					fail.success = false;
					return fail;
				}

				// Check quanity
				if (itm.quantity < 0) {
					// Invalid quantity
					InventoryUpdateResponseData fail = new InventoryUpdateResponseData();
					fail.success = false;
					return fail;
				}

				// Verify cost
				CostInfo cost = store.getItem(itm.defID).getFinalCost(false); // FIXME: membership support
				if (!cost.isFree) {
					if (cost.isGems) {
						costGemsTotal += cost.cost * itm.quantity;
					} else if (cost.isCoins) {
						costCoinsTotal += cost.cost * itm.quantity;
					}
				}
			}

			// Check
			if (costGemsTotal > currencyUpdate.gemCount) {
				// Not enough gems
				InventoryUpdateResponseData fail = new InventoryUpdateResponseData();
				fail.success = false;
				return fail;
			} else if (costCoinsTotal > currencyUpdate.coinCount) {
				// Not enough coins
				InventoryUpdateResponseData fail = new InventoryUpdateResponseData();
				fail.success = false;
				return fail;
			}

			// Prepare response
			InventoryUpdateResponseData resp = new InventoryUpdateResponseData();
			ArrayList<ItemUpdateBlock> itemLst = new ArrayList<ItemUpdateBlock>();
			ArrayList<PrizeItemInfo> prizeLst = new ArrayList<PrizeItemInfo>();

			// Handle requests
			resp.success = processItemRedemption(items, account, save, openMysteryBoxes, itemLst, prizeLst,
					currencyUpdate, null, (item, quant) -> {
						// Payment
						item = store.getItem(item.getID());
						CostInfo cost = item.getFinalCost(false); // FIXME: membership support
						if (!cost.isFree) {
							if (cost.isGems) {
								if ((cost.cost * quant) > currencyUpdate.gemCount) {
									// Not enough gems
									return false;
								}
							} else if (cost.isCoins) {
								if ((cost.cost * quant) > currencyUpdate.coinCount) {
									// Not enough coins
									return false;
								}
							}
						}
						return true;
					}, (item, quant) -> {
						// Payment
						item = store.getItem(item.getID());
						CostInfo cost = item.getFinalCost(false); // FIXME: membership support
						if (!cost.isFree) {
							// Check type
							if (cost.isGems) {
								if ((cost.cost * quant) <= currencyUpdate.gemCount) {
									// Apply cost
									currencyUpdate.gemCount -= (cost.cost * quant);
								} else {
									// Not enough gems
									return false;
								}
							} else if (cost.isCoins) {
								if ((cost.cost * quant) <= currencyUpdate.coinCount) {
									// Apply cost
									currencyUpdate.coinCount -= (cost.cost * quant);
								} else {
									// Not enough coins
									return false;
								}
							}

							try {
								// Load popular item data
								CommonDataContainer contPopularItems = CommonDataManager.getInstance()
										.getContainer("POPULARITEMS");
								JsonObject popularItems = new JsonObject();
								if (contPopularItems.entryExists("current-" + store.getID()))
									popularItems = contPopularItems.getEntry("current-" + store.getID())
											.getAsJsonObject();

								// Load item
								int last = 0;
								if (popularItems.has(Integer.toString(item.getID())))
									last = popularItems.get(Integer.toString(item.getID())).getAsInt();

								// Apply
								popularItems.addProperty(Integer.toString(item.getID()), last + 1);

								// Save
								contPopularItems.setEntry("current-" + store.getID(), popularItems);
							} catch (IOException e) {
								LogManager.getLogger().error("Failed to save popular items", e);
							}
						}

						// Return
						return true;
					}, null);

			// Save
			if (currencyUpdate.gemCount != currentG) {
				resp.currencyUpdate = currencyUpdate;
				currencyAccWide.setEntry("gems", new JsonPrimitive(currencyUpdate.gemCount));
			}
			if (currencyUpdate.coinCount != currentC) {
				resp.currencyUpdate = currencyUpdate;
				currency.setEntry("coins", new JsonPrimitive(currencyUpdate.coinCount));
			}

			if (ExperimentManager.getInstance().isExperimentEnabled(EdgeDefaultExperiments.LEGACY_INVENTORY_SUPPORT)) {
				// Rebuild update list for 2.x support without quantity fields
				ArrayList<ItemUpdateBlock> itemLstNew = new ArrayList<ItemUpdateBlock>();
				for (ItemUpdateBlock block : itemLst) {
					if (block.quantity <= 0)
						itemLstNew.add(block);
					else {
						for (int i = 0; i < block.quantity; i++) {
							ItemUpdateBlock nB = new ItemUpdateBlock();
							nB.quantity = 1;
							nB.itemID = block.itemID;
							nB.itemUniqueID = block.itemUniqueID;
							itemLstNew.add(nB);
						}
					}
				}
				itemLst = itemLstNew;
			}

			// Set response
			resp.prizeItems = prizeLst.toArray(t -> new PrizeItemInfo[t]);
			resp.updateItems = itemLst.toArray(t -> new ItemUpdateBlock[t]);
			if (resp.prizeItems.length == 0)
				resp.prizeItems = null;
			if (resp.updateItems.length == 0)
				resp.updateItems = null;
			return resp;
		}
		InventoryUpdateResponseData fail = new InventoryUpdateResponseData();
		fail.success = false;
		return fail;
	}

	private static boolean processItemRedemption(ItemRedemptionInfo[] items, AccountObject account,
			AccountSaveContainer save, boolean openMysteryBoxes, ArrayList<ItemUpdateBlock> itemLst,
			ArrayList<PrizeItemInfo> prizeLst, CurrencyUpdateBlock currencyUpdate, PrizeItemInfo prizeInfo,
			BiFunction<ItemInfo, Integer, Boolean> itemRedemptionValidationCall,
			BiFunction<ItemInfo, Integer, Boolean> itemRedemptionCall, Consumer<ItemInfo> innerItemRedemptionCall) {
		// Verify items
		for (ItemRedemptionInfo req : items) {
			// Check item def
			ItemInfo itm = itemManager.getItemDefinition(req.defID);
			if (itm == null)
				return false; // Invalid item ID

			// Check
			if (itemRedemptionValidationCall != null)
				if (!itemRedemptionValidationCall.apply(itm, req.quantity))
					return false;
		}

		// Verify mystery boxes
		if (openMysteryBoxes) {
			// Go through redemption requests
			for (ItemRedemptionInfo req : items) {
				// Check if mystery box
				ItemInfo itm = itemManager.getItemDefinition(req.defID);
				if (isMysteryBox(itm)) {
					// Verify rewards
					for (ItemRelationData nd : getRelationsOfType("Prize", itm)) {
						// Check item def
						ItemInfo itm2 = itemManager.getItemDefinition(nd.itemID);
						if (itm2 == null)
							return false; // Invalid item ID
					}
				}
			}
		}

		// Verify bundles
		for (ItemRedemptionInfo req : items) {
			// Check if bundle
			ItemInfo itm = itemManager.getItemDefinition(req.defID);
			if (!verifyBundle(itm))
				return false;
		}

		// Handle items
		for (ItemRedemptionInfo req : items) {
			// Pull ID
			ItemInfo itm = itemManager.getItemDefinition(req.defID);

			// Check if its a bundle or box
			if (!isBundle(itm) && (!openMysteryBoxes || !isMysteryBox(itm))) {
				// Attempt redemption
				if (itemRedemptionCall != null)
					if (!itemRedemptionCall.apply(itm, req.quantity))
						return false;

				// Regular item
				if (!addItem(itm, req, account, save, itemLst, prizeLst, currencyUpdate, prizeInfo))
					return false;

				// Call
				if (innerItemRedemptionCall != null)
					innerItemRedemptionCall.accept(itm);
			}
		}

		// Handle mystery boxes
		if (openMysteryBoxes) {
			for (ItemRedemptionInfo req : items) {
				// Pull ID
				ItemInfo itm = itemManager.getItemDefinition(req.defID);

				// Check if its a box
				if (isMysteryBox(itm)) {
					for (int l = 0; l < req.quantity; l++) {
						// Gather prize items
						HashMap<ItemRedemptionInfo, Integer> prizes = new HashMap<ItemRedemptionInfo, Integer>();
						for (ItemRelationData nd : getRelationsOfType("Prize", itm)) {
							// Load item info
							int weight = nd.weight;
							int defID = nd.itemID;
							int quantity = nd.quantity;
							if (quantity == 0)
								quantity = 1;

							// Add items to prize pool
							ItemRedemptionInfo it = new ItemRedemptionInfo();
							it.defID = defID;
							it.quantity = quantity;
							it.containerID = req.containerID;
							prizes.put(it, weight);
						}

						// Select prize
						ItemRedemptionInfo prize = RandomSelectorUtil.selectWeighted(prizes);
						if (prize != null) {
							// Create info object
							PrizeItemInfo obj = new PrizeItemInfo();
							obj.boxItemID = itm.getID();
							obj.prizeItemID = prize.defID;
							obj.mysteryPrizeItems = new ItemDefData[0];
							ItemInfo itm2 = itemManager.getItemDefinition(prize.defID);
							if (isBundle(itm2) || isMysteryBox(itm2)) {
								// Add as item
								obj.mysteryPrizeItems = new ItemDefData[] { itm2.getRawObject() };
							}

							// Add
							if (!processItemRedemption(new ItemRedemptionInfo[] { prize }, account, save, false,
									itemLst, prizeLst, currencyUpdate, obj, null, null, innerItemRedemptionCall))
								return false;
							prizeLst.add(obj);
						}
					}
				}
			}
		}

		// Handle bundles
		for (ItemRedemptionInfo req : items) {
			// Pull ID
			ItemInfo itm = itemManager.getItemDefinition(req.defID);

			// Check if its a bundle
			if (isBundle(itm)) {
				// Add bundle items
				for (ItemRelationData nd : getRelationsOfType("Bundle", itm)) {
					// Load item info
					int defID = nd.itemID;
					int quantity = nd.quantity;
					if (quantity == 0)
						quantity = 1;

					// Add items
					ItemRedemptionInfo it = new ItemRedemptionInfo();
					it.defID = defID;
					it.quantity = quantity * req.quantity;
					it.containerID = req.containerID;

					// Add
					if (!processItemRedemption(new ItemRedemptionInfo[] { it }, account, save, false, itemLst, prizeLst,
							currencyUpdate, null, null, null, innerItemRedemptionCall))
						return false;
				}
			}
		}

		// Success status
		return true;
	}

	private static boolean addItem(ItemInfo itm, ItemRedemptionInfo req, AccountObject account,
			AccountSaveContainer save, ArrayList<ItemUpdateBlock> itemLst, ArrayList<PrizeItemInfo> prizeLst,
			CurrencyUpdateBlock currencyUpdate, PrizeItemInfo prizeInfo) {
		// Find handler
		for (AbstractItemRedemptionHandler handler : handlers) {
			if (handler.canHandle(itm)) {
				// Handle
				RedemptionResult resp = handler.handleRedemption(itm, req, account, save, currencyUpdate);

				// Handle update
				for (ItemUpdateBlock block : resp.getUpdates()) {
					if (block != null) {
						// Add update
						itemLst.add(block);

						// Add to prize info if needed
						ItemInfo def = itemManager.getItemDefinition(block.itemID);
						if (def != null) {
							ItemDefData infoBlock = def.getRawObject();
							if (infoBlock != null && prizeInfo != null) {
								// Add
								prizeInfo.mysteryPrizeItems = appendTo(prizeInfo.mysteryPrizeItems, infoBlock);
							}
						}
					}
				}

				// If failed, return result
				if (!resp.isSuccessful())
					return false;

				// Delegate if needed, else return result
				if (!handler.delegating())
					return true;
			}
		}
		return false;
	}

	private static ItemDefData[] appendTo(ItemDefData[] arr, ItemDefData block) {
		ItemDefData[] newB = new ItemDefData[arr.length + 1];
		for (int i = 0; i < newB.length; i++) {
			if (i >= arr.length)
				newB[i] = block;
			else
				newB[i] = arr[i];
		}
		return newB;
	}

	private static boolean verifyBundle(ItemInfo itm) {
		if (isBundle(itm)) {
			// Verify rewards
			for (ItemRelationData nd : getRelationsOfType("Bundle", itm)) {
				// Check item def
				ItemInfo itm2 = itemManager.getItemDefinition(nd.itemID);
				if (itm2 == null)
					return false; // Invalid item ID

				// Check nested bundles
				if (!verifyBundle(itm2))
					return false;
			}
		}
		return true;
	}

	private static ItemRelationData[] getRelationsOfType(String type, ItemInfo itm) {
		ArrayList<ItemRelationData> rels = new ArrayList<ItemRelationData>();

		// Go through relations
		for (ItemRelationData n : itm.getRawObject().relations) {
			if (n.type.equalsIgnoreCase(type))
				rels.add(n);
		}

		// Return
		return rels.toArray(t -> new ItemRelationData[t]);
	}

	private static boolean isMysteryBox(ItemInfo def) {
		// Go through relations
		for (ItemRelationData n : def.getRawObject().relations) {
			if (isMysteryItem(n)) {
				// Its a mystery box
				return true;
			}
		}

		// Not a mystery box
		return false;
	}

	private static boolean isMysteryItem(ItemRelationData rel) {
		// Check type
		return rel.type.equalsIgnoreCase("Prize");
	}

	private static boolean isBundle(ItemInfo def) {
		// Go through relations
		for (ItemRelationData n : def.getRawObject().relations) {
			if (isBundleItem(n)) {
				// Its a bundle
				return true;
			}
		}

		// Not a bundle
		return false;
	}

	private static boolean isBundleItem(ItemRelationData rel) {
		// Check type
		return rel.type.equalsIgnoreCase("Bundle");
	}

}
