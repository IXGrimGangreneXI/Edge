package org.asf.edge.gameplayapi.http.handlers.itemstore;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.apihandlerutils.BaseApiHandler;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.gameplayapi.EdgeGameplayApiServer;
import org.asf.edge.gameplayapi.entities.ItemStoreInfo;
import org.asf.edge.gameplayapi.services.ItemManager;
import org.asf.edge.gameplayapi.xmls.items.GetStoreRequestData;
import org.asf.edge.gameplayapi.xmls.items.GetStoreResponseData;
import org.asf.edge.gameplayapi.xmls.items.ItemStoreDefinitionData;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ItemStoreWebServiceProcessor extends BaseApiHandler<EdgeGameplayApiServer> {

	private static ItemManager itemManager;

	public ItemStoreWebServiceProcessor(EdgeGameplayApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new ItemStoreWebServiceProcessor(getServerInstance());
	}

	@Override
	public String path() {
		return "/ItemStoreWebService.asmx";
	}

	@Override
	public void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		// Handle request
		path = path;
		setResponseStatus(404, "Not found");
	}

	@Function(allowedMethods = { "POST" })
	public void getRankAttributeData(FunctionInfo info) throws IOException {
		// Handle request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;

		// Load XML
		InputStream strm = getClass().getClassLoader().getResourceAsStream("rankattrs.xml");
		String data = new String(strm.readAllBytes(), "UTF-8");
		strm.close();
		setResponseContent("text/xml", data);
	}

	@Function(allowedMethods = { "POST" })
	public void getStore(FunctionInfo info) throws IOException {
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Handle store request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;

		// Find stores
		GetStoreRequestData stores = req.parseXmlValue(req.payload.get("getStoreRequest"), GetStoreRequestData.class);
		GetStoreResponseData resp = new GetStoreResponseData();
		resp.stores = new ItemStoreDefinitionData[stores.storeIDs.length];
		for (int i = 0; i < resp.stores.length; i++) {
			// Find store
			ItemStoreInfo store = itemManager.getStore(stores.storeIDs[i]);
			if (store != null) {
				ItemStoreDefinitionData storeData = new ItemStoreDefinitionData();
				storeData.storeID = store.getID();
				storeData.storeName = store.getName();
				storeData.storeDescription = store.getDescription();
				storeData.items = Stream.of(store.getItems()).map(t -> t.getRawObject())
						.toArray(t -> new ObjectNode[t]);
				resp.stores[i] = storeData;
			} else {
				resp.stores[i] = new ItemStoreDefinitionData();
			}
		}

		// Set response
		setResponseContent("text/xml", req.generateXmlValue("GetStoreResponse", resp));
	}

}
