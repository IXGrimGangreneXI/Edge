package org.asf.edge.gameplayapi.http.handlers.gameplayapi;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.asf.connective.RemoteClient;
import org.asf.connective.TlsSecuredHttpServer;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.entities.coordinates.Vector3D;
import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.entities.minigamedata.MinigameData;
import org.asf.edge.common.entities.minigamedata.MinigameDataRequest;
import org.asf.edge.common.entities.minigamedata.MinigameSaveRequest;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionResult;
import org.asf.edge.common.http.apihandlerutils.functions.LegacyFunction;
import org.asf.edge.common.http.apihandlerutils.functions.LegacyFunctionInfo;
import org.asf.edge.common.http.apihandlerutils.functions.SodRequest;
import org.asf.edge.common.http.apihandlerutils.functions.SodRequestParam;
import org.asf.edge.common.http.apihandlerutils.functions.SodTokenSecured;
import org.asf.edge.common.http.apihandlerutils.functions.TokenRequireCapability;
import org.asf.edge.common.http.apihandlerutils.functions.TokenRequireSave;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.services.minigamedata.MinigameDataManager;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.common.xmls.data.EmptyKeyValuePairSetData;
import org.asf.edge.common.xmls.data.KeyValuePairData;
import org.asf.edge.common.xmls.data.KeyValuePairSetData;
import org.asf.edge.common.xmls.items.ItemDefData;
import org.asf.edge.common.xmls.items.inventory.InventoryItemEntryData;
import org.asf.edge.common.xmls.items.state.ItemStateCriteriaWrapperData;
import org.asf.edge.common.xmls.items.state.ItemStateData;
import org.asf.edge.common.xmls.items.state.criteria.ItemStateConsumableCriteriaData;
import org.asf.edge.common.xmls.items.state.criteria.ItemStateExpiryCriteriaData;
import org.asf.edge.common.xmls.items.state.criteria.ItemStateLengthCriteriaData;
import org.asf.edge.common.xmls.items.state.criteria.ItemStateSpeedUpCriteriaData;
import org.asf.edge.gameplayapi.EdgeGameplayApiServer;
import org.asf.edge.gameplayapi.entities.quests.UserQuestInfo;
import org.asf.edge.gameplayapi.entities.rooms.PlayerRoomInfo;
import org.asf.edge.gameplayapi.entities.rooms.RoomItemInfo;
import org.asf.edge.gameplayapi.services.quests.QuestManager;
import org.asf.edge.gameplayapi.services.rooms.PlayerRoomManager;
import org.asf.edge.gameplayapi.util.InventoryUtils;
import org.asf.edge.gameplayapi.util.RewardsUtil;
import org.asf.edge.gameplayapi.util.inventory.ItemRedemptionInfo;
import org.asf.edge.gameplayapi.xmls.dragons.DragonData;
import org.asf.edge.gameplayapi.xmls.dragons.DragonListData;
import org.asf.edge.gameplayapi.xmls.inventories.CommonInventoryData;
import org.asf.edge.gameplayapi.xmls.inventories.SetCommonInventoryRequestData;
import org.asf.edge.gameplayapi.xmls.items.ItemRedeemRequestData;
import org.asf.edge.gameplayapi.xmls.minigamedata.GameDataSummaryData;
import org.asf.edge.gameplayapi.xmls.inventories.InventoryUpdateResponseData;
import org.asf.edge.gameplayapi.xmls.inventories.InventoryUpdateResponseData.CurrencyUpdateBlock;
import org.asf.edge.gameplayapi.xmls.inventories.InventoryUpdateResponseData.ItemUpdateBlock;
import org.asf.edge.gameplayapi.xmls.names.DisplayNameUniqueResponseData;
import org.asf.edge.gameplayapi.xmls.quests.MissionData;
import org.asf.edge.gameplayapi.xmls.quests.MissionData.MissionRulesBlock.PrerequisiteInfoBlock;
import org.asf.edge.gameplayapi.xmls.quests.MissionData.MissionRulesBlock.PrerequisiteInfoBlock.PrerequisiteRuleTypes;
import org.asf.edge.gameplayapi.xmls.quests.QuestListResponseData;
import org.asf.edge.gameplayapi.xmls.quests.RequestFilterDataLegacy;
import org.asf.edge.gameplayapi.xmls.quests.SetTaskStateResultData;
import org.asf.edge.gameplayapi.xmls.rooms.RoomItemData;
import org.asf.edge.gameplayapi.xmls.rooms.RoomItemData.ItemStateBlock;
import org.asf.edge.gameplayapi.xmls.rooms.RoomItemList;
import org.asf.edge.gameplayapi.xmls.rooms.RoomItemUpdateRequestData;
import org.asf.edge.gameplayapi.xmls.rooms.RoomItemUpdateResponseData;
import org.asf.edge.gameplayapi.xmls.rooms.RoomList;
import org.asf.edge.gameplayapi.xmls.rooms.RoomListEntryData;
import org.asf.edge.gameplayapi.xmls.rooms.RoomListRequestData;
import org.asf.edge.gameplayapi.xmls.rooms.RoomUpdateResponseData;
import org.asf.edge.gameplayapi.xmls.rooms.SetItemStateRequestData;
import org.asf.edge.gameplayapi.xmls.rooms.SetItemStateResponseData;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.function.Predicate;

public class ContentWebServiceV1Processor extends EdgeWebService<EdgeGameplayApiServer> {

	private static AccountManager manager;
	private static PlayerRoomManager roomManager;
	private static ItemManager itemManager;
	private static QuestManager questManager;

	public ContentWebServiceV1Processor(EdgeGameplayApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new ContentWebServiceV1Processor(getServerInstance());
	}

	@Override
	public String path() {
		return "/ContentWebService.asmx";
	}

