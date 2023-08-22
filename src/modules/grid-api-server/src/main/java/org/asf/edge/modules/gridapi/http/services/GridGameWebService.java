package org.asf.edge.modules.gridapi.http.services;

import java.io.IOException;
import java.util.UUID;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionResult;
import org.asf.edge.modules.gridapi.EdgeGridApiServer;
import org.asf.edge.modules.gridapi.utils.PhoenixToken;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GridGameWebService extends EdgeWebService<EdgeGridApiServer> {

	public GridGameWebService(EdgeGridApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new GridGameWebService(getServerInstance());
	}

	@Override
	public String path() {
		return "/grid/gameservice";
	}

	@Function(allowedMethods = { "POST" }, value = "startgame")
	public FunctionResult startGame(FunctionInfo func) throws IOException {
		// Game startup call, used to obtain login tokens

		// Load request
		JsonObject request;
		try {
			// Parse and check
			request = JsonParser.parseString(func.getRequest().getRequestBodyAsString()).getAsJsonObject();
			if (!request.has("clientApiVersion") || !request.has("clientSoftwareID"))
				throw new IOException();
		} catch (Exception e) {
			// Bad request
			return response(400, "Bad request");
		}

		// Verify version
		if (!request.get("clientApiVersion").getAsString().equals(EdgeGridApiServer.GRID_API_VERSION))
			return response(409, "Conflict", "application/json", "{\"error\":\"api_version_mismatch\"}");
		if (!request.get("clientSoftwareID").getAsString().equals(EdgeGridApiServer.GRID_SOFTWARE_ID))
			return response(409, "Conflict", "application/json", "{\"error\":\"software_id_mismatch\"}");

		// Generate game token
		PhoenixToken jwt = new PhoenixToken();
		jwt.gameID = "nexusgrid";
		jwt.identity = new UUID(0, 0).toString();
		jwt.lastUpdate = -1;
		jwt.tokenExpiryTime = (System.currentTimeMillis() / 1000) + (60 * 60);
		jwt.tokenGenerationTime = System.currentTimeMillis() / 1000;
		jwt.tokenNotBefore = -1;
		jwt.capabilities = new String[] { "login", "refresh" };

		// Build response
		JsonObject resp = new JsonObject();
		resp.addProperty("token", jwt.toTokenString());

		// Return
		return ok("application/json", resp.toString());
	}

}
