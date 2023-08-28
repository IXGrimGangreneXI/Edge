package org.asf.edge.modules.gridapi.http.services;

import java.io.IOException;
import java.util.UUID;

import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionResult;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.modules.gridapi.EdgeGridApiServer;
import org.asf.edge.modules.gridapi.http.services.phoenix.PhoenixAuthWebService;
import org.asf.edge.modules.gridapi.identities.IdentityDef;
import org.asf.edge.modules.gridapi.utils.IdentityUtils;
import org.asf.edge.modules.gridapi.utils.PhoenixToken;
import org.asf.edge.modules.gridapi.utils.TokenUtils;
import org.asf.edge.modules.gridapi.utils.TokenUtils.AccessContext;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class GridGameplayWebService extends EdgeWebService<EdgeGridApiServer> {

	public GridGameplayWebService(EdgeGridApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new GridGameplayWebService(getServerInstance());
	}

	@Override
	public String path() {
		return "/grid/gameplay";
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult authenticatePlayer(FunctionInfo func) throws IOException {
		// Load token
		AccessContext ctx = TokenUtils.fromFunction(func, "host");
		if (ctx == null || !ctx.identity.properties.containsKey("serverCertificateProperties")) {
			return response(401, "Unauthorized");
		}

		// Check if the owner is not host-banned
		IdentityDef serverDef = ctx.identity;
		if (!serverDef.properties.get("owner").value.equals(new UUID(0, 0).toString())) {
			// Attempt to find account
			AccountObject acc = AccountManager.getInstance().getAccount(serverDef.properties.get("owner").value);
			if (acc != null) {
				AccountDataContainer data = acc.getAccountData().getChildContainer("accountdata");
				if (data.entryExists("hostBanned") && data.getEntry("hostBanned").getAsBoolean()) {
					// Banned from hosting
					return response(401, "Unauthorized");
				}
			} else {
				// Attempt to find identity
				IdentityDef ownerDef = IdentityUtils.getIdentity(serverDef.properties.get("owner").value);
				if (ownerDef != null && ownerDef.properties.containsKey("hostBanned")
						&& ownerDef.properties.get("hostBanned").value.equals("true")) {
					// Banned from hosting
					return response(401, "Unauthorized");
				} else if (ownerDef == null) {
					// Owner was deleted
					IdentityUtils.deleteIdentity(serverDef.identity);
					return response(401, "Unauthorized");
				}
			}
		}

		// Read payload
		if (!getRequest().hasRequestBody())
			return response(400, "Bad request");
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("secret") || !payload.get("secret").isJsonPrimitive()) {
			return response(400, "Bad request");
		}

		// Find ID
		String playerID = PhoenixAuthWebService.authenticatePlayer(payload.get("secret").getAsString(),
				ctx.identity.identity);
		if (playerID == null) {
			// Failed
			return response(400, "Bad Request", "application/json", "{\"error\":\"invalid_secret\"}");
		}

		// Find account
		AccountObject account = AccountManager.getInstance().getAccount(playerID);
		if (account == null) {
			// Failed
			return response(400, "Bad Request", "application/json", "{\"error\":\"invalid_secret\"}");
		}

		// Load
		AccountDataContainer data = account.getAccountData().getChildContainer("accountdata");
		if (!data.entryExists("last_update"))
			data.setEntry("last_update", new JsonPrimitive(System.currentTimeMillis()));

		// Generate response
		JsonObject response = new JsonObject();
		PhoenixToken jwt = new PhoenixToken();
		jwt.gameID = "nexusgrid";
		jwt.identity = playerID;
		jwt.lastUpdate = data.getEntry("last_update").getAsLong();
		jwt.tokenExpiryTime = (System.currentTimeMillis() / 1000) + (60 * 60);
		jwt.tokenNotBefore = -1;
		jwt.tokenGenerationTime = System.currentTimeMillis() / 1000;
		jwt.capabilities = new String[] { "serveraccess", "gpdata" };
		JsonObject pl = new JsonObject();
		pl.addProperty("isfr", ctx.identity.significantFieldRandom);
		pl.addProperty("isfn", ctx.identity.significantFieldNumber);
		pl.addProperty("sid", serverDef.identity);
		response.addProperty("token", jwt.toTokenString());
		response.addProperty("accountID", account.getAccountID());
		return ok("application/json", response.toString());
	}

	@Function(value = "gridjoinserver", allowedMethods = { "POST" })
	public FunctionResult joinServer(FunctionInfo func) throws IOException {
		// Load token
		TokenUtils.AccessContext ctx = TokenUtils.fromFunction(func, "play");
		if (ctx == null || ctx.token.payload == null || !ctx.token.payload.isJsonObject()
				|| !ctx.token.payload.getAsJsonObject().has("isfr") || !ctx.token.payload.getAsJsonObject().has("isfn")
				|| !ctx.isAccount) {
			return response(401, "Unauthorized", "application/json", "{\"error\":\"token_invalid\"}");
		}
		PhoenixToken tkn = ctx.token;

		// Check significant fields
		try {
			// Load data
			AccountDataContainer data = ctx.account.getAccountData().getChildContainer("accountdata");
			if (!data.entryExists("significantFieldRandom"))
				return response(401, "Unauthorized", "application/json", "{\"error\":\"token_invalid\"}");
			if (!data.entryExists("significantFieldNumber"))
				return response(401, "Unauthorized", "application/json", "{\"error\":\"token_invalid\"}");

			// Check fields
			long isfn = data.getEntry("significantFieldNumber").getAsLong();
			int isfr = data.getEntry("significantFieldRandom").getAsInt();
			if (isfn != tkn.payload.getAsJsonObject().get("isfn").getAsLong()
					|| isfr != tkn.payload.getAsJsonObject().get("isfr").getAsInt()) {
				return response(401, "Unauthorized", "application/json", "{\"error\":\"token_invalid\"}");
			}
		} catch (IOException e) {
			return response(401, "Unauthorized", "application/json", "{\"error\":\"token_invalid\"}");
		}

		// Check token
		long isfn = tkn.payload.getAsJsonObject().get("isfn").getAsLong();
		int isfr = tkn.payload.getAsJsonObject().get("isfr").getAsInt();
		if (isfn != ctx.identity.significantFieldNumber || isfr != ctx.identity.significantFieldRandom)
			return response(401, "Unauthorized", "application/json", "{\"error\":\"token_invalid\"}");

		// Load payload
		JsonObject payload = new JsonObject();
		try {
			if (!func.getRequest().hasRequestBody())
				return response(400, "Bad Request");
			payload = JsonParser.parseString(func.getRequest().getRequestBodyAsString()).getAsJsonObject();
		} catch (Exception e) {
			return response(400, "Bad Request", "application/json", "{\"error\":\"missing_payload\"}");
		}
		if (!payload.has("serverID"))
			return response(400, "Bad Request", "application/json", "{\"error\":\"invalid_payload\"}");

		// Check server ID
		IdentityDef serverDef = IdentityUtils.getIdentity(payload.get("serverID").getAsString());
		if (serverDef == null || !serverDef.properties.containsKey("serverCertificateProperties"))
			return response(404, "Not found", "application/json", "{\"error\":\"invalid_server\"}");

		// Load
		AccountDataContainer data = ctx.account.getAccountData().getChildContainer("accountdata");
		if (!data.entryExists("last_update"))
			data.setEntry("last_update", new JsonPrimitive(System.currentTimeMillis()));

		// Assign server ID
		data.setEntry("current_server", new JsonPrimitive(serverDef.identity));

		// Generate authentication JTW
		PhoenixToken jwt = new PhoenixToken();
		jwt.gameID = "nexusgrid";
		jwt.identity = ctx.identity.identity;
		jwt.lastUpdate = serverDef.lastUpdateTime;
		jwt.tokenExpiryTime = (System.currentTimeMillis() / 1000) + 15;
		jwt.tokenNotBefore = -1;
		jwt.tokenGenerationTime = System.currentTimeMillis() / 1000;
		jwt.capabilities = new String[] { "srvaurc" };
		JsonObject pl = new JsonObject();
		pl.addProperty("isfr", isfr);
		pl.addProperty("isfn", isfn);
		pl.addProperty("sid", serverDef.identity);
		jwt.payload = pl;

		// Set response
		JsonObject response = new JsonObject();
		response.addProperty("secret", jwt.toTokenString());
		return ok("application/json", response.toString());
	}

}
