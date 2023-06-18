package org.asf.edge.gameplayapi.http.handlers.gameplayapi;

import java.io.IOException;
import java.util.ArrayList;

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
import org.asf.edge.gameplayapi.xmls.CommonInventoryData;
import org.asf.edge.gameplayapi.xmls.CommonInventoryData.ItemBlock;

import com.google.gson.JsonElement;

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
			data.setEntry("itemlist", e);
		}
		ArrayList<ItemBlock> items = new ArrayList<ItemBlock>();
		for (JsonElement itemDefEle : e.getAsJsonArray()) {
			int id = itemDefEle.getAsInt();

			// Locate item
			AccountDataContainer itm = data.getChildContainer("item-" + id);

			// Add item
			ItemBlock block = new ItemBlock();
			block.itemID = id;
			block.quantity = itm.getEntry("quantity").getAsInt();
			block.uses = itm.getEntry("uses").getAsInt();
			block.userInventoryID = itm.getEntry("userInventoryID").getAsInt();

			// Add data info from item manager
			ItemInfo def = ItemManager.getInstance().getItemDefinition(id);
			if (def != null)
				block.data = def.getRawObject();
		}
		resp.items = items.toArray(t -> new ItemBlock[t]);

		// Set response
		setResponseContent("text/xml", req.generateXmlValue("CI", resp));
	}
}
