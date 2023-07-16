package org.asf.edge.gameplayapi.http.handlers.edgespecific;

import java.io.IOException;

import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionResult;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.common.services.textfilter.result.FilterResult;
import org.asf.edge.common.services.textfilter.result.WordMatch;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.gameplayapi.EdgeGameplayApiServer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * 
 * Edge API endpoint, holds public edge-specific API features
 * 
 * @author Sky Swimmer
 *
 */
public class EdgeApiService extends EdgeWebService<EdgeGameplayApiServer> {

	public EdgeApiService(EdgeGameplayApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new EdgeApiService(getServerInstance());
	}

	@Override
	public String path() {
		return "/API/ProjectEdge/";
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult authenticate(FunctionInfo func) throws IOException {
		// Simple authentication

		// Load request
		JsonObject request;
		try {
			// Parse and check
			request = JsonParser.parseString(func.getRequest().getRequestBodyAsString()).getAsJsonObject();
			if (!request.has("username") || !request.has("password"))
				throw new IOException();
		} catch (Exception e) {
			// Bad request
			return response(400, "Bad request");
		}

		// Pull username and password from request
		String username = request.get("username").getAsString();
		String password = request.get("password").getAsString();

		// Find account manager
		AccountManager manager = AccountManager.getInstance();

		// Find account
		if (!manager.isValidUsername(username)) {
			// Invalid username
			return invalidUserCallback(username, null);
		}
		if (!manager.isUsernameTaken(username)) {
			// Log
			getServerInstance().getLogger().warn("API login from IP " + func.getClient().getRemoteAddress()
					+ " rejected for " + username + ": account not found");

			// Account not found
			return invalidUserCallback(username, null);
		}

		// Retrieve ID
		String id = manager.getAccountID(username);

		// Check ID
		if (!manager.accountExists(id)) {
			// Log
			getServerInstance().getLogger().warn("API login from IP " + func.getClient().getRemoteAddress()
					+ " rejected for " + id + ": account not found");

			// ID does not exist
			return invalidUserCallback(username, null);
		}

		// Find account
		AccountObject acc = manager.getAccount(id);
		if (acc.isGuestAccount()) {
			// Log
			getServerInstance().getLogger().warn("API login from IP " + func.getClient().getRemoteAddress()
					+ " rejected for " + acc.getAccountID() + ": guest accounts may not be directly logged in on");

			// NO
			return invalidUserCallback(username, acc);
		}

		// Return invalid if the username is on cooldown
		JsonElement lock = acc.getAccountData().getChildContainer("accountdata").getEntry("lockedsince");
		if (lock != null && (System.currentTimeMillis() - lock.getAsLong()) < 8000) {
			// Log
			getServerInstance().getLogger().warn("API login from IP " + func.getClient().getRemoteAddress()
					+ " rejected for " + acc.getAccountID() + ": login rejected by antibruteforce");

			// Locked
			return response(401, "Unauthorized", "text/json", "{\"error\":\"invalid_credentials\"}");
		}

		// Password check
		if (!manager.verifyPassword(id, password)) {
			// Log
			getServerInstance().getLogger().warn("API login from IP " + func.getClient().getRemoteAddress()
					+ " rejected for " + id + ": invalid password");

			// Password incorrect
			return invalidUserCallback(username, acc);
		}

		// Create session token
		SessionToken tkn = new SessionToken();
		tkn.accountID = acc.getAccountID();
		tkn.lastLoginTime = acc.getLastLoginTime();
		tkn.capabilities = new String[] { "api" };

		// Log
		getServerInstance().getLogger().info("API login from IP " + func.getClient().getRemoteAddress() + " to "
				+ acc.getAccountID() + ": logged in as " + acc.getUsername());

		// Create response
		String token = getUtilities().encodeToken(tkn.toTokenString());
		acc.ping(true);

		// Build response
		JsonObject resp = new JsonObject();
		resp.addProperty("token", token);
		resp.addProperty("accountId", acc.getAccountID());
		resp.addProperty("accountUsername", acc.getUsername());
		resp.addProperty("lastLoginTime", acc.getLastLoginTime());
		resp.addProperty("registrationTime", acc.getRegistrationTimestamp());
		resp.addProperty("multiplayerEnabled", acc.isMultiplayerEnabled());
		resp.addProperty("chatEnabled", acc.isChatEnabled());
		resp.addProperty("strictChat", acc.isStrictChatFilterEnabled());

		// Add profiles
		JsonObject profiles = new JsonObject();
		for (String save : acc.getSaveIDs()) {
			AccountSaveContainer sv = acc.getSave(save);
			if (sv != null) {
				JsonObject profile = new JsonObject();
				profile.addProperty("username", sv.getUsername());
				profile.addProperty("creationTime", sv.getCreationTime());
				profiles.add(save, profile);
			}
		}
		resp.add("profiles", profiles);

		// Update login time
		acc.updateLastLoginTime();

		// Return
		return ok("text/json", resp.toString());
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult textFilter(FunctionInfo func) {
		// Edge text filter

		// Load request
		JsonObject request;
		try {
			// Parse and check
			request = JsonParser.parseString(func.getRequest().getRequestBodyAsString()).getAsJsonObject();
			if (!request.has("message") || !request.has("strictMode"))
				throw new IOException();
		} catch (Exception e) {
			// Bad request
			return response(400, "Bad request");
		}

		// Run filter
		String message = request.get("message").getAsString();
		boolean strictMode = request.get("strictMode").getAsBoolean();
		FilterResult result = TextFilterService.getInstance().filter(message, strictMode);

		// Build response match list
		JsonArray matches = new JsonArray();
		for (WordMatch match : result.getMatches()) {
			JsonObject m = new JsonObject();
			m.addProperty("phrase", match.getPhraseFilter().getPhrase());
			m.addProperty("matchedPhrase", match.getMatchedPhrase());
			m.addProperty("reason", match.getReason());
			m.addProperty("severity", match.getSeverity().toString());
			matches.add(m);
		}

		// Build response
		JsonObject res = new JsonObject();
		res.addProperty("isFiltered", result.isMatch());
		res.addProperty("resultMessage", result.getFilterResult());
		res.addProperty("resultSeverity", result.getSeverity().toString());
		res.add("matches", matches);

		// Return result
		return ok("text/json", res.toString());
	}

	private FunctionResult invalidUserCallback(String username, AccountObject account) throws IOException {
		if (account != null)
			account.getAccountData().getChildContainer("accountdata").setEntry("lockedsince",
					new JsonPrimitive(System.currentTimeMillis()));
		try {
			Thread.sleep(8000);
		} catch (InterruptedException e) {
		}
		return response(401, "Unauthorized", "text/json", "{\"error\":\"invalid_credentials\"}");
	}

}