	@Override
	public void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		// Handle request
		path = path;
		if (path.equals(""))
			setResponseStatus(200, "OK");
		else
			setResponseStatus(404, "Not found");
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getDefaultNameSuggestion(LegacyFunctionInfo func) throws IOException {
		// Handle default suggestions request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;

		// Select default names
		DisplayNameUniqueResponseData resp = new DisplayNameUniqueResponseData();
		resp.suggestions = new DisplayNameUniqueResponseData.SuggestionResultBlock();
		resp.suggestions.suggestions = new String[0]; // TODO

		// Set response
		setResponseContent("text/xml", req.generateXmlValue("DisplayNameUniqueResponse", resp));
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getAuthoritativeTime(LegacyFunctionInfo func) throws IOException {
		// Handle time request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;

		// Set response
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		setResponseContent("text/xml",
				req.generateXmlValue("dateTime", fmt.format(new Date(System.currentTimeMillis()))));
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void purchaseItems(LegacyFunctionInfo func) throws IOException {
		// Handle quest data request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}
		if (!tkn.hasCapability("gp")) {
			// Oh frack COME ON
			// Well lets select the first save

			// Check saves
			String[] saves = account.getSaveIDs();
			if (saves.length == 0) {
				// Error
				setResponseStatus(404, "Not found");
				return;
			}

			// Set ID
			tkn.saveID = account.getSaveIDs()[0];
		}

		// Retrieve container info
		AccountSaveContainer save = account.getSave(tkn.saveID);

		// Parse request
		int storeID = Integer.parseInt(req.payload.get("storeId"));
		int containerID = Integer.parseInt(req.payload.getOrDefault("ContainerID", "1"));
		int[] itemIds = req.parseXmlValue(req.payload.get("itemIDArrayXml"), int[].class);

		// Build request objects
		HashMap<Integer, ItemRedemptionInfo> items = new HashMap<Integer, ItemRedemptionInfo>();
		for (int id : itemIds) {
			if (!items.containsKey(id)) {
				ItemRedemptionInfo itm = new ItemRedemptionInfo();
				itm.containerID = containerID;
				itm.defID = id;
				items.put(id, itm);
			}
			items.get(id).quantity++;
		}

		// Run request
		InventoryUpdateResponseData response = InventoryUtils.purchaseItems(storeID,
				items.values().toArray(t -> new ItemRedemptionInfo[t]), account, save, true);

		// Swap ID if needed
		if (!tkn.hasCapability("gp")) {
			if (response.currencyUpdate != null)
				response.currencyUpdate.userID = account.getAccountID();
		}

		// Set response
		setResponseContent("text/xml", req.generateXmlValue("CIRS", response));
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getUserGameCurrency(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();
		if (questManager == null)
			questManager = QuestManager.getInstance();

		// Handle quest data request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Parse request
		String userID = req.payload.get("userId");
		AccountSaveContainer save = account.getSave(userID);
		if (save == null) {
			setResponseStatus(403, "Forbidden");
			return;
		}

		// Load currency
		CurrencyUpdateBlock c = new CurrencyUpdateBlock();
		AccountDataContainer currency = save.getSaveData().getChildContainer("currency");
		int currentC = 300;
		if (currency.entryExists("coins"))
			currentC = currency.getEntry("coins").getAsInt();
		AccountDataContainer currencyAccWide = save.getAccount().getAccountData().getChildContainer("currency");
		int currentG = 0;
		if (currencyAccWide.entryExists("gems"))
			currentG = currencyAccWide.getEntry("gems").getAsInt();
		c.coinCount = currentC;
		c.gemCount = currentG;

		// Set response
		c.userID = save.getSaveID();
		setResponseContent("text/xml", req.generateXmlValue("UserGameCurrency", c));
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void acceptMission(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();
		if (questManager == null)
			questManager = QuestManager.getInstance();

		// Handle quest data request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Parse request
		String userID = req.payload.get("userId");

		// Retrieve container
		AccountSaveContainer save = account.getSave(userID);
		if (save != null) {
			// Load fields
			int missionId = Integer.parseInt(req.payload.get("missionId"));

			// Find mission
			UserQuestInfo quest = questManager.getUserQuest(save, missionId);
			if (quest == null) {
				setResponseContent("text/xml", req.generateXmlValue("boolean", false));
				return;
			}

			// Update
			quest.acceptQuest();
			setResponseContent("text/xml", req.generateXmlValue("boolean", true));
		} else {
			setResponseContent("text/xml", req.generateXmlValue("boolean", false));
		}
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getCommonInventory(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Handle inventory request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Retrieve container info
		AccountDataContainer data = account.getAccountData();
		int containerID = Integer.parseInt(req.payload.get("ContainerId"));

		// Retrieve container
		CommonInventoryData resp = new CommonInventoryData();
		resp.userID = account.getAccountID();

		// Find items
		ArrayList<InventoryItemEntryData> items = new ArrayList<InventoryItemEntryData>();
		for (PlayerInventoryItem itm : itemManager.getCommonInventory(data).getContainer(containerID).getItems()) {
			// Add item
			InventoryItemEntryData block = new InventoryItemEntryData();
			block.itemID = itm.getItemDefID();
			block.quantity = itm.getQuantity();
			block.uses = itm.getUses();
			block.uniqueItemID = itm.getUniqueID();
			// TODO: stats and attributes

			// Add data info from item manager
			ItemInfo def = ItemManager.getInstance().getItemDefinition(block.itemID);
			if (def != null)
				block.data = def.getRawObject();
			items.add(block);
		}
		if (tkn.saveID != null) {
			// Pull account-wide inventory, for compatibility with legacy versions
			data = account.getSave(tkn.saveID).getSaveData();

			// Retrieve container
			resp = new CommonInventoryData();
			resp.userID = account.getAccountID(); // FIXME: may break

			// Find items
			for (PlayerInventoryItem itm : itemManager.getCommonInventory(data).getContainer(containerID).getItems()) {
				// Add item
				InventoryItemEntryData block = new InventoryItemEntryData();
				block.itemID = itm.getItemDefID();
				block.quantity = itm.getQuantity();
				block.uses = itm.getUses();
				block.uniqueItemID = itm.getUniqueID();
				// TODO: stats and attributes

				// Add data info from item manager
				ItemInfo def = ItemManager.getInstance().getItemDefinition(block.itemID);
				if (def != null)
					block.data = def.getRawObject();
				items.add(block);
			}
		}
		resp.items = items.toArray(t -> new InventoryItemEntryData[t]);

		// Set response
		setResponseContent("text/xml", req.generateXmlValue("CI", resp));
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void setCommonInventory(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Handle inventory request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Parse request
		SetCommonInventoryRequestData[] requests = req.parseXmlValue(req.payload.get("commonInventoryRequestXml"),
				SetCommonInventoryRequestData[].class);

		// Retrieve container info
		AccountDataContainer data = account.getAccountData();
		if (tkn.saveID != null)
			data = account.getSave(tkn.saveID).getSaveData();

		// Set
		setResponseContent("text/xml", req.generateXmlValue("CIRS", InventoryUtils.processCommonInventorySet(requests,
				data, Integer.parseInt(req.payload.get("ContainerId")))));
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getKeyValuePair(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Handle key/value request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Parse request
		int pair = Integer.parseInt(req.payload.get("pairId"));

		// Retrieve container info
		AccountDataContainer data = account.getAccountData();
		if (tkn.saveID != null)
			data = account.getSave(tkn.saveID).getSaveData();
		data = data.getChildContainer("keyvaluedata");

		// Set result
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		if (data.entryExists("pairs-" + pair)) {
			// Get data
			int i = 0;
			JsonObject pairs = data.getEntry("pairs-" + pair).getAsJsonObject();
			KeyValuePairSetData setData = new KeyValuePairSetData();
			setData.items = new KeyValuePairData[pairs.size()];
			for (String key : pairs.keySet()) {
				JsonObject obj = pairs.get(key).getAsJsonObject();
				KeyValuePairData p = new KeyValuePairData();
				p.key = key;
				p.value = obj.get("value").getAsString();
				p.updateDate = fmt.format(new Date(obj.get("time").getAsLong()));
				setData.items[i++] = p;
			}

			// Check
			if (pair == 2017) {
				if (!Stream.of(setData.items).anyMatch(t -> t.key.equals("HubBerkDOFirstCommon"))) {
					JsonObject o = new JsonObject();
					o.addProperty("value", "1");
					o.addProperty("time", System.currentTimeMillis());
					pairs.add("HubBerkDOFirstCommon", o);
					data.setEntry("pairs-" + pair, pairs);
					KeyValuePairData d = new KeyValuePairData();
					d.key = "HubBerkDOFirstCommon";
					d.value = "1";
					d.updateDate = fmt.format(new Date(o.get("time").getAsLong()));
					data.setEntry("pairs-" + pair, pairs);
					setData.items = appendTo(setData.items, d);
				}
			}

			// Set response
			if (setData.items.length != 0)
				setResponseContent("text/xml", req.generateXmlValue("Pairs", setData));
			else
				setResponseContent("text/xml", req.generateXmlValue("Pairs", new EmptyKeyValuePairSetData()));
		} else {
			// Check
			if (pair == 2017) {
				KeyValuePairSetData setData = new KeyValuePairSetData();
				setData.items = new KeyValuePairData[1];
				JsonObject pairs = new JsonObject();
				JsonObject o = new JsonObject();
				o.addProperty("value", "1");
				o.addProperty("time", System.currentTimeMillis());
				pairs.add("HubBerkDOFirstCommon", o);
				data.setEntry("pairs-" + pair, pairs);
				KeyValuePairData d = new KeyValuePairData();
				d.key = "HubBerkDOFirstCommon";
				d.value = "1";
				d.updateDate = fmt.format(new Date(o.get("time").getAsLong()));
				setResponseContent("text/xml", req.generateXmlValue("Pairs", setData));
			}

			// Not found
			setResponseContent("text/xml", req.generateXmlValue("Pairs", new EmptyKeyValuePairSetData()));
		}
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getKeyValuePairByUserID(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Handle key/value request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Parse request
		int pair = Integer.parseInt(req.payload.get("pairId"));
		String userID = req.payload.get("userId");

		// Retrieve container info
		boolean isDragon = false;
		AccountDataContainer dragonData = null;
		if (!userID.equals(account.getAccountID()) && account.getSave(userID) == null) {
			// Check save
			if (tkn.hasCapability("gp")) {
				// Find dragon
				AccountDataContainer data = account.getSave(tkn.saveID).getSaveData().getChildContainer("dragons");
				JsonArray dragonIds = new JsonArray();
				if (data.entryExists("dragonlist"))
					dragonIds = data.getEntry("dragonlist").getAsJsonArray();
				else
					data.setEntry("dragonlist", dragonIds);
				for (JsonElement ele : dragonIds) {
					String id = ele.getAsString();
					ObjectNode dragon = req.parseXmlValue(data.getEntry("dragon-" + id).getAsString(),
							ObjectNode.class);

					// Check dragon entity ID
					if (dragon.get("eid").asText().equals(userID)) {
						// Its a dragon
						isDragon = true;
						dragonData = data.getChildContainer("dragondata-" + userID);
						break;
					}
				}
			}
		}
		if (isDragon || userID.equals(account.getAccountID()) || account.getSave(userID) != null) {
			AccountDataContainer data = dragonData;
			if (data == null) {
				data = account.getAccountData();
				if (!userID.equals(account.getAccountID()))
					data = account.getSave(userID).getSaveData();
			}
			data = data.getChildContainer("keyvaluedata");

			// Set result
			SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
			fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
			if (data.entryExists("pairs-" + pair)) {
				// Get data
				int i = 0;
				JsonObject pairs = data.getEntry("pairs-" + pair).getAsJsonObject();
				KeyValuePairSetData setData = new KeyValuePairSetData();
				setData.items = new KeyValuePairData[pairs.size()];
				for (String key : pairs.keySet()) {
					JsonObject obj = pairs.get(key).getAsJsonObject();
					KeyValuePairData p = new KeyValuePairData();
					p.key = key;
					p.value = obj.get("value").getAsString();
					p.updateDate = fmt.format(new Date(obj.get("time").getAsLong()));
					setData.items[i++] = p;
				}

				// Check
				if (pair == 2017) {
					if (!Stream.of(setData.items).anyMatch(t -> t.key.equals("HubBerkDOFirstCommon"))) {
						JsonObject o = new JsonObject();
						o.addProperty("value", "1");
						o.addProperty("time", System.currentTimeMillis());
						pairs.add("HubBerkDOFirstCommon", o);
						KeyValuePairData d = new KeyValuePairData();
						d.key = "HubBerkDOFirstCommon";
						d.value = "1";
						d.updateDate = fmt.format(new Date(o.get("time").getAsLong()));
						data.setEntry("pairs-" + pair, pairs);
						setData.items = appendTo(setData.items, d);
					}
				}

				// Set response
				if (setData.items.length != 0)
					setResponseContent("text/xml", req.generateXmlValue("Pairs", setData));
				else
					setResponseContent("text/xml", req.generateXmlValue("Pairs", new EmptyKeyValuePairSetData()));
			} else {
				// Check
				if (pair == 2017) {
					KeyValuePairSetData setData = new KeyValuePairSetData();
					setData.items = new KeyValuePairData[1];
					JsonObject pairs = new JsonObject();
					JsonObject o = new JsonObject();
					o.addProperty("value", "1");
					o.addProperty("time", System.currentTimeMillis());
					pairs.add("HubBerkDOFirstCommon", o);
					data.setEntry("pairs-" + pair, pairs);
					KeyValuePairData d = new KeyValuePairData();
					d.key = "HubBerkDOFirstCommon";
					d.value = "1";
					d.updateDate = fmt.format(new Date(o.get("time").getAsLong()));
					setResponseContent("text/xml", req.generateXmlValue("Pairs", setData));
				}

				// Not found
				setResponseContent("text/xml", req.generateXmlValue("Pairs", new EmptyKeyValuePairSetData()));
			}
		} else {
			setResponseStatus(403, "Forbidden");
		}
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void setKeyValuePair(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Handle key/value request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Parse request
		KeyValuePairSetData updateData = req.parseXmlValue(req.payload.get("contentXML"), KeyValuePairSetData.class);
		int pair = Integer.parseInt(req.payload.get("pairId"));

		// Retrieve container info
		AccountDataContainer data = account.getAccountData();
		if (tkn.saveID != null)
			data = account.getSave(tkn.saveID).getSaveData();
		data = data.getChildContainer("keyvaluedata");

		// Set entries
		JsonObject pairData = new JsonObject();
		if (data.entryExists("pairs-" + pair))
			pairData = data.getEntry("pairs-" + pair).getAsJsonObject();
		if (updateData != null && updateData.items != null) {
			for (KeyValuePairData pairI : updateData.items) {
				// Set entry
				JsonObject p = new JsonObject();
				p.addProperty("value", pairI.value);
				p.addProperty("time", System.currentTimeMillis());
				pairData.add(pairI.key, p);
			}
			data.setEntry("pairs-" + pair, pairData);
		}

		// Set response
		setResponseContent("text/xml", req.generateXmlValue("boolean", true));
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void setKeyValuePairByUserID(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Handle key/value request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS || !tkn.hasCapability("gp")) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Parse request
		KeyValuePairSetData updateData = req.parseXmlValue(req.payload.get("contentXML"), KeyValuePairSetData.class);
		int pair = Integer.parseInt(req.payload.get("pairId"));
		String userID = req.payload.get("userId");

		// Retrieve container info
		boolean isDragon = false;
		AccountDataContainer dragonData = null;
		if (!userID.equals(account.getAccountID()) && account.getSave(userID) == null) {
			// Check save
			if (tkn.hasCapability("gp")) {
				// Find dragon
				AccountDataContainer data = account.getSave(tkn.saveID).getSaveData().getChildContainer("dragons");
				JsonArray dragonIds = new JsonArray();
				if (data.entryExists("dragonlist"))
					dragonIds = data.getEntry("dragonlist").getAsJsonArray();
				else
					data.setEntry("dragonlist", dragonIds);
				for (JsonElement ele : dragonIds) {
					String id = ele.getAsString();
					ObjectNode dragon = req.parseXmlValue(data.getEntry("dragon-" + id).getAsString(),
							ObjectNode.class);

					// Check dragon entity ID
					if (dragon.get("eid").asText().equals(userID)) {
						// Its a dragon
						isDragon = true;
						dragonData = data.getChildContainer("dragondata-" + userID);
						break;
					}
				}
			}
		}
		if (isDragon || userID.equals(account.getAccountID()) || account.getSave(userID) != null) {
			AccountDataContainer data = dragonData;
			if (data == null) {
				data = account.getAccountData();
				if (!userID.equals(account.getAccountID()))
					data = account.getSave(userID).getSaveData();
			}
			data = data.getChildContainer("keyvaluedata");

			// Set entries
			JsonObject pairData = new JsonObject();
			if (data.entryExists("pairs-" + pair))
				pairData = data.getEntry("pairs-" + pair).getAsJsonObject();
			for (KeyValuePairData pairI : updateData.items) {
				// Set entry
				JsonObject p = new JsonObject();
				p.addProperty("value", pairI.value);
				p.addProperty("time", System.currentTimeMillis());
				pairData.add(pairI.key, p);
			}
			data.setEntry("pairs-" + pair, pairData);

			// Set response
			setResponseContent("text/xml", req.generateXmlValue("boolean", true));
		} else {
			setResponseStatus(403, "Forbidden");
		}
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void delKeyValuePair(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Handle key/value request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Parse request
		int pair = Integer.parseInt(req.payload.get("pairId"));

		// Retrieve container info
		AccountDataContainer data = account.getAccountData();
		if (tkn.saveID != null)
			data = account.getSave(tkn.saveID).getSaveData();
		data = data.getChildContainer("keyvaluedata");
		if (data.entryExists("pairs-" + pair))
			data.deleteEntry("pairs-" + pair);

		// Set response
		setResponseContent("text/xml", req.generateXmlValue("boolean", true));
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void delKeyValuePairByUserID(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Handle key/value request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS || !tkn.hasCapability("gp")) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Parse request
		int pair = Integer.parseInt(req.payload.get("pairId"));
		String userID = req.payload.get("userId");

		// Retrieve container info
		if (userID.equals(account.getAccountID()) || account.getSave(userID) != null) {
			AccountDataContainer data = account.getAccountData();
			if (!userID.equals(account.getAccountID()))
				data = account.getSave(userID).getSaveData();
			data = data.getChildContainer("keyvaluedata");
			if (data.entryExists("pairs-" + pair))
				data.deleteEntry("pairs-" + pair);

			// Set response
			setResponseContent("text/xml", req.generateXmlValue("boolean", true));
		} else {
			setResponseStatus(403, "Forbidden");
		}
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void delKeyValuePairByKey(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Handle key/value request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS || !tkn.hasCapability("gp")) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Parse request
		int pair = Integer.parseInt(req.payload.get("pairId"));
		String userID = req.payload.get("userId");

		// Retrieve container info
		if (userID.equals(account.getAccountID()) || account.getSave(userID) != null) {
			AccountDataContainer data = account.getAccountData();
			if (!userID.equals(account.getAccountID()))
				data = account.getSave(userID).getSaveData();
			data = data.getChildContainer("keyvaluedata");
			if (data.entryExists("pairs-" + pair)) {
				JsonObject pairData = data.getEntry("pairs-" + pair).getAsJsonObject();
				if (pairData.has(req.payload.get("pairKey"))) {
					pairData.remove(req.payload.get("pairKey"));
					data.setEntry("pairs-" + pair, pairData);
				}
			}

			// Set response
			setResponseContent("text/xml", req.generateXmlValue("boolean", true));
		} else {
			setResponseStatus(403, "Forbidden");
		}
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getSelectedRaisedPet(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Handle dragon data request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Parse request
		String userID = req.payload.get("userId");

		// Retrieve container
		if (userID.equals(account.getAccountID()) || account.getSave(userID) != null) {
			AccountDataContainer data = account.getAccountData();
			if (!userID.equals(account.getAccountID()))
				data = account.getSave(userID).getSaveData();

			// Pull dragons
			data = data.getChildContainer("dragons");
			JsonArray dragonIds = new JsonArray();
			if (data.entryExists("dragonlist"))
				dragonIds = data.getEntry("dragonlist").getAsJsonArray();
			else
				data.setEntry("dragonlist", dragonIds);

			// Prepare response
			ArrayList<DragonData> dragons = new ArrayList<DragonData>();

			// Populate list
			for (JsonElement ele : dragonIds) {
				String id = ele.getAsString();
				DragonData dragon = req.parseXmlValue(data.getEntry("dragon-" + id).getAsString(), DragonData.class);

				// Check filter
				if (TextFilterService.getInstance().isFiltered(dragon.name, true)) {
					// Filtered

					// Reset name
					dragon.name = TextFilterService.getInstance().filterString(dragon.name,
							account.isStrictChatFilterEnabled());

					// Force rename
					ArrayList<ObjectNode> attrs = new ArrayList<ObjectNode>(List.of(dragon.attributes));
					Optional<ObjectNode> att = attrs.stream()
							.filter(t -> t.get("k").asText().equalsIgnoreCase("NameCustomized")).findFirst();
					if (att.isPresent())
						attrs.remove(att.get());
					dragon.attributes = attrs.toArray(t -> new ObjectNode[t]);

					// Save
					data.setEntry("dragon-" + id, new JsonPrimitive(req.generateXmlValue("RaisedPetData", dragon)));
				}

				// Check if active
				if (dragon.isSelected) {
					// Add selected
					dragons.add(dragon);
				}
			}

			// Set response
			if (dragons.size() != 0) {
				DragonListData ls = new DragonListData();
				ls.dragons = dragons.toArray(t -> new DragonData[t]);
				setResponseContent("text/xml", req.generateXmlValue("ArrayOfRaisedPetData", ls));
			} else
				setResponseContent("text/xml", req.generateXmlValue("ArrayOfRaisedPetData", null));
		} else {
			// ???
			setResponseStatus(403, "Forbidden, attempted to interact with other user's data (devs please inspect)");
		}
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void setSelectedPet(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Handle dragon data request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS || !tkn.hasCapability("gp")) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Parse request
		String dragonID = req.payload.get("raisedPetID");

		// Retrieve container
		AccountDataContainer data = account.getSave(tkn.saveID).getSaveData();

		// Pull dragons
		data = data.getChildContainer("dragons");
		JsonArray dragonIds = new JsonArray();
		if (data.entryExists("dragonlist"))
			dragonIds = data.getEntry("dragonlist").getAsJsonArray();
		else
			data.setEntry("dragonlist", dragonIds);

		// Select dragon and deselect old
		boolean found = false;
		for (JsonElement ele : dragonIds) {
			String id = ele.getAsString();
			ObjectNode dragon = req.parseXmlValue(data.getEntry("dragon-" + id).getAsString(), ObjectNode.class);

			// Check if active
			if (id.equals(dragonID) || dragon.get("is").asBoolean()) {
				// Select/deselect
				if (id.equals(dragonID)) {
					dragon.set("is", BooleanNode.TRUE);
					found = true;
				} else
					dragon.set("is", BooleanNode.FALSE);

				// Save
				data.setEntry("dragon-" + id, new JsonPrimitive(req.generateXmlValue("RaisedPetData", dragon)));
			}
		}

		// Set response
		setResponseContent("text/xml", req.generateXmlValue("boolean", found));
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getUnselectedPetByTypes(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Handle dragon data request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Parse request
		String userID = req.payload.get("userId");
		String[] types = req.payload.get("petTypeIDs").split(",");

		// Retrieve container
		if (userID.equals(account.getAccountID()) || account.getSave(userID) != null) {
			AccountDataContainer data = account.getAccountData();
			if (!userID.equals(account.getAccountID()))
				data = account.getSave(userID).getSaveData();

			// Pull dragons
			data = data.getChildContainer("dragons");
			JsonArray dragonIds = new JsonArray();
			if (data.entryExists("dragonlist"))
				dragonIds = data.getEntry("dragonlist").getAsJsonArray();
			else
				data.setEntry("dragonlist", dragonIds);

			// Prepare response
			ArrayList<DragonData> dragons = new ArrayList<DragonData>();

			// Populate list
			for (JsonElement ele : dragonIds) {
				// Load dragon
				String id = ele.getAsString();
				DragonData dragon = req.parseXmlValue(data.getEntry("dragon-" + id).getAsString(), DragonData.class);

				// Check filter
				if (TextFilterService.getInstance().isFiltered(dragon.name, true)) {
					// Filtered

					// Reset name
					dragon.name = TextFilterService.getInstance().filterString(dragon.name,
							account.isStrictChatFilterEnabled());

					// Force rename
					ArrayList<ObjectNode> attrs = new ArrayList<ObjectNode>(List.of(dragon.attributes));
					Optional<ObjectNode> att = attrs.stream()
							.filter(t -> t.get("k").asText().equalsIgnoreCase("NameCustomized")).findFirst();
					if (att.isPresent())
						attrs.remove(att.get());
					dragon.attributes = attrs.toArray(t -> new ObjectNode[t]);

					// Save
					data.setEntry("dragon-" + id, new JsonPrimitive(req.generateXmlValue("RaisedPetData", dragon)));
				}

				// Add if needed
				if (types.length == 0 || Stream.of(types).anyMatch(t -> t.equals(dragon.typeID)))
					dragons.add(dragon);
			}

			// Set response
			if (dragons.size() != 0) {
				DragonListData ls = new DragonListData();
				ls.dragons = dragons.toArray(t -> new DragonData[t]);
				setResponseContent("text/xml", req.generateXmlValue("ArrayOfRaisedPetData", ls));
			} else
				setResponseContent("text/xml", req.generateXmlValue("ArrayOfRaisedPetData", null));
		} else {
			setResponseStatus(403, "Forbidden");
		}
	}

	@LegacyFunction(allowedMethods = { "POST", "GET" })
	public void getImage(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Check query
		if (func.getQuery().containsKey("edgereq") && func.getQuery().get("edgereq").equals("true")) {
			// Load info
			String slot = func.getQuery().get("slot");
			String type = func.getQuery().get("type");
			String account = func.getQuery().get("account");
			String save = func.getQuery().get("save");

			// Locate account
			AccountObject acc = manager.getAccount(account);
			if (acc == null || acc.getSave(save) == null) {
				setResponseStatus(404, "Not found");
				return;
			}

			// Locate save data
			AccountDataContainer data = acc.getSave(save).getSaveData().getChildContainer("images");
			if (!data.entryExists("imagefile-" + slot + "-" + type)) {
				setResponseStatus(404, "Not found");
				return;
			}

			// Set
			getResponse().setContent("image/jpeg",
					Base64.getDecoder().decode(data.getEntry("imagefile-" + slot + "-" + type).getAsString()));
			return;
		}

		// Handle set image request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS || !tkn.hasCapability("gp")) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Parse
		String slot = req.payload.get("ImageSlot");
		String type = req.payload.get("ImageType");

		// Retrieve data
		AccountDataContainer data = account.getSave(tkn.saveID).getSaveData().getChildContainer("images");

		// Not found
		if (!data.entryExists("imageslotinfo-" + slot + "-" + type)) {
			setResponseContent("text/xml", req.generateXmlValue("ImageData", null));
			return;
		}

		// Set response
		ObjectNode imgD = req.parseXmlValue(data.getEntry("imageslotinfo-" + slot + "-" + type).getAsString(),
				ObjectNode.class);
		if (imgD.has("ImageURL")) {
			// Make relative to host
			URL u = new URL(imgD.get("ImageURL").asText());
			String prot = "http://";
			if (getServerInstance().getServer() instanceof TlsSecuredHttpServer)
				prot = "https://";
			imgD.put("ImageURL", prot + getHeader("Host") + "/" + u.getFile());
		}
		setResponseContent("text/xml", req.generateXmlValue("ImageData", imgD));
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void setImage(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Handle set image request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS || !tkn.hasCapability("gp")) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Parse
		String xml = req.payload.get("contentXML");
		String slot = req.payload.get("ImageSlot");
		String type = req.payload.get("ImageType");

		// Retrieve data
		AccountDataContainer data = account.getSave(tkn.saveID).getSaveData().getChildContainer("images");

		// Set data
		ObjectNode imgD = req.parseXmlValue(xml, ObjectNode.class);
		String prot = "http://";
		if (getServerInstance().getServer() instanceof TlsSecuredHttpServer)
			prot = "https://";
		imgD.set("ImageURL",
				new TextNode(prot + getHeader("Host") + "/"
						+ (getRequest().getRequestPath().substring(0,
								getRequest().getRequestPath().toLowerCase().lastIndexOf("setimage"))
								+ "GetImage?edgereq=true&slot=" + URLEncoder.encode(slot, "UTF-8") + "&account="
								+ URLEncoder.encode(account.getAccountID(), "UTF-8") + "&save="
								+ URLEncoder.encode(tkn.saveID, "UTF-8") + "&type=" + URLEncoder.encode(type, "UTF-8")
								+ "&file=image.jpg")));

		// Set image
		data.setEntry("imageslotinfo-" + slot + "-" + type, new JsonPrimitive(req.generateXmlValue("ImageData", imgD)));
		data.setEntry("imagefile-" + slot + "-" + type, new JsonPrimitive(req.payload.get("imageFile")));

		// Set response
		setResponseContent("text/xml", req.generateXmlValue("boolean", true));
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getUserMissionState(LegacyFunctionInfo func) throws IOException {
		// 1.x questing
		if (manager == null)
			manager = AccountManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();
		if (questManager == null)
			questManager = QuestManager.getInstance();

		// Handle quest data request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}

		// Parse request
		String userID = req.payload.get("userId");

		// Retrieve container
		AccountSaveContainer save = account.getSave(userID);
		if (save != null) {
			// Pull quests
			MissionData[] quests = questManager.getAllQuestDefs();

			// Parse filters
			RequestFilterDataLegacy filter = req.parseXmlValue(req.payload.get("filter"),
					RequestFilterDataLegacy.class);

			// Create response
			ArrayList<Integer> addedQuests = new ArrayList<Integer>();
			ArrayList<MissionData> questLst = new ArrayList<MissionData>();
			QuestListResponseData resp = new QuestListResponseData();
			resp.userID = userID;

			// Apply ID filters
			boolean hasExplicitRequests = false;
			if (filter.missionID != -1) {
				hasExplicitRequests = true;

				// Find quest
				UserQuestInfo quest = questManager.getUserQuest(save, filter.missionID);
				if (quest != null && (filter.getCompletedMissions || !quest.isCompleted())) {
					questLst.add(quest.getData());
					addedQuests.add(quest.getQuestID());
				}
			}

			// Apply group ID versions
			if (filter.groupID != -1) {
				hasExplicitRequests = true;
				for (MissionData data : quests) {
					// Check ID
					if (addedQuests.contains(data.id))
						continue;

					// Check group
					if (data.groupID == filter.groupID) {
						// Found a quest
						UserQuestInfo quest = questManager.getUserQuest(save, data.id);
						if (quest != null && (filter.getCompletedMissions || !quest.isCompleted())) {
							// Add
							questLst.add(quest.getData());
							addedQuests.add(quest.getQuestID());
						}
					}
				}
			}

			// Add all quests with other filter if not explicit
			if (!hasExplicitRequests) {
				// Add missions
				if (filter.getCompletedMissions) {
					for (UserQuestInfo quest : questManager.getCompletedQuests(save)) {
						// Check ID
						if (addedQuests.contains(quest.getQuestID()))
							continue;

						// Check quest
						MissionData questDef = quest.getDef();
						if (questDef.missionRules != null && questDef.missionRules.prerequisites != null
								&& Stream.of(questDef.missionRules.prerequisites)
										.anyMatch(new Predicate<PrerequisiteInfoBlock>() {

											@Override
											public boolean test(PrerequisiteInfoBlock t) {
												// Check type
												if (t.type >= 7)
													return true;
												else if (t.type == PrerequisiteRuleTypes.MISSION) {
													// Get quest
													MissionData def = questManager
															.getQuestDef(Integer.parseInt(t.value));
													if (def == null || Stream.of(def.missionRules.prerequisites)
															.anyMatch(this)) {
														// Incompatible
														return true;
													}
												}

												// Compatible
												return false;
											}

										})) {
							// Cannot use this quest in 1.x
							continue;
						}

						// Add
						MissionData d = quest.getData();
						d.accepted = true;
						questLst.add(d);
						addedQuests.add(quest.getQuestID());
					}

					// Add fishing quest
					if (!addedQuests.contains(1000)) {
						MissionData fishing = questManager.getQuestDef(999).copy();
						fishing.id = 1000;
						fishing.completed = 1;
						fishing.version = 1;
						fishing.acceptanceAchievementID = -1;
						fishing.achievementID = -1;
						fishing.accepted = true;
						questLst.add(fishing);
					}
				} else {
					// Add quests
					for (UserQuestInfo quest : questManager.getActiveQuests(save)) {
						// Check ID
						if (addedQuests.contains(quest.getQuestID()))
							continue;

						// Check quest
						MissionData questDef = quest.getDef();
						if (questDef.missionRules != null && questDef.missionRules.prerequisites != null
								&& Stream.of(questDef.missionRules.prerequisites)
										.anyMatch(new Predicate<PrerequisiteInfoBlock>() {

											@Override
											public boolean test(PrerequisiteInfoBlock t) {
												// Check type
												if (t.type >= 7)
													return true;
												else if (t.type == PrerequisiteRuleTypes.MISSION) {
													// Get quest
													MissionData def = questManager
															.getQuestDef(Integer.parseInt(t.value));
													if (def == null || Stream.of(def.missionRules.prerequisites)
															.anyMatch(this)) {
														// Incompatible
														return true;
													}
												}

												// Compatible
												return false;
											}

										})) {
							// Cannot use this quest in 1.x
							continue;
						}

						// Add
						MissionData d = quest.getData();
						d.accepted = quest.isStarted();
						questLst.add(d);
						addedQuests.add(quest.getQuestID());
					}

					// Add quests
					for (UserQuestInfo quest : questManager.getUpcomingQuests(save)) {
						// Check ID
						if (addedQuests.contains(quest.getQuestID()))
							continue;

						// Check quest
						MissionData questDef = quest.getDef();
						if (questDef.missionRules != null && questDef.missionRules.prerequisites != null
								&& Stream.of(questDef.missionRules.prerequisites)
										.anyMatch(new Predicate<PrerequisiteInfoBlock>() {

											@Override
											public boolean test(PrerequisiteInfoBlock t) {
												// Check type
												if (t.type >= 7)
													return true;
												else if (t.type == PrerequisiteRuleTypes.MISSION) {
													// Get quest
													MissionData def = questManager
															.getQuestDef(Integer.parseInt(t.value));
													if (def == null || Stream.of(def.missionRules.prerequisites)
															.anyMatch(this)) {
														// Incompatible
														return true;
													}
												}

												// Compatible
												return false;
											}

										})) {
							// Cannot use this quest in 1.x
							continue;
						}

						// Add
						MissionData d = quest.getData();
						d.accepted = false;
						questLst.add(d);
						addedQuests.add(quest.getQuestID());
					}

					// Add fishing quest
					if (!addedQuests.contains(1000)) {
						MissionData fishing = questManager.getQuestDef(999).copy();
						fishing.id = 1000;
						fishing.completed = 1;
						fishing.version = 1;
						fishing.acceptanceAchievementID = -1;
						fishing.achievementID = -1;
						fishing.accepted = true;
						questLst.add(fishing);
					}

					// Add tutorial quest
					if (!addedQuests.contains(999)) {
						questLst.add(questManager.getUserQuest(save, 999).getData());
					}
				}
			}

			// Set response
			resp.quests = questLst.toArray(t -> new MissionData[t]);
			setResponseContent("text/xml", req.generateXmlValue("UserMissionStateResult", resp));
		} else {
			setResponseStatus(404, "Not found");
		}
	}

	@SodRequest
	@SodTokenSecured
	@Function(allowedMethods = { "POST" })
	public FunctionResult setTaskState(FunctionInfo func, ServiceRequestInfo req, SessionToken tkn,
			AccountObject account, @SodRequestParam String userId, @SodRequestParam int missionId,
			@SodRequestParam int taskId, @SodRequestParam boolean completed, @SodRequestParam String xmlPayload)
			throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();
		if (questManager == null)
			questManager = QuestManager.getInstance();

		// Retrieve container
		AccountSaveContainer save = account.getSave(userId);
		if (save != null) {
			// Load fields
			int invContainer = Integer.parseInt(req.payload.getOrDefault("ContainerId", "1"));

			// Find quest
			UserQuestInfo quest = questManager.getUserQuest(save, missionId);
			if (quest != null) {
				return ok("text/xml", req.generateXmlValue("SetTaskStateResult", quest.handleTaskCall(taskId,
						xmlPayload, completed, invContainer, new SetCommonInventoryRequestData[0])));
			} else {
				// Not found
				SetTaskStateResultData resp = new SetTaskStateResultData();
				resp.success = false;
				resp.status = SetTaskStateResultData.SetTaskStateResultStatuses.MISSION_STATE_NOT_FOUND;
				return ok("text/xml", req.generateXmlValue("SetTaskStateResult", resp));
			}
		} else {
			return response(404, "Not found");
		}
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void redeemMysteryBoxItems(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Handle box redemption
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;
		String apiToken = getUtilities().decodeToken(req.payload.get("apiToken").toUpperCase());

		// Read token
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(apiToken);
		AccountObject account = tkn.account;
		if (res != TokenParseResult.SUCCESS || !tkn.hasCapability("gp")) {
			// Error
			setResponseStatus(404, "Not found");
			return;
		}
		AccountSaveContainer save = account.getSave(tkn.saveID);

		// Parse payload
		ItemRedeemRequestData request = req.parseXmlValue(req.payload.get("request"), ItemRedeemRequestData.class);
		int id = request.itemID;

		// Check item
		int containerId = Integer.parseInt(req.payload.getOrDefault("ContainerId", "1"));

		// Pull inventory container
		PlayerInventoryContainer cont = save.getInventory().getContainer(containerId);

		// Check item
		PlayerInventoryItem itm = cont.findFirst(id);
		if (itm == null || itm.getQuantity() < 1) {
			// Error
			InventoryUpdateResponseData resp = new InventoryUpdateResponseData();
			resp.success = false;
			setResponseContent("text/xml", req.generateXmlValue("CIRS", resp));
			return;
		}

		// Remove item quantity
		itm.remove(1);

		// Prepare chest request
		ItemRedemptionInfo rReq = new ItemRedemptionInfo();
		rReq.containerID = containerId;
		rReq.defID = id;
		rReq.quantity = 1;

		// Run request
		InventoryUpdateResponseData response = InventoryUtils.redeemItems(new ItemRedemptionInfo[] { rReq }, account,
				save, true);

		// Check quantity
		if (itm.getQuantity() > 0) {
			// Create array if needed
			if (response.updateItems == null)
				response.updateItems = new ItemUpdateBlock[0];

			// Add to response
			ItemUpdateBlock b = new ItemUpdateBlock();
			b.itemID = itm.getItemDefID();
			b.itemUniqueID = itm.getUniqueID();
			b.quantity = -1;
			response.updateItems = appendTo(response.updateItems, b);
		}

		// Set response
		setResponseContent("text/xml", req.generateXmlValue("CIRS", response));
	}

	@SodRequest
	public FunctionResult getDisplayNameByUserID(FunctionInfo func, ServiceRequestInfo req,
			@SodRequestParam String userId) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();

		// Find save
		AccountSaveContainer save = manager.getSaveByID(userId);

		// Get messenger
		if (save == null)
			return response(404, "Not found");

		// Return
		return ok("text/xml", req.generateXmlValue("string", save.getUsername()));
	}

	@SodRequest
	@SodTokenSecured
	@TokenRequireSave
	@TokenRequireCapability("gp")
	public FunctionResult useInventory(FunctionInfo func, ServiceRequestInfo req, SessionToken tkn,
			AccountObject account, @SodRequestParam int userInventoryId, @SodRequestParam int numberOfUses)
			throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Retrieve container info
		AccountDataContainer data = account.getAccountData();
		if (tkn.saveID != null)
			data = account.getSave(tkn.saveID).getSaveData();

		// Parse request
		int containerId = Integer.parseInt(req.payload.getOrDefault("ContainerId", "1"));

		// Find item
		PlayerInventory inv = itemManager.getCommonInventory(data);
		PlayerInventoryContainer cont = inv.getContainer(containerId);
		PlayerInventoryItem itm = cont.getItem(userInventoryId);
		if (itm == null) {
			return ok("text/xml", req.generateXmlValue("boolean", false));
		}

		// Check uses
		int usesLeft = itm.getUses();
		if (usesLeft == -1) {
			usesLeft = 1;
		}

		// Check
		if (numberOfUses > usesLeft) {
			// Not enough uses left
			return ok("text/xml", req.generateXmlValue("boolean", false));
		}

		// Remove uses
		if (!itm.useItem(numberOfUses)) {
			// Failed to use item
			return ok("text/xml", req.generateXmlValue("boolean", false));
		}

		// Return
		return ok("text/xml", req.generateXmlValue("boolean", true));
	}

	@SodRequest
	@SodTokenSecured
	public FunctionResult getGameDataByUser(FunctionInfo func, ServiceRequestInfo req, SessionToken tkn,
			AccountObject account, @SodRequestParam String userId) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();

		// Retrieve container
		AccountSaveContainer save = account.getSave(userId);
		if (save == null)
			return response(404, "Not found");

		// Create request
		MinigameDataRequest srq = new MinigameDataRequest();
		srq.gameLevel = Integer.parseInt(req.payload.get("gameLevel"));
		srq.difficulty = Integer.parseInt(req.payload.get("difficulty"));
		srq.friendsOnly = req.payload.get("buddyFilter").equalsIgnoreCase("true");
		srq.maxEntries = Integer.parseInt(req.payload.get("count"));
		srq.key = req.payload.get("key");
		MinigameData data = MinigameDataManager.getInstance().getGameDataOf(save,
				Integer.parseInt(req.payload.get("gameId")), srq);

		// Prepare response
		GameDataSummaryData resp = new GameDataSummaryData();
		resp.gameID = Integer.parseInt(req.payload.get("gameId"));
		resp.difficulty = srq.difficulty;
		resp.isMultiplayer = req.payload.get("isMultiplayer").equalsIgnoreCase("true");
		resp.userPosition = 0;
		resp.key = srq.key;
		resp.entries = new GameDataSummaryData.GameDataBlock[1];
		resp.entries[0] = new GameDataSummaryData.GameDataBlock();
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		resp.entries[0].datePlayed = fmt.format(new Date(data.timePlayed));
		resp.entries[0].rankID = 1;
		resp.entries[0].timesLost = data.timesLost;
		resp.entries[0].timesWon = data.timesWon;
		resp.entries[0].userID = data.userID;
		resp.entries[0].userName = AccountManager.getInstance().getSaveByID(data.userID).getUsername();
		resp.entries[0].value = data.value;

		// Return
		return ok("text/xml", req.generateXmlValue("GameDataSummary", resp));
	}

	@SodRequest
	@SodTokenSecured
	public FunctionResult getGameDataByGame(FunctionInfo func, ServiceRequestInfo req, SessionToken tkn,
			AccountObject account, @SodRequestParam String userId) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();

		// Retrieve container
		AccountSaveContainer save = account.getSave(userId);
		if (save == null)
			return response(404, "Not found");

		// Create request
		MinigameDataRequest srq = new MinigameDataRequest();
		srq.gameLevel = Integer.parseInt(req.payload.get("gameLevel"));
		srq.difficulty = Integer.parseInt(req.payload.get("difficulty"));
		srq.friendsOnly = req.payload.get("buddyFilter").equalsIgnoreCase("true");
		srq.maxEntries = Integer.parseInt(req.payload.get("count"));
		srq.key = req.payload.get("key");
		MinigameData[] list = MinigameDataManager.getInstance().getAllGameData(userId,
				Integer.parseInt(req.payload.get("gameId")), srq);

		// Prepare response
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		GameDataSummaryData resp = new GameDataSummaryData();
		resp.gameID = Integer.parseInt(req.payload.get("gameId"));
		resp.difficulty = srq.difficulty;
		resp.isMultiplayer = req.payload.get("isMultiplayer").equalsIgnoreCase("true");
		resp.key = srq.key;
		resp.userPosition = -1;
		resp.entries = new GameDataSummaryData.GameDataBlock[list.length];
		for (int i = 0; i < resp.entries.length; i++) {
			MinigameData data = list[i];
			resp.entries[i] = new GameDataSummaryData.GameDataBlock();
			resp.entries[i].datePlayed = fmt.format(new Date(data.timePlayed));
			resp.entries[i].rankID = i + 1;
			resp.entries[i].timesLost = data.timesLost;
			resp.entries[i].timesWon = data.timesWon;
			resp.entries[i].userID = data.userID;
			resp.entries[i].userName = AccountManager.getInstance().getSaveByID(data.userID).getUsername();
			resp.entries[i].value = data.value;
			if (resp.entries[i].userID.equals(userId))
				resp.userPosition = i;
		}

		// Return
		return ok("text/xml", req.generateXmlValue("GameDataSummary", resp));
	}

	@SodRequest
	@SodTokenSecured
	public FunctionResult sendRawGameData(FunctionInfo func, ServiceRequestInfo req, SessionToken tkn,
			AccountObject account, @SodRequestParam String userId) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();

		// Retrieve container
		AccountSaveContainer save = manager.getSaveByID(userId);
		if (save == null)
			return ok("text/xml", req.generateXmlValue("boolean", false));

		// Create request
		MinigameSaveRequest srq = new MinigameSaveRequest();
		srq.gameLevel = Integer.parseInt(req.payload.get("gameLevel"));
		srq.difficulty = Integer.parseInt(req.payload.get("difficulty"));
		srq.isLoss = req.payload.get("loss").equalsIgnoreCase("true");
		srq.isWin = req.payload.get("win").equalsIgnoreCase("true");
		srq.data = new HashMap<String, Integer>();
		ObjectNode nd = req.parseXmlValue(req.payload.get("xmlDocumentData"), ObjectNode.class);
		Iterator<String> names = nd.fieldNames();
		while (names.hasNext()) {
			String key = names.next();
			srq.data.put(key, nd.get(key).asInt());
		}
		MinigameDataManager.getInstance().saveGameData(save, Integer.parseInt(req.payload.get("gameId")), srq);

		// Return
		return ok("text/xml", req.generateXmlValue("boolean", true));
	}

	@SodRequest
	@SodTokenSecured
	public FunctionResult getUserRoomList(FunctionInfo func, ServiceRequestInfo req, SessionToken tkn,
			AccountObject account, @SodRequestParam RoomListRequestData request) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (roomManager == null)
			roomManager = PlayerRoomManager.getInstance();

		// Retrieve container
		AccountSaveContainer save = account.getSave(request.userID.toString());
		if (save == null)
			return ok("text/xml", req.generateXmlValue("URR", new RoomList()));

		// Create list
		RoomList list = new RoomList();
		ArrayList<RoomListEntryData> rooms = new ArrayList<RoomListEntryData>();
		for (PlayerRoomInfo room : roomManager.getRooms(save)) {
			if (room.getCategoryID() != request.categoryID)
				continue;
			RoomListEntryData entry = new RoomListEntryData();
			entry.roomID = room.getID();
			entry.name = room.getName();
			entry.itemID = room.getItemID();
			entry.creativePoints = room.getCreativePoints();
			entry.categoryID = room.getCategoryID();
			rooms.add(entry);
		}
		list.rooms = rooms.toArray(t -> new RoomListEntryData[t]);
		return ok("text/xml", req.generateXmlValue("URR", list));
	}

	@SodRequest
	@SodTokenSecured
	@TokenRequireSave
	@TokenRequireCapability("gp")
	public FunctionResult setUserRoom(FunctionInfo func, ServiceRequestInfo req, SessionToken tkn,
			AccountObject account, AccountSaveContainer save, @SodRequestParam RoomListEntryData request)
			throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (roomManager == null)
			roomManager = PlayerRoomManager.getInstance();

		// Find room
		PlayerRoomInfo room = roomManager.createOrGetRoom(request.roomID, request.categoryID, save);

		// Update room
		room.setName(request.name);
		room.setCreativePoints(request.creativePoints);
		room.setItemID(request.itemID);
		room.setCategoryID(request.categoryID);

		// Send response
		return ok("text/xml", req.generateXmlValue("URSR", new RoomUpdateResponseData()));
	}

	@SodRequest
	@SodTokenSecured
	public FunctionResult getUserRoomItemPositions(FunctionInfo func, ServiceRequestInfo req, SessionToken tkn,
			AccountObject account, @SodRequestParam String userId, @SodRequestParam String roomID) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (roomManager == null)
			roomManager = PlayerRoomManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Retrieve container
		AccountSaveContainer save = account.getSave(userId);
		if (save == null)
			return ok("text/xml", req.generateXmlValue("ArrayOfUserItemPosition", new RoomItemList()));

		// Find room
		if (!roomManager.roomExists(roomID, save))
			return ok("text/xml", req.generateXmlValue("ArrayOfUserItemPosition", new RoomItemList()));
		PlayerRoomInfo room = roomManager.getRoom(roomID, save);

		// Return
		RoomItemList list = new RoomItemList();
		RoomItemInfo[] items = room.getItems();
		ArrayList<Integer> addingItems = new ArrayList<Integer>();
		ArrayList<RoomItemData> itmData = new ArrayList<RoomItemData>();
		for (RoomItemInfo data : items)
			addItem(itmData, data, items, save, addingItems);
		list.roomItems = itmData.toArray(t -> new RoomItemData[t]);
		String res = req.generateXmlValue("ArrayOfUserItemPosition", list);
		return ok("text/xml", res);
	}

	private void addItem(ArrayList<RoomItemData> itmData, RoomItemInfo it, RoomItemInfo[] itms,
			AccountSaveContainer save, ArrayList<Integer> addingItems) {
		// Check
		if (addingItems.contains(it.roomItemID))
			return;
		addingItems.add(it.roomItemID);

		// Add parent if needed
		if (it.parentID != -1 && Stream.of(itms).anyMatch(t -> t.roomItemID == it.parentID)) {
			RoomItemInfo p = Stream.of(itms).filter(t -> t.roomItemID == it.parentID).findFirst().get();
			addItem(itmData, p, itms, save, addingItems);
		}

		// Create item
		RoomItemData d = new RoomItemData();
		d.itemPositionID = new RoomItemData.IntWrapper(it.roomItemID);
		if (it.parentID != -1)
			d.parentID = new RoomItemData.IntWrapper(it.parentID);
		if (it.itemID != -1)
			d.itemID = new RoomItemData.IntWrapper(it.itemID);
		if (it.itemUniqueID != -1) {
			d.itemUniqueID = new RoomItemData.IntWrapper(it.itemUniqueID);
			d.itemDef = ItemManager.getInstance().getItemDefinition(it.itemID).getRawObject();
		}
		d.uses = new RoomItemData.IntWrapper(it.getCurrentUses(save));
		if (it.inventoryModificationDate != null)
			d.inventoryModificationDate = new RoomItemData.StringWrapper(it.inventoryModificationDate);
		if (it.itemAttributes != null)
			d.itemAttributes = it.itemAttributes;
		if (it.itemStats != null)
			d.itemStats = it.itemStats;
		d.posX = new RoomItemData.DoubleWrapper(it.position.x);
		d.posY = new RoomItemData.DoubleWrapper(it.position.y);
		d.posZ = new RoomItemData.DoubleWrapper(it.position.z);
		d.rotX = new RoomItemData.DoubleWrapper(it.rotation.x);
		d.rotY = new RoomItemData.DoubleWrapper(it.rotation.y);
		d.rotZ = new RoomItemData.DoubleWrapper(it.rotation.z);
		if (it.currentStateID != -1) {
			d.itemState = new RoomItemData.ItemStateBlock();
			SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
			fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
			d.itemState.itemStateID = it.currentStateID;
			d.itemState.stateChangeDate = fmt.format(new Date(it.lastStateChange));
			d.itemState.itemDefID = it.itemID;
			d.itemState.itemUniqueID = it.itemUniqueID;
			d.itemState.itemPositionID = it.roomItemID;
		}
		itmData.add(d);
	}

	@SodRequest
	@SodTokenSecured
	@TokenRequireSave
	@TokenRequireCapability("gp")
	public FunctionResult setUserRoomItemPositions(FunctionInfo func, ServiceRequestInfo req, SessionToken tkn,
			AccountObject account, AccountSaveContainer save, @SodRequestParam String roomID,
			@SodRequestParam RoomItemUpdateRequestData[] createXml,
			@SodRequestParam RoomItemUpdateRequestData[] updateXml, @SodRequestParam int[] removeXml)
			throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (roomManager == null)
			roomManager = PlayerRoomManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Response
		RoomItemUpdateResponseData resp = new RoomItemUpdateResponseData();
		ArrayList<ItemStateBlock> states = new ArrayList<ItemStateBlock>();
		ArrayList<Integer> createdItems = new ArrayList<Integer>();
		resp.success = true;

		// Find room
		PlayerRoomInfo room = roomManager.createOrGetRoom(roomID, roomID.equals("") ? 541 : -1, save);
		HashMap<Integer, RoomItemInfo> newItemData = new HashMap<Integer, RoomItemInfo>();
		for (RoomItemInfo itm : room.getItems())
			newItemData.put(itm.roomItemID, itm);

		// Check create requests
		ArrayList<RoomItemUpdateRequestData> ls = new ArrayList<RoomItemUpdateRequestData>();
		for (RoomItemUpdateRequestData crReq : createXml) {
			if (!handleCreate(newItemData, room, save, crReq, createXml, resp, ls, states, createdItems)) {
				// Invalid
				resp.success = false;
				return ok("text/xml", req.generateXmlValue("UIPSRS", resp));
			}
		}

		// Handle update requests
		for (RoomItemUpdateRequestData crReq : updateXml) {
			// Check
			if (crReq.itemPositionID == null || !newItemData.containsKey(crReq.itemPositionID.value)) {
				continue;
			}

			// Find data
			RoomItemInfo info = newItemData.get(crReq.itemPositionID.value);

			// Assign item fields
			SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
			fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
			if (crReq.uses != null)
				info.uses = crReq.uses.value;
			if (crReq.inventoryModificationDate != null)
				info.inventoryModificationDate = crReq.inventoryModificationDate.value;
			if (crReq.itemID != null) {
				info.itemID = crReq.itemID.value;
				info.inventoryModificationDate = fmt.format(new Date(System.currentTimeMillis()));
			}
			if (crReq.itemUniqueID != null) {
				info.itemUniqueID = crReq.itemUniqueID.value;
				info.inventoryModificationDate = fmt.format(new Date(System.currentTimeMillis()));
			}
			if (crReq.itemAttributes != null)
				info.itemAttributes = crReq.itemAttributes;
			if (crReq.itemStats != null)
				info.itemStats = crReq.itemStats;

			// Load item
			ItemInfo def = itemManager.getItemDefinition(info.itemID);
			if (def == null) {
				// Invalid
				resp.success = false;
				resp.status = 7;
				return ok("text/xml", req.generateXmlValue("UIPSRS", resp));
			}

			// Populate position and rotation
			info.position = new Vector3D(crReq.posX.value, crReq.posY.value, crReq.posZ.value);
			info.rotation = new Vector3D(crReq.rotX.value, crReq.rotY.value, crReq.rotZ.value);

			// Use supplied states
			boolean addedState = false;
			if (crReq.itemState != null) {
				// Assign
				info.currentStateID = crReq.itemState.itemStateID;
				addedState = true;
			}

			// Add modification time
			if (addedState)
				info.lastStateChange = System.currentTimeMillis();
		}

		// Handle removal requests
		for (int id : removeXml) {
			// Check
			if (!newItemData.containsKey(id))
				continue;

			// Remove
			newItemData.remove(id);
		}

		// Return
		if (resp.success) {
			// Add to response
			resp.createdRoomItemIDs = new int[createdItems.size()];
			for (int i = 0; i < resp.createdRoomItemIDs.length; i++)
				resp.createdRoomItemIDs[i] = createdItems.get(i);
			resp.states = states.toArray(t -> new ItemStateBlock[t]);

			// Save
			room.setItems(newItemData.values().toArray(t -> new RoomItemInfo[t]));
		}
		String res = req.generateXmlValue("UIPSRS", resp);
		return ok("text/xml", res);
	}

	private boolean handleCreate(HashMap<Integer, RoomItemInfo> newItemData, PlayerRoomInfo room,
			AccountSaveContainer save, RoomItemUpdateRequestData crReq, RoomItemUpdateRequestData[] createXml,
			RoomItemUpdateResponseData resp, ArrayList<RoomItemUpdateRequestData> addedItems,
			ArrayList<ItemStateBlock> statesL, ArrayList<Integer> createdItems) {
		// Check
		if (addedItems.contains(crReq))
			return true;
		addedItems.add(crReq);

		// Create object
		RoomItemInfo info = new RoomItemInfo();

		// If present, find parent
		if (crReq.parentIndex != null) {
			// Verify
			int ind = crReq.parentIndex.value;
			if (ind < 0 || ind > createXml.length) {
				// Invalid
				resp.status = 4;
				return false;
			}

			// Found parent index
			if (!handleCreate(newItemData, room, save, createXml[ind], createXml, resp, addedItems, statesL,
					createdItems))
				return false;

			// Assign parent
			info.parentID = createXml[ind].result.roomItemID;
		} else if (crReq.parentID != null) {
			// Verify
			if (!newItemData.containsKey(crReq.parentID.value)) {
				// Invalid
				resp.status = 3;
				return false;
			}

			// Assign parent
			info.parentID = crReq.parentID.value;
		}

		// Assign item fields
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		if (crReq.itemID != null)
			info.itemID = crReq.itemID.value;
		else
			info.itemID = save.getInventory().getContainer(1).getItem(crReq.itemUniqueID.value).getItemDefID();
		info.itemUniqueID = crReq.itemUniqueID.value;
		info.inventoryModificationDate = fmt.format(new Date(System.currentTimeMillis()));
		if (crReq.uses != null)
			info.uses = crReq.uses.value;
		if (crReq.itemAttributes != null)
			info.itemAttributes = crReq.itemAttributes;
		if (crReq.itemStats != null)
			info.itemStats = crReq.itemStats;

		// Load item
		ItemInfo def = itemManager.getItemDefinition(info.itemID);
		if (def == null) {
			resp.status = 7;
			return false; // Invalid
		}

		// Populate position and rotation
		info.position = new Vector3D(crReq.posX.value, crReq.posY.value, crReq.posZ.value);
		info.rotation = new Vector3D(crReq.rotX.value, crReq.rotY.value, crReq.rotZ.value);

		// Find best state
		boolean addedState = false;
		ItemDefData raw = def.getRawObject();
		if (raw.states.length >= 1) {
			// Assign first
			info.currentStateID = raw.states[0].stateID;
			addedState = true;
		}

		// Use supplied states
		if (crReq.itemState != null) {
			// Assign
			info.currentStateID = crReq.itemState.itemStateID;
			addedState = true;
		}

		// Create
		info = roomManager.createRoomItem(info, save);

		// Create state block
		if (addedState) {
			ItemStateBlock stateBlock = new ItemStateBlock();
			stateBlock.itemStateID = info.currentStateID;
			stateBlock.stateChangeDate = fmt.format(new Date(info.lastStateChange));
			stateBlock.itemDefID = info.itemID;
			stateBlock.itemUniqueID = info.itemUniqueID;
			stateBlock.itemPositionID = info.roomItemID;
			statesL.add(stateBlock);
		}

		// Success
		crReq.result = info;
		newItemData.put(info.roomItemID, info);
		createdItems.add(info.roomItemID);
		return true;
	}

	@SodRequest
	@SodTokenSecured
	@TokenRequireSave
	@TokenRequireCapability("gp")
	public FunctionResult setNextItemState(FunctionInfo func, ServiceRequestInfo req, SessionToken tkn,
			AccountObject account, AccountSaveContainer save,
			@SodRequestParam("setNextItemStateRequest") SetItemStateRequestData request) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (roomManager == null)
			roomManager = PlayerRoomManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Find item
		RoomItemInfo item = roomManager.getRoomItem(request.roomItemID, save);
		if (item == null) {
			// Error
			SetItemStateResponseData resp = new SetItemStateResponseData();
			resp.errorCode = 255;
			resp.success = false;
			return ok("text/xml", req.generateXmlValue("SetNextItemStateResult", resp));
		}

		// Find room
		PlayerRoomInfo room = roomManager.getRoom(item.roomID, save);
		RoomItemInfo[] roomItems = room.getItems();

		// Build response
		SetItemStateResponseData resp = new SetItemStateResponseData();
		resp.errorCode = 1;
		resp.success = true;

		// Find inventory item
		PlayerInventoryItem invItm = itemManager.getCommonInventory(save.getSaveData()).getContainer(1)
				.getItem(request.itemUniqueID);
		if (invItm == null) {
			// Error
			resp.errorCode = 4; // 4 = cannot find inventory item
			resp.success = false;
			return ok("text/xml", req.generateXmlValue("SetNextItemStateResult", resp));
		}

		// Get current state
		ItemDefData def = invItm.getItemDef().getRawObject();
		Optional<ItemStateData> stateO = Stream.of(def.states).filter(t -> t.stateID == item.currentStateID)
				.findFirst();
		if (!stateO.isPresent()) {
			// Error
			resp.errorCode = 10; // 10 = cannot find state
			resp.success = false;
			return ok("text/xml", req.generateXmlValue("SetNextItemStateResult", resp));
		}
		ItemStateData state = stateO.get();

		// Check criteria
		if (state.rules == null || state.rules.criteria.length == 0) {
			// Error
			resp.errorCode = 12; // 12 = cannot find criteria
			resp.success = false;
			return ok("text/xml", req.generateXmlValue("SetNextItemStateResult", resp));
		}

		// Handle transition
		int nextState = -1;
		if (state.rules.completionAction != null) {
			switch (state.rules.completionAction.transitionMode) {

			case 1: {
				// Next state

				// Find next state
				int i = 0;
				ItemStateData[] states = Stream.of(def.states).sorted((t1, t2) -> Integer.compare(t1.order, t2.order))
						.toArray(t -> new ItemStateData[t]);
				for (ItemStateData st : states) {
					if (st == state) {
						// Next state is the one
						if (i + 1 < states.length) {
							nextState = states[i + 1].stateID;
							break;
						}
					}
					i++;
				}

				break;
			}

			case 3: {
				// Deletion
				nextState = -2;
				break;
			}

			case 4: {
				// Initial state

				// Find state
				ItemStateData st = null;
				for (ItemStateData st2 : def.states) {
					if (st == null || st2.order < st.order)
						st = st2;
				}

				// Assign
				if (st != null)
					nextState = st.stateID;

			}

			}
		}

		// Go through criteria
		for (ItemStateCriteriaWrapperData criteria : state.rules.criteria) {
			switch (criteria.criteriaType) {

			case 1:
				// Length criteria
				if (!request.isSpeedUp) {
					// Check time passed
					ItemStateLengthCriteriaData crit = (ItemStateLengthCriteriaData) criteria.criteriaData;
					if (System.currentTimeMillis() - item.lastStateChange < crit.period * 1000) {
						// Error
						resp.errorCode = 3; // 3 = time not reached
						resp.success = false;
						return ok("text/xml", req.generateXmlValue("SetNextItemStateResult", resp));
					}
				}
				break;

			case 2:
				// Consumable criteria
				if (!request.isSpeedUp) {
					// Find item
					ItemStateConsumableCriteriaData crit = (ItemStateConsumableCriteriaData) criteria.criteriaData;

					// Check
					if (crit.amount != 0) {
						if (crit.appliesUses) {
							// Find from room
							Optional<RoomItemInfo> targetItmO = Stream.of(roomItems)
									.filter(t -> t.itemID == crit.itemID).findFirst();
							if (targetItmO.isPresent()) {
								// Check uses
								RoomItemInfo targetItm = targetItmO.get();
								if (targetItm.getCurrentUses(save) >= crit.amount) {
									// Decrease target uses
									targetItm.uses = targetItm.getCurrentUses(save) - crit.amount;
								} else {
									// Error
									resp.errorCode = 5; // 5 = less uses than required
									resp.success = false;
									return ok("text/xml", req.generateXmlValue("SetNextItemStateResult", resp));
								}
							} else {
								// Error
								resp.errorCode = 5; // 5 = less uses than required
								resp.success = false;
								return ok("text/xml", req.generateXmlValue("SetNextItemStateResult", resp));
							}
						} else {
							// Find items
							PlayerInventoryItem itm = itemManager.getCommonInventory(save.getSaveData()).getContainer(1)
									.findFirst(crit.itemID);
							if (itm == null || itm.getQuantity() < crit.amount) {
								// Error
								resp.errorCode = 7; // 7 = quantity less than required
								resp.success = false;
								return ok("text/xml", req.generateXmlValue("SetNextItemStateResult", resp));
							}

							// Decrease
							itm.remove(crit.amount);
						}
					}
				}
				break;

			case 5:
				// Expiry criteria
				if (!request.isSpeedUp) {
					// Check time passed
					ItemStateExpiryCriteriaData crit = (ItemStateExpiryCriteriaData) criteria.criteriaData;
					if (System.currentTimeMillis() - item.lastStateChange >= crit.period * 1000) {
						// Expiry
						nextState = crit.targetState;
					}
				}
				break;

			case 4:
				// Speed up criteria
				if (request.isSpeedUp) {
					ItemStateSpeedUpCriteriaData crit = (ItemStateSpeedUpCriteriaData) criteria.criteriaData;

					// Check
					if (crit.appliesUses) {
						// Find from room
						Optional<RoomItemInfo> targetItmO = Stream.of(roomItems).filter(t -> t.itemID == crit.itemID)
								.findFirst();
						if (targetItmO.isPresent()) {
							// Check uses
							RoomItemInfo targetItm = targetItmO.get();
							if (targetItm.getCurrentUses(save) >= crit.amount) {
								// Decrease target uses
								targetItm.uses = targetItm.getCurrentUses(save) - crit.amount;
							} else {
								// Error
								resp.errorCode = 2; // 2 = cannot override criteria
								resp.success = false;
								return ok("text/xml", req.generateXmlValue("SetNextItemStateResult", resp));
							}
						} else {
							// Error
							resp.errorCode = 2; // 2 = cannot override criteria
							resp.success = false;
							return ok("text/xml", req.generateXmlValue("SetNextItemStateResult", resp));
						}
					} else {
						// Find item
						PlayerInventoryItem itm = itemManager.getCommonInventory(save.getSaveData()).getContainer(1)
								.findFirst(crit.itemID);
						if (itm == null || itm.getQuantity() < crit.amount) {
							// Attempt purchase
							PlayerInventoryItem itmF = itm;
							InventoryUpdateResponseData purchase = InventoryUtils.purchaseItems(request.storeID,
									new ItemRedemptionInfo[] { new ItemRedemptionInfo() {
										{
											defID = crit.itemID;
											quantity = crit.amount;
											if (itmF != null)
												quantity -= itmF.getQuantity();
											containerID = 1;
										}
									} }, account, save, false);
							if (!purchase.success) {
								// Error
								resp.errorCode = 2; // 2 = cannot override criteria
								resp.success = false;
								return ok("text/xml", req.generateXmlValue("SetNextItemStateResult", resp));
							}
							itm = itemManager.getCommonInventory(save.getSaveData()).getContainer(1)
									.findFirst(crit.itemID);
						}

						// Decrease
						itm.remove(crit.amount);
					}

					// Apply
					if (crit.speedUpUses) {
						// Apply uses speedup
						item.uses = crit.speedUpUsesCount;
					}

					// Move state
					if (crit.changesState) {
						// Set state
						nextState = crit.targetState;
					}
				}
				break;

			}
		}

		// Check state
		int nextStateF = nextState;
		if (nextState == -1 || (nextState != -2 && !Stream.of(def.states).anyMatch(t -> t.stateID == nextStateF))) {
			// Error
			resp.errorCode = 9; // 9 = transition failed
			resp.success = false;
			return ok("text/xml", req.generateXmlValue("SetNextItemStateResult", resp));
		}

		// Transition
		if (nextState == -2) {
			// Delete item
			roomManager.deleteRoomItem(item.roomItemID, save);

			// Remove from room
			ArrayList<RoomItemInfo> newItems = new ArrayList<RoomItemInfo>();
			for (RoomItemInfo itm : roomItems) {
				if (itm.roomItemID != item.roomItemID)
					newItems.add(itm);
			}
			room.setItems(newItems.toArray(t -> new RoomItemInfo[t]));
		} else {
			// Set state
			item.currentStateID = nextState;
			item.lastStateChange = System.currentTimeMillis();

			// Save item
			roomManager.saveRoomItem(item, save);
		}

		// Apply rewards
		resp.rewards = RewardsUtil.giveRewardsTo(save, state.rewards, false, 1);

		// Create state object
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		resp.state = new RoomItemData.ItemStateBlock();
		resp.state.itemDefID = item.itemID;
		resp.state.itemUniqueID = item.itemUniqueID;
		resp.state.itemPositionID = item.roomItemID;
		resp.state.stateChangeDate = fmt.format(new Date(item.lastStateChange));
		resp.state.itemStateID = item.currentStateID;

		// Return
		return ok("text/xml", req.generateXmlValue("SetNextItemStateResult", resp));
	}

	@SodRequest
	@SodTokenSecured
	@TokenRequireSave
	@TokenRequireCapability("gp")
	public FunctionResult setGameCurrency(FunctionInfo func, ServiceRequestInfo req, SessionToken tkn,
			AccountObject account, AccountSaveContainer save, @SodRequestParam int amount) throws IOException {
		// Load currency
		AccountDataContainer currency = save.getSaveData().getChildContainer("currency");

		// Load coins
		int currentC = 300;
		if (currency.entryExists("coins"))
			currentC = currency.getEntry("coins").getAsInt();

		// Update
		currency.setEntry("coins", new JsonPrimitive(currentC + amount));

		// Return
		return ok("text/xml", req.generateXmlValue("int", currentC + amount));
	}

	private static KeyValuePairData[] appendTo(KeyValuePairData[] arr, KeyValuePairData block) {
		KeyValuePairData[] newB = new KeyValuePairData[arr.length + 1];
		for (int i = 0; i < newB.length; i++) {
			if (i >= arr.length)
				newB[i] = block;
			else
				newB[i] = arr[i];
		}
		return newB;
	}

	private static ItemUpdateBlock[] appendTo(ItemUpdateBlock[] arr, ItemUpdateBlock block) {
		ItemUpdateBlock[] newB = new ItemUpdateBlock[arr.length + 1];
		for (int i = 0; i < newB.length; i++) {
			if (i >= arr.length)
				newB[i] = block;
			else
				newB[i] = arr[i];
		}
		return newB;
	}

}
