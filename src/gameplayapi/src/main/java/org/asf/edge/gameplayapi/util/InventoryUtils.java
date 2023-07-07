package org.asf.edge.gameplayapi.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.util.RandomSelectorUtil;
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
import org.asf.edge.modules.eventbus.EventBus;
import org.asf.edge.gameplayapi.xmls.inventories.InventoryUpdateResponseData.CurrencyUpdateBlock;
import org.asf.edge.gameplayapi.xmls.inventories.InventoryUpdateResponseData.PrizeItemInfo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
				null);

		// Save
		if (currencyUpdate.gemCount != currentG) {
			resp.currencyUpdate = currencyUpdate;
			currencyAccWide.setEntry("gems", new JsonPrimitive(currencyUpdate.gemCount));
		}
		if (currencyUpdate.coinCount != currentC) {
			resp.currencyUpdate = currencyUpdate;
			currency.setEntry("coins", new JsonPrimitive(currencyUpdate.coinCount));
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

	private static boolean processItemRedemption(ItemRedemptionInfo[] items, AccountObject account,
			AccountSaveContainer save, boolean openMysteryBoxes, ArrayList<ItemUpdateBlock> itemLst,
			ArrayList<PrizeItemInfo> prizeLst, CurrencyUpdateBlock currencyUpdate, PrizeItemInfo prizeInfo) {
		// Verify items
		for (ItemRedemptionInfo req : items) {
			// Check item def
			ItemInfo itm = itemManager.getItemDefinition(req.defID);
			if (itm == null)
				return false; // Invalid item ID
		}

		// Verify mystery boxes
		if (openMysteryBoxes) {
			// Go through redemption requests
			for (ItemRedemptionInfo req : items) {
				// Check if mystery box
				ItemInfo itm = itemManager.getItemDefinition(req.defID);
				if (isMysteryBox(itm)) {
					// Verify rewards
					for (JsonNode nd : getRelationsOfType("Prize", itm)) {
						if (!nd.has("id") || !nd.has("wt"))
							return false; // Invalid object

						// Check item def
						ItemInfo itm2 = itemManager.getItemDefinition(nd.get("id").asInt());
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
				// Regular item
				if (!addItem(itm, req, account, save, itemLst, prizeLst, currencyUpdate, prizeInfo))
					return false;
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
						for (JsonNode nd : getRelationsOfType("Prize", itm)) {
							// Load item info
							int weight = nd.get("wt").asInt();
							int defID = nd.get("id").asInt();
							int quantity = 1;
							if (nd.has("q"))
								quantity = nd.get("q").asInt();
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
							obj.mysteryPrizeItems = new ObjectNode[0];
							ItemInfo itm2 = itemManager.getItemDefinition(prize.defID);
							if (isBundle(itm2) || isMysteryBox(itm2)) {
								// Add as item
								obj.mysteryPrizeItems = new ObjectNode[] { itm2.getRawObject() };
							}

							// Add
							if (!processItemRedemption(new ItemRedemptionInfo[] { prize }, account, save, false,
									itemLst, prizeLst, currencyUpdate, obj))
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
				for (JsonNode nd : getRelationsOfType("Bundle", itm)) {
					// Load item info
					int defID = nd.get("id").asInt();
					int quantity = 1;
					if (nd.has("q"))
						quantity = nd.get("q").asInt();
					if (quantity == 0)
						quantity = 1;

					// Add items
					ItemRedemptionInfo it = new ItemRedemptionInfo();
					it.defID = defID;
					it.quantity = quantity * req.quantity;
					it.containerID = req.containerID;

					// Add
					if (!processItemRedemption(new ItemRedemptionInfo[] { it }, account, save, false, itemLst, prizeLst,
							currencyUpdate, null))
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
				ItemUpdateBlock block = resp.getUpdate();
				if (block != null) {
					// Add update
					itemLst.add(block);
				}

				// Add to prize info if needed
				ObjectNode infoBlock = resp.getItemDef();
				if (infoBlock != null && prizeInfo != null) {
					// Add
					prizeInfo.mysteryPrizeItems = appendTo(prizeInfo.mysteryPrizeItems, infoBlock);
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

	private static ObjectNode[] appendTo(ObjectNode[] arr, ObjectNode block) {
		ObjectNode[] newB = new ObjectNode[arr.length + 1];
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
			for (JsonNode nd : getRelationsOfType("Bundle", itm)) {
				if (!nd.has("id"))
					return false; // Invalid object

				// Check item def
				ItemInfo itm2 = itemManager.getItemDefinition(nd.get("id").asInt());
				if (itm2 == null)
					return false; // Invalid item ID

				// Check nested bundles
				if (!verifyBundle(itm2))
					return false;
			}
		}
		return true;
	}

	private static JsonNode[] getRelationsOfType(String type, ItemInfo itm) {
		ArrayList<JsonNode> nodes = new ArrayList<JsonNode>();

		// Check item def
		JsonNode node = itm.getRawObject().get("r");
		if (node.isArray()) {
			// Go through all nodes
			for (JsonNode n : node) {
				if (n.has("t")) {
					if (n.get("t").asText().equalsIgnoreCase(type))
						nodes.add(n);
				}
			}
		} else if (node.has("t")) {
			// Go through single item
			if (node.get("t").asText().equalsIgnoreCase(type))
				nodes.add(node);
		}

		// Return
		return nodes.toArray(t -> new JsonNode[t]);
	}

	private static boolean isMysteryBox(ItemInfo def) {
		// Check item def
		if (!def.getRawObject().has("r"))
			return false;
		JsonNode node = def.getRawObject().get("r");
		if (node.isArray()) {
			// Go through all nodes
			for (JsonNode n : node) {
				if (isMysteryItem(n)) {
					// Its a mystery box
					return true;
				}
			}

			// Not a mystery box
			return false;
		}

		// Go through single node
		return isMysteryItem(node);
	}

	private static boolean isMysteryItem(JsonNode node) {
		// Check type
		if (node.has("t")) {
			return node.get("t").asText().equalsIgnoreCase("Prize");
		}
		return false;
	}

	private static boolean isBundle(ItemInfo def) {
		// Check item def
		if (!def.getRawObject().has("r"))
			return false;
		JsonNode node = def.getRawObject().get("r");
		if (node.isArray()) {
			// Go through all nodes
			for (JsonNode n : node) {
				if (isBundleItem(n)) {
					// Its a bundle
					return true;
				}
			}

			// Not a bundle
			return false;
		}

		// Go through single node
		return isBundleItem(node);
	}

	private static boolean isBundleItem(JsonNode node) {
		// Check type
		if (node.has("t")) {
			return node.get("t").asText().equalsIgnoreCase("Bundle");
		}
		return false;
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

				// Find inventory
				int cQuant = 0;
				PlayerInventoryItem itm = null;
				if (request.itemUniqueID != -1)
					itm = cont.getItem(request.itemUniqueID);
				if (itm == null)
					itm = cont.findFirst(request.itemID);
				if (itm != null)
					cQuant = itm.getQuantity();

				// Check
				int newQuant = cQuant + request.quantity;
				if (newQuant < 0) {
					// Invalid
					LogManager.getLogger("ItemManager")
							.warn("Warning! Security checks did not pass for common inventory request of user '"
									+ data.getAccount().getUsername() + "', failed item ID: " + request.itemID
									+ " (quantity: " + request.quantity
									+ "), item was NOT added to inventory. (invalid quantity in request, resulting quantity would be less than zero)");
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
				// TODO: default security

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

				// Check old quantity
				int cQuant = 0;
				if (itm != null)
					cQuant = itm.getQuantity();
				if (cQuant <= 0 && request.quantity < 0) {
					// Invalid
					LogManager.getLogger("ItemManager")
							.warn("Warning! Security checks did not pass for common inventory request of user '"
									+ data.getAccount().getUsername() + "', failed item ID: " + request.itemID
									+ " (quantity: " + request.quantity
									+ "), item was NOT added to inventory. (invalid quantity in request, resulting quantity would be less than zero)");
					continue;
				}

				// Check
				if (itm == null)
					itm = cont.createItem(request.itemID, 0);

				// Update
				int newQuant = itm.getQuantity() + request.quantity;
				itm.setQuantity(newQuant);
				if (request.uses != null) {
					int uses = 0;
					if (itm.getUses() != -1)
						uses = itm.getUses();
					itm.setUses(uses + Integer.parseInt(request.uses));
				}

				// Add update
				if (newQuant > 0) {
					ItemUpdateBlock b = new ItemUpdateBlock();
					b.itemID = itm.getItemDefID();
					b.itemUniqueID = itm.getUniqueID();
					b.addedQuantity = request.quantity;
					updates.add(b);
				}
			}
		}

		// Set response
		resp.updateItems = updates.toArray(t -> new ItemUpdateBlock[t]);
		return resp;
	}

}
