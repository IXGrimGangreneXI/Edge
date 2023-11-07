package org.asf.edge.modules.gridapi.http.services.phoenix;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.events.accounts.AccountAuthenticatedEvent;
import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.webservices.EdgeWebService;
import org.asf.edge.common.webservices.annotations.Function;
import org.asf.edge.common.webservices.annotations.FunctionInfo;
import org.asf.edge.common.webservices.annotations.FunctionResult;
import org.asf.edge.modules.gridapi.EdgeGridApiServer;
import org.asf.edge.modules.gridapi.events.auth.AuthenticationDeferredEvent;
import org.asf.edge.modules.gridapi.events.auth.AuthenticationFailedEvent;
import org.asf.edge.modules.gridapi.events.auth.AuthenticationSuccessEvent;
import org.asf.edge.modules.gridapi.events.auth.GridAuthenticateEvent;
import org.asf.edge.modules.gridapi.identities.IdentityDef;
import org.asf.edge.modules.gridapi.utils.*;
import org.asf.edge.modules.gridapi.utils.TokenUtils.AccessContext;
import org.asf.nexus.events.EventBus;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class PhoenixAuthWebService extends EdgeWebService<EdgeGridApiServer> {

	public PhoenixAuthWebService(EdgeGridApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new PhoenixAuthWebService(getServerInstance());
	}

	@Override
	public String path() {
		return "/auth";
	}

	/**
	 * Authenticates a player
	 * 
	 * @param secret   Server authentication secret
	 * @param serverID Server ID
	 * @return Player ID or null
	 */
	public static String authenticatePlayer(String secret, String serverID) {
		// Verify secret
		try {
			// Parse token
			PhoenixToken jwt = new PhoenixToken();
			if (jwt.parseToken(secret) != TokenParseResult.SUCCESS || !jwt.hasCapability("srvaurc"))
				return null;

			// Verify other fields
			// Check server ID
			if (jwt.payload == null || !jwt.payload.getAsJsonObject().get("sid").getAsString().equals(serverID))
				return null;

			// Find player ID
			AccountObject playerDef = AccountManager.getInstance().getAccount(jwt.identity);
			if (playerDef == null)
				return null;

			// Check significant fields
			long isfn = jwt.payload.getAsJsonObject().get("isfn").getAsLong();
			int isfr = jwt.payload.getAsJsonObject().get("isfr").getAsInt();
			AccountKvDataContainer data = playerDef.getAccountKeyValueContainer().getChildContainer("accountdata");
			if (isfn != data.getEntry("significantFieldNumber").getAsLong()
					|| isfr != data.getEntry("significantFieldRandom").getAsInt())
				return null;

			// Success
			return playerDef.getAccountID();
		} catch (Exception e) {
			// Invalid token
		}
		return null;
	}

	@Function(value = "joinserver", allowedMethods = { "POST" })
	public FunctionResult joinServer(FunctionInfo func) throws IOException {
		// Load token
		TokenUtils.AccessContext ctx = TokenUtils.fromFunction(func, "play");
		if (ctx == null || ctx.token.payload == null || !ctx.token.payload.isJsonObject()
				|| !ctx.token.payload.getAsJsonObject().has("isfr")
				|| !ctx.token.payload.getAsJsonObject().has("isfn")) {
			return response(401, "Unauthorized", "application/json", "{\"error\":\"token_invalid\"}");
		}
		PhoenixToken tkn = ctx.token;

		// Check significant fields
		try {
			// Identity-based token
			if (ctx.account != null) {
				// Load data
				AccountKvDataContainer data = ctx.account.getAccountKeyValueContainer().getChildContainer("accountdata");
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
			} else if (ctx.identity != null) {
				// Check fields
				if (ctx.identity.significantFieldNumber != tkn.payload.getAsJsonObject().get("isfn").getAsLong()
						|| ctx.identity.significantFieldRandom != tkn.payload.getAsJsonObject().get("isfr")
								.getAsInt()) {
					return response(401, "Unauthorized", "application/json", "{\"error\":\"token_invalid\"}");
				}
			} else
				return response(401, "Unauthorized", "application/json", "{\"error\":\"token_invalid\"}");
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

	@Function(value = "authenticate", allowedMethods = { "POST" })
	public FunctionResult authenticate(FunctionInfo func) throws NoSuchAlgorithmException, IOException {
		// Load token
		TokenUtils.AccessContext ctx = TokenUtils.fromFunction(func, "login");
		if (ctx == null) {
			return response(401, "Unauthorized", "application/json", "{\"error\":\"token_invalid\"}");
		}

		// Find account manager
		AccountManager manager = AccountManager.getInstance();

		// Load payload
		JsonObject payload = new JsonObject();
		try {
			if (!func.getRequest().hasRequestBody())
				return response(400, "Bad Request");
			payload = JsonParser.parseString(func.getRequest().getRequestBodyAsString()).getAsJsonObject();
		} catch (Exception e) {
			return response(400, "Bad Request");
		}

		// Check payload
		if (!payload.has("mode")) {
			// Generate response
			JsonObject response = new JsonObject();
			response.addProperty("status", "deferred");
			response.addProperty("dataRequestKey", "mode");
			response.addProperty("error", "invalid_mode");
			response.addProperty("errorMessage",
					"Missing 'mode' field, expecting 'refreshtoken' or 'usernamepassword'");
			return response(400, "Derred", "application/json", response.toString());
		} else if (!payload.get("mode").getAsString().equals("refreshtoken")
				&& !payload.get("mode").getAsString().equals("usernamepassword")) {
			// Generate response
			JsonObject response = new JsonObject();
			response.addProperty("status", "failure");
			response.addProperty("error", "invalid_mode");
			response.addProperty("errorMessage",
					"Invalid 'mode' field, expecting 'refreshtoken' or 'usernamepassword'");
			return response(401, "Unauthorized", "application/json", response.toString());
		}

		// Check mode
		AccountObject account = null;
		JsonObject response = new JsonObject();
		boolean byRefreshToken = payload.get("mode").getAsString().equals("refreshtoken");
		if (byRefreshToken) {
			// Check payload
			if (!payload.has("token")) {
				// Generate response
				response.addProperty("status", "deferred");
				response.addProperty("dataRequestKey", "token");
				response.addProperty("error", "invalid_token");
				response.addProperty("errorMessage", "Missing 'token' field, expecting a refresh token.");
				EventBus.getInstance().dispatchEvent(new AuthenticationDeferredEvent(null, response));
				return response(400, "Derred", "application/json", response.toString());
			}

			// Load token
			String refreshToken = payload.get("token").getAsString();
			AccessContext plrCtx = TokenUtils.fromToken(refreshToken, "playerrefreshlogin");
			if (plrCtx == null || !plrCtx.isAccount) {
				// Generate response
				response.addProperty("status", "failure");
				response.addProperty("error", "invalid_token");
				response.addProperty("errorMessage", "Refresh token is invalid");
				EventBus.getInstance().dispatchEvent(new AuthenticationFailedEvent(null, response));
				return response(401, "Unauthorized", "application/json", response.toString());
			}
			account = plrCtx.account;

			// Generate response
			response.addProperty("status", "success");
		} else {
			// Check payload
			if (!payload.has("username") || !payload.has("password")) {
				// Generate response
				response.addProperty("status", "deferred");
				if (!payload.has("username"))
					response.addProperty("dataRequestKey", "username");
				else if (!payload.has("password"))
					response.addProperty("dataRequestKey", "password");
				response.addProperty("error", "invalid_credentials");
				response.addProperty("errorMessage", "Missing 'username' and/or 'password' field.");
				EventBus.getInstance().dispatchEvent(new AuthenticationDeferredEvent(null, response));
				return response(400, "Derred", "application/json", response.toString());
			}

			// Load
			String username = payload.get("username").getAsString();
			String password = payload.get("password").getAsString();

			// Find account
			if (!manager.isValidUsername(username)) {
				// Invalid username
				return invalidUserCallback(username, null);
			}
			if (!manager.isUsernameTaken(username)) {
				// Log
				getServerInstance().getLogger().warn("Grid login from IP " + func.getClient().getRemoteAddress()
						+ " rejected for " + username + ": account not found");

				// Account not found
				return invalidUserCallback(username, null);
			}

			// Retrieve ID
			String id = manager.getAccountID(username);

			// Check ID
			if (!manager.accountExists(id)) {
				// Log
				getServerInstance().getLogger().warn("Grid login from IP " + func.getClient().getRemoteAddress()
						+ " rejected for " + id + ": account not found");

				// ID does not exist
				return invalidUserCallback(username, null);
			}

			// Find account
			AccountObject acc = manager.getAccount(id);
			if (acc.isGuestAccount()) {
				// Log
				getServerInstance().getLogger().warn("Grid login from IP " + func.getClient().getRemoteAddress()
						+ " rejected for " + acc.getAccountID() + ": guest accounts may not be directly logged in on");

				// NO
				return invalidUserCallback(username, acc);
			}

			// Return invalid if the username is on cooldown
			JsonElement lock = acc.getAccountKeyValueContainer().getChildContainer("accountdata").getEntry("lockedsince");
			if (lock != null && (System.currentTimeMillis() - lock.getAsLong()) < 8000) {
				// Log
				getServerInstance().getLogger().warn("Grid login from IP " + func.getClient().getRemoteAddress()
						+ " rejected for " + acc.getAccountID() + ": login rejected by antibruteforce");

				// Locked
				return response(401, "Unauthorized", "text/json", "{\"error\":\"invalid_credentials\"}");
			}

			// Password check
			if (!manager.verifyPassword(id, password)) {
				// Log
				getServerInstance().getLogger().warn("Grid login from IP " + func.getClient().getRemoteAddress()
						+ " rejected for " + id + ": invalid password");

				// Password incorrect
				return invalidUserCallback(username, acc);
			}

			// Apply
			account = acc;

			// Generate response
			response.addProperty("status", "success");

			// Dispatch event
			GridAuthenticateEvent ev = new GridAuthenticateEvent(account, payload, response);
			EventBus.getInstance().dispatchEvent(ev);
		}

		// Update time
		if (account != null)
			account.updateLastLoginTime();

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new AccountAuthenticatedEvent(account, manager));

		// Handle status
		if (!response.get("status").getAsString().equals("success")) {

			// Handle response
			switch (response.get("status").getAsString()) {

			case "failure": {
				// Generate response
				response.addProperty("status", "failure");
				return response(401, "Unauthorized", "application/json", response.toString());
			}

			case "deferred": {
				// Generate response
				response.addProperty("status", "deferred");
				return response(400, "Derred", "application/json", response.toString());
			}

			// Error
			default: {
				// Generate response
				getServerInstance().getLogger().error("A module returned invalid response type '"
						+ response.get("status").getAsString()
						+ "' in response to /auth/auhtenticate, expected either 'success', 'failure' or 'deferred'");
				response.addProperty("status", "failure");
				response.addProperty("error", "internal_error");
				response.addProperty("errorMessage", "An internal server error occurred!");
				return response(401, "Unauthorized", "application/json", response.toString());
			}

			}
		}

		// Success
		AccountKvDataContainer data = account.getAccountKeyValueContainer().getChildContainer("accountdata");
		data.setEntry("significantFieldRandom", new JsonPrimitive(IdentityUtils.rnd.nextInt()));
		data.setEntry("significantFieldNumber", new JsonPrimitive(System.currentTimeMillis()));
		if (!data.entryExists("last_update"))
			data.setEntry("last_update", new JsonPrimitive(System.currentTimeMillis()));

		// Generate game token
		PhoenixToken jwt = new PhoenixToken();
		jwt.gameID = "nexusgrid";
		jwt.identity = account.getAccountID();
		jwt.lastUpdate = data.getEntry("last_update").getAsLong();
		jwt.tokenExpiryTime = (System.currentTimeMillis() / 1000) + (60 * 60);
		jwt.tokenGenerationTime = System.currentTimeMillis() / 1000;
		jwt.tokenNotBefore = -1;
		jwt.capabilities = new String[] { "play", "refresh", "accprops", "gpdata" };
		JsonObject pl = new JsonObject();
		pl.addProperty("isfr", data.getEntry("significantFieldRandom").getAsLong());
		pl.addProperty("isfn", data.getEntry("significantFieldNumber").getAsLong());
		jwt.payload = pl;

		// Generate refresh token
		PhoenixToken refresh = new PhoenixToken();
		refresh.gameID = "nexusgrid";
		refresh.identity = account.getAccountID();
		refresh.lastUpdate = data.getEntry("last_update").getAsLong();
		refresh.tokenExpiryTime = (System.currentTimeMillis() / 1000) + (30 * 24 * 60 * 60);
		refresh.tokenGenerationTime = System.currentTimeMillis() / 1000;
		refresh.tokenNotBefore = -1;
		refresh.capabilities = new String[] { "playerrefreshlogin" };

		// Add properties
		response.addProperty("accountID", account.getAccountID());
		response.addProperty("displayName", account.getUsername());
		response.addProperty("sessionToken", jwt.toTokenString());
		response.addProperty("refreshToken", refresh.toTokenString());
		EventBus.getInstance().dispatchEvent(new AuthenticationSuccessEvent(account, response));
		return ok("application/json", response.toString());
	}

	private FunctionResult invalidUserCallback(String username, AccountObject account) throws IOException {
		if (account != null)
			account.getAccountKeyValueContainer().getChildContainer("accountdata").setEntry("lockedsince",
					new JsonPrimitive(System.currentTimeMillis()));
		try {
			Thread.sleep(8000);
		} catch (InterruptedException e) {
		}
		// Generate response
		JsonObject response = new JsonObject();
		response.addProperty("status", "failure");
		response.addProperty("error", "invalid_credentials");
		response.addProperty("errorMessage", "Credentials are invalid");
		EventBus.getInstance().dispatchEvent(new AuthenticationFailedEvent(account, response));
		return response(401, "Unauthorized", "application/json", response.toString());
	}

}
