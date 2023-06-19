package org.asf.edge.gameplayapi.http.handlers.gameplayapi;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.account.AccountDataContainer;
import org.asf.edge.common.account.AccountManager;
import org.asf.edge.common.account.AccountObject;
import org.asf.edge.common.http.apihandlerutils.BaseApiHandler;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.gameplayapi.EdgeGameplayApiServer;
import org.asf.edge.gameplayapi.entities.ItemInfo;
import org.asf.edge.gameplayapi.services.ItemManager;
import org.asf.edge.gameplayapi.xmls.data.KeyValuePairData;
import org.asf.edge.gameplayapi.xmls.data.KeyValuePairSetData;
import org.asf.edge.gameplayapi.xmls.data.EmptyKeyValuePairSetData;
import org.asf.edge.gameplayapi.xmls.inventories.CommonInventoryData;
import org.asf.edge.gameplayapi.xmls.inventories.CommonInventoryData.ItemBlock;
import org.asf.edge.gameplayapi.xmls.names.DisplayNameUniqueResponseData;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ContentWebServiceV1Processor extends BaseApiHandler<EdgeGameplayApiServer> {

	private AccountManager manager;
	private static ItemManager itemManager;

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
		setResponseContent("text/xml",
				req.generateXmlValue("dateTime", fmt.format(new Date(System.currentTimeMillis()))));
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
			int id = itemDefEle.getAsInt();

			// Locate item
			JsonObject itm = data.getEntry("item-" + id).getAsJsonObject();

			// Add item
			ItemBlock block = new ItemBlock();
			block.itemID = id;
			block.quantity = itm.get("quantity").getAsInt();
			block.uses = itm.get("uses").getAsInt();
			block.userInventoryID = itm.get("userInventoryID").getAsInt();

			// Add data info from item manager
			ItemInfo def = ItemManager.getInstance().getItemDefinition(id);
			if (def != null)
				block.data = def.getRawObject();
			items.add(block);
		}
		resp.items = items.toArray(t -> new ItemBlock[t]);

		// Set response
		setResponseContent("text/xml", req.generateXmlValue("CI", resp));
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
		if (data.entryExists("pairs-" + pair)) {
			// Get data
			int i = 0;
			JsonArray pairs = data.getEntry("pairs-" + pair).getAsJsonArray();
			KeyValuePairSetData setData = new KeyValuePairSetData();
			setData.items = new KeyValuePairData[pairs.size()];
			for (JsonElement ele : pairs) {
				JsonObject obj = ele.getAsJsonObject();
				KeyValuePairData p = new KeyValuePairData();
				p.key = obj.get("key").getAsString();
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
		if (userID.equals(account.getAccountID()) || account.getSave(userID) != null) {
			AccountDataContainer data = account.getAccountData();
			if (!userID.equals(account.getAccountID()))
				data = account.getSave(userID).getSaveData();
			data = data.getChildContainer("keyvaluedata");

			// Set result
			SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss");
			if (data.entryExists("pairs-" + pair)) {
				// Get data
				int i = 0;
				JsonArray pairs = data.getEntry("pairs-" + pair).getAsJsonArray();
				KeyValuePairSetData setData = new KeyValuePairSetData();
				setData.items = new KeyValuePairData[pairs.size()];
				for (JsonElement ele : pairs) {
					JsonObject obj = ele.getAsJsonObject();
					KeyValuePairData p = new KeyValuePairData();
					p.key = obj.get("key").getAsString();
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
			pair = pair; // ???
			setResponseStatus(403, "Forbidden, attempted to interact with other user's data (devs please inspect)");
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
		JsonArray pairData = new JsonArray();
		for (KeyValuePairData pairI : updateData.items) {
			// Set entry
			JsonObject p = new JsonObject();
			p.addProperty("key", pairI.key);
			p.addProperty("value", pairI.value);
			p.addProperty("time", System.currentTimeMillis());
			pairData.add(p);
		}
		data.setEntry("pairs-" + pair, pairData);

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
		if (userID.equals(account.getAccountID()) || account.getSave(userID) != null) {
			AccountDataContainer data = account.getAccountData();
			if (!userID.equals(account.getAccountID()))
				data = account.getSave(userID).getSaveData();
			data = data.getChildContainer("keyvaluedata");

			// Set entries
			JsonArray pairData = new JsonArray();
			for (KeyValuePairData pairI : updateData.items) {
				// Set entry
				JsonObject p = new JsonObject();
				p.addProperty("key", pairI.key);
				p.addProperty("value", pairI.value);
				p.addProperty("time", System.currentTimeMillis());
				pairData.add(p);
			}
			data.setEntry("pairs-" + pair, pairData);

			// Set response
			setResponseContent("text/xml", req.generateXmlValue("boolean", true));
		} else {
			pair = pair; // ???
			setResponseStatus(403, "Forbidden, attempted to interact with other user's data (devs please inspect)");
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
			pair = pair; // ???
			setResponseStatus(403, "Forbidden, attempted to interact with other user's data (devs please inspect)");
		}
	}

}
