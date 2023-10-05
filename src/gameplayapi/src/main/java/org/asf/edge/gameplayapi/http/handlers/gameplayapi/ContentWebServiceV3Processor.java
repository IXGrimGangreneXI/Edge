package org.asf.edge.gameplayapi.http.handlers.gameplayapi;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.http.apihandlerutils.functions.LegacyFunction;
import org.asf.edge.common.http.apihandlerutils.functions.LegacyFunctionInfo;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.gameplayapi.EdgeGameplayApiServer;
import org.asf.edge.gameplayapi.util.InventoryUtils;
import org.asf.edge.gameplayapi.xmls.dragons.DragonData;
import org.asf.edge.gameplayapi.xmls.dragons.PetUpdateRequestData;
import org.asf.edge.gameplayapi.xmls.dragons.PetUpdateResponseData;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ContentWebServiceV3Processor extends EdgeWebService<EdgeGameplayApiServer> {

	private static AccountManager manager;
	private static ItemManager itemManager;

	public ContentWebServiceV3Processor(EdgeGameplayApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new ContentWebServiceV3Processor(getServerInstance());
	}

	@Override
	public String path() {
		return "/v3/ContentWebService.asmx";
	}

	@Override
	public void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		// Handle request
		path = path;
		setResponseStatus(404, "Not found");
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void setRaisedPet(LegacyFunctionInfo func) throws IOException {
		if (manager == null)
			manager = AccountManager.getInstance();
		if (itemManager == null)
			itemManager = ItemManager.getInstance();

		// Handle dragon request
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
		PetUpdateRequestData request = req.parseXmlValue(req.payload.get("request"), PetUpdateRequestData.class);

		// Find save
		AccountSaveContainer save = account.getSave(tkn.saveID);
		AccountDataContainer data = save.getSaveData();

		// Prepare response
		PetUpdateResponseData resp = new PetUpdateResponseData();
		resp.raisedPetSetResult = 1;

		// Pull dragons
		data = data.getChildContainer("dragons");
		JsonArray dragonIds = new JsonArray();
		if (data.entryExists("dragonlist"))
			dragonIds = data.getEntry("dragonlist").getAsJsonArray();
		else
			data.setEntry("dragonlist", dragonIds);

		// Find id
		int id = request.dragonData.id;
		if (!data.entryExists("dragon-" + id)) {
			// Error
			resp.raisedPetSetResult = 3;
			resp.errorMessage = "Dragon ID not found";
			setResponseContent("text/xml", req.generateXmlValue("SetRaisedPetResponse", resp));
			return;
		}

		// Check filter
		if (TextFilterService.getInstance().isFiltered(request.dragonData.name, true)) {
			// Error
			resp.raisedPetSetResult = 4;
			resp.errorMessage = "Invalid name";
			setResponseContent("text/xml", req.generateXmlValue("SetRaisedPetResponse", resp));
			return;
		}

		// Read dragon data
		DragonData cdragon = req.parseXmlValue(data.getEntry("dragon-" + id).getAsString(), DragonData.class);
		if (!cdragon.name.equals(request.dragonData.name)) {
			// Check name
			String newName = request.dragonData.name;
			if (newName.length() > 100 || newName.replace(" ", "").length() < 1) {
				// Error
				resp.raisedPetSetResult = 4;
				resp.errorMessage = "Invalid name";
				setResponseContent("text/xml", req.generateXmlValue("SetRaisedPetResponse", resp));
				return;
			}
		}

		// Fill fields
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		DragonData dragonUpdate = request.dragonData;

		// Merge data
		if (dragonUpdate.accessories != null)
			cdragon.accessories = dragonUpdate.accessories;
		if (dragonUpdate.attributes != null) {
			if (cdragon.attributes == null)
				cdragon.attributes = new ObjectNode[0];

			// Apply attributes
			for (ObjectNode attr : dragonUpdate.attributes) {
				String key = attr.get("k").asText();
				Optional<ObjectNode> optA = Stream.of(cdragon.attributes).filter(t -> t.get("k").asText().equals(key))
						.findFirst();
				if (optA.isPresent()) {
					// Update
					optA.get().set("v", attr.get("v"));
					optA.get().set("dt", attr.get("dt"));
				} else {
					// Add
					int i = 0;
					ObjectNode[] newA = new ObjectNode[cdragon.attributes.length + 1];
					for (i = 0; i < cdragon.attributes.length; i++)
						newA[i] = cdragon.attributes[i];
					newA[i] = attr;
					cdragon.attributes = newA;
				}
			}
		}
		if (dragonUpdate.colors != null)
			cdragon.colors = dragonUpdate.colors;
		if (dragonUpdate.gender != null)
			cdragon.gender = dragonUpdate.gender;
		if (dragonUpdate.geometry != null)
			cdragon.geometry = dragonUpdate.geometry;
		if (dragonUpdate.texture != null)
			cdragon.texture = dragonUpdate.texture;
		if (dragonUpdate.skills != null)
			cdragon.skills = dragonUpdate.skills;
		if (dragonUpdate.growthState != null)
			cdragon.growthState = dragonUpdate.growthState;
		if (dragonUpdate.imagePosition != null)
			cdragon.imagePosition = dragonUpdate.imagePosition;
		if (dragonUpdate.states != null)
			cdragon.states = dragonUpdate.states;
		if (dragonUpdate.typeID != null)
			cdragon.typeID = dragonUpdate.typeID;
		if (dragonUpdate.name != null)
			cdragon.name = dragonUpdate.name;

		// Set update time
		cdragon.updateDate = fmt.format(new Date()); // Update time

		// Set active if needed
		if (request.setAsSelected) {
			// Deselect old
			for (JsonElement ele : dragonIds) {
				String did = ele.getAsString();
				ObjectNode ddragon = req.parseXmlValue(data.getEntry("dragon-" + did).getAsString(), ObjectNode.class);

				// Check if active
				if (ddragon.get("is").asBoolean()) {
					// deselect
					ddragon.set("is", BooleanNode.FALSE);

					// Save
					data.setEntry("dragon-" + did, new JsonPrimitive(req.generateXmlValue("RaisedPetData", ddragon)));
				}
			}

			// Set current as active
			cdragon.isSelected = true;
		}

		// Save dragon
		data.setEntry("dragon-" + id, new JsonPrimitive(req.generateXmlValue("RaisedPetData", cdragon)));

		// Handle inventory
		if (request.commonInventoryRequests != null && request.commonInventoryRequests.length != 0) {
			// Handle inventory request
			resp.inventoryUpdate = InventoryUtils.processCommonInventorySet(request.commonInventoryRequests,
					save.getSaveData(), (request.containerID == -1 ? 1 : request.containerID));
		}

		// Set response
		setResponseContent("text/xml", req.generateXmlValue("SetRaisedPetResponse", resp));
	}

}
