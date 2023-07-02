package org.asf.edge.gameplayapi.http.handlers.gameplayapi;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.TimeZone;

import org.asf.connective.RemoteClient;
import org.asf.connective.TlsSecuredHttpServer;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.http.apihandlerutils.BaseApiHandler;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.gameplayapi.EdgeGameplayApiServer;
import org.asf.edge.gameplayapi.entities.quests.UserQuestInfo;
import org.asf.edge.gameplayapi.services.quests.QuestManager;
import org.asf.edge.gameplayapi.xmls.data.KeyValuePairData;
import org.asf.edge.gameplayapi.xmls.data.KeyValuePairSetData;
import org.asf.edge.gameplayapi.xmls.dragons.DragonListData;
import org.asf.edge.gameplayapi.xmls.data.EmptyKeyValuePairSetData;
import org.asf.edge.gameplayapi.xmls.inventories.CommonInventoryData;
import org.asf.edge.gameplayapi.xmls.inventories.SetCommonInventoryRequestData;
import org.asf.edge.gameplayapi.xmls.inventories.CommonInventoryData.ItemBlock;
import org.asf.edge.gameplayapi.xmls.names.DisplayNameUniqueResponseData;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ContentWebServiceV1Processor extends BaseApiHandler<EdgeGameplayApiServer> {

	private static AccountManager manager;
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
		setResponseStatus(404, "Not found");
	}

	@Function(allowedMethods = { "POST" })
	public void getDefaultNameSuggestion(FunctionInfo func) throws IOException {
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

	@Function(allowedMethods = { "POST" })
	public void getAuthoritativeTime(FunctionInfo func) throws IOException {
		// Handle time request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;

		// Set response
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		setResponseContent("text/xml",
				req.generateXmlValue("dateTime", fmt.format(new Date(System.currentTimeMillis()))));
	}

	@Function(allowedMethods = { "POST" })
	public void acceptMission(FunctionInfo func) throws IOException {
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

	@Function(allowedMethods = { "POST" })
	public void getCommonInventory(FunctionInfo func) throws IOException {
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
		String containerID = req.payload.get("ContainerId");
		data = data.getChildContainer("commoninventories").getChildContainer(containerID);

		// Retrieve container
		CommonInventoryData resp = new CommonInventoryData();
		resp.userID = account.getAccountID();

		// Find items
		JsonElement e = data.getEntry("itemlist");
		if (e == null) {
			e = new JsonArray();
			data.setEntry("itemlist", e);
		}
		ArrayList<ItemBlock> items = new ArrayList<ItemBlock>();
		for (JsonElement itemDefEle : e.getAsJsonArray()) {
			int uniqueID = itemDefEle.getAsInt();

			// Locate item
			JsonObject itm = data.getEntry("item-" + uniqueID).getAsJsonObject();

			// Add item
			ItemBlock block = new ItemBlock();
			block.itemID = itm.get("id").getAsInt();
			block.quantity = itm.get("quantity").getAsInt();
			block.uses = itm.get("uses").getAsInt();
			block.uniqueItemID = uniqueID;

			// Add data info from item manager
			ItemInfo def = ItemManager.getInstance().getItemDefinition(block.itemID);
			if (def != null)
				block.data = def.getRawObject();
			items.add(block);
		}
		resp.items = items.toArray(t -> new ItemBlock[t]);

		// Set response
		setResponseContent("text/xml", req.generateXmlValue("CI", resp));
	}

	@Function(allowedMethods = { "POST" })
	public void setCommonInventory(FunctionInfo func) throws IOException {
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
		setResponseContent("text/xml", req.generateXmlValue("CIRS", ContentWebServiceV2Processor
				.processCommonInventorySet(requests, data, Integer.parseInt(req.payload.get("ContainerId")))));
	}

	@Function(allowedMethods = { "POST" })
	public void getKeyValuePair(FunctionInfo func) throws IOException {
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
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss");
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

			// Set response
			if (setData.items.length != 0)
				setResponseContent("text/xml", req.generateXmlValue("Pairs", setData));
			else
				setResponseContent("text/xml", req.generateXmlValue("Pairs", new EmptyKeyValuePairSetData()));
		} else {
			// Not found
			setResponseContent("text/xml", req.generateXmlValue("Pairs", new EmptyKeyValuePairSetData()));
		}
	}

	@Function(allowedMethods = { "POST" })
	public void getKeyValuePairByUserID(FunctionInfo func) throws IOException {
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
			SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss");
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

				// Set response
				if (setData.items.length != 0)
					setResponseContent("text/xml", req.generateXmlValue("Pairs", setData));
				else
					setResponseContent("text/xml", req.generateXmlValue("Pairs", new EmptyKeyValuePairSetData()));
			} else {
				// Not found
				setResponseContent("text/xml", req.generateXmlValue("Pairs", new EmptyKeyValuePairSetData()));
			}
		} else {
			setResponseStatus(403, "Forbidden");
		}
	}

	@Function(allowedMethods = { "POST" })
	public void setKeyValuePair(FunctionInfo func) throws IOException {
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

	@Function(allowedMethods = { "POST" })
	public void setKeyValuePairByUserID(FunctionInfo func) throws IOException {
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

	@Function(allowedMethods = { "POST" })
	public void delKeyValuePair(FunctionInfo func) throws IOException {
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

	@Function(allowedMethods = { "POST" })
	public void delKeyValuePairByUserID(FunctionInfo func) throws IOException {
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

	@Function(allowedMethods = { "POST" })
	public void delKeyValuePairByKey(FunctionInfo func) throws IOException {
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

	@Function(allowedMethods = { "POST" })
	public void getSelectedRaisedPet(FunctionInfo func) throws IOException {
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
			ArrayList<ObjectNode> dragons = new ArrayList<ObjectNode>();

			// Populate list
			for (JsonElement ele : dragonIds) {
				String id = ele.getAsString();
				ObjectNode dragon = req.parseXmlValue(data.getEntry("dragon-" + id).getAsString(), ObjectNode.class);

				// Check if active
				if (dragon.get("is").asBoolean()) {
					// Add selected
					dragons.add(dragon);
				}
			}

			// Set response
			if (dragons.size() != 0) {
				DragonListData ls = new DragonListData();
				ls.dragons = dragons.toArray(t -> new ObjectNode[t]);
				setResponseContent("text/xml", req.generateXmlValue("ArrayOfRaisedPetData", ls));
			} else
				setResponseContent("text/xml", req.generateXmlValue("ArrayOfRaisedPetData", null));
		} else {
			// ???
			setResponseStatus(403, "Forbidden, attempted to interact with other user's data (devs please inspect)");
		}
	}

	@Function(allowedMethods = { "POST" })
	public void setSelectedPet(FunctionInfo func) throws IOException {
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

	@Function(allowedMethods = { "POST", "GET" })
	public void getImage(FunctionInfo func) throws IOException {
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
		setResponseContent("text/xml", data.getEntry("imageslotinfo-" + slot + "-" + type).getAsString());
	}

	@Function(allowedMethods = { "POST" })
	public void setImage(FunctionInfo func) throws IOException {
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
}
