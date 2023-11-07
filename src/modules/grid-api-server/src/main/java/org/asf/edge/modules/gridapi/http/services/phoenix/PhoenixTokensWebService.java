package org.asf.edge.modules.gridapi.http.services.phoenix;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.UUID;

import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.webservices.EdgeWebService;
import org.asf.edge.common.webservices.annotations.*;
import org.asf.edge.modules.gridapi.EdgeGridApiServer;
import org.asf.edge.modules.gridapi.identities.IdentityDef;
import org.asf.edge.modules.gridapi.utils.PhoenixToken;
import org.asf.edge.modules.gridapi.utils.TokenUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class PhoenixTokensWebService extends EdgeWebService<EdgeGridApiServer> {

	public PhoenixTokensWebService(EdgeGridApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new PhoenixTokensWebService(getServerInstance());
	}

	@Override
	public String path() {
		return "/tokens";
	}

	@Function(value = "gentoken", allowedMethods = { "POST" })
	public FunctionResult generateToken(FunctionInfo func) throws NoSuchAlgorithmException, IOException {
		// Load token
		TokenUtils.AccessContext ctx = TokenUtils.fromFunction(func, "gen");
		if (ctx == null) {
			return response(401, "Unauthorized");
		}

		// Load payload
		JsonObject payload;
		try {
			if (!func.getRequest().hasRequestBody())
				return response(400, "Bad request");
			payload = JsonParser.parseString(func.getRequest().getRequestBodyAsString()).getAsJsonObject();
		} catch (Exception e) {
			return response(400, "Bad request");
		}

		// Parse payload
		if (!payload.has("identity") || !payload.has("expiry") || !payload.has("capabilities")
				|| !payload.get("capabilities").isJsonArray()
				|| (payload.has("payload") && !payload.get("payload").isJsonObject()))
			return response(400, "Bad request");

		// Find identity
		String targetId = payload.get("identity").getAsString();

		// Find identity
		long lastUpdateTime = -1;
		IdentityDef id = ctx.identity;
		if (id != null)
			lastUpdateTime = id.lastUpdateTime;
		else if (ctx.account != null) {
			AccountObject acc = ctx.account;
			AccountKvDataContainer data = acc.getAccountKeyValueContainer().getChildContainer("accountdata");
			if (!data.entryExists("last_update"))
				data.setEntry("last_update", new JsonPrimitive(System.currentTimeMillis()));
			lastUpdateTime = data.getEntry("last_update").getAsLong();
		} else
			return response(404, "Not found");

		// Build token
		PhoenixToken token = new PhoenixToken();
		token.gameID = "nexusgrid";
		token.identity = targetId;
		token.lastUpdate = lastUpdateTime;
		token.tokenExpiryTime = payload.get("expiry").getAsLong();
		token.tokenNotBefore = (System.currentTimeMillis() / 1000) + 5;
		token.tokenGenerationTime = System.currentTimeMillis() / 1000;
		ArrayList<String> caps = new ArrayList<String>();
		for (JsonElement ele : payload.get("capabilities").getAsJsonArray()) {
			if (!ele.isJsonPrimitive()) {
				return response(400, "Bad Request");
			} else {
				String cap = ele.getAsString();
				if (cap.equalsIgnoreCase("master") || cap.equalsIgnoreCase("login") || cap.equalsIgnoreCase("play")) {
					return response(400, "Bad Request");
				}
				caps.add(cap);
			}
		}
		token.capabilities = caps.toArray(t -> new String[t]);
		if (payload.has("payload"))
			token.payload = payload.get("payload").getAsJsonObject();

		// Set response
		return ok("application/jwt", token.toTokenString());
	}

	@Function(value = "refresh")
	public FunctionResult refreshToken(FunctionInfo func) throws NoSuchAlgorithmException, IOException {
		// Load token
		TokenUtils.AccessContext ctx = TokenUtils.fromFunction(func, "refresh");
		if (ctx == null) {
			return response(401, "Unauthorized");
		}
		PhoenixToken jwt = ctx.token;

		// Check significant fields
		if (!jwt.identity.equals(new UUID(0, 0).toString()) && jwt.payload != null && jwt.payload.isJsonObject()
				&& jwt.payload.getAsJsonObject().has("isfr") && jwt.payload.getAsJsonObject().has("isfn")) {
			try {
				// Identity-based token
				if (ctx.account != null) {
					// Load data
					AccountKvDataContainer data = ctx.account.getAccountKeyValueContainer().getChildContainer("accountdata");
					if (!data.entryExists("significantFieldRandom"))
						return response(401, "Unauthorized");
					if (!data.entryExists("significantFieldNumber"))
						return response(401, "Unauthorized");

					// Check fields
					long isfn = data.getEntry("significantFieldNumber").getAsLong();
					int isfr = data.getEntry("significantFieldRandom").getAsInt();
					if (isfn != jwt.payload.getAsJsonObject().get("isfn").getAsLong()
							|| isfr != jwt.payload.getAsJsonObject().get("isfr").getAsInt()) {
						return response(401, "Unauthorized");
					}
				} else if (ctx.identity != null) {
					// Check fields
					if (ctx.identity.significantFieldNumber != jwt.payload.getAsJsonObject().get("isfn").getAsLong()
							|| ctx.identity.significantFieldRandom != jwt.payload.getAsJsonObject().get("isfr")
									.getAsInt()) {
						return response(401, "Unauthorized");
					}
				} else
					return response(401, "Unauthorized");
			} catch (IOException e) {
				return response(401, "Unauthorized");
			}
		}

		// Refresh
		if (jwt.tokenExpiryTime != -1) {
			long time = jwt.tokenExpiryTime - jwt.tokenGenerationTime;
			jwt.tokenExpiryTime = (System.currentTimeMillis() / 1000) + time;
		}

		// Set response
		return ok("application/jwt", jwt.toTokenString());
	}

}
