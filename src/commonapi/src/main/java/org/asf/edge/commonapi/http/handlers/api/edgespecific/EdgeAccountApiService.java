package org.asf.edge.commonapi.http.handlers.api.edgespecific;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.stream.Stream;

import org.asf.connective.processors.HttpPushProcessor;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.common.entities.achivements.EntityRankInfo;
import org.asf.edge.common.events.accounts.AccountAuthenticatedEvent;
import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.achievements.AchievementManager;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.common.webservices.EdgeWebService;
import org.asf.edge.common.webservices.annotations.Function;
import org.asf.edge.common.webservices.annotations.FunctionInfo;
import org.asf.edge.common.webservices.annotations.FunctionResult;
import org.asf.edge.commonapi.EdgeCommonApiServer;
import org.asf.nexus.events.EventBus;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * 
 * Edge API endpoint for account management, holds public edge-specific API
 * features for accounts
 * 
 * @author Sky Swimmer
 *
 */
public class EdgeAccountApiService extends EdgeWebService<EdgeCommonApiServer> {

	public EdgeAccountApiService(EdgeCommonApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new EdgeAccountApiService(getServerInstance());
	}

	@Override
	public String path() {
		return "/API/ProjectEdge/Accounts/";
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult registerAccount(FunctionInfo func) throws IOException {
		// Register accounts

		// Load request
		JsonObject request;
		try {
			// Parse and check
			request = JsonParser.parseString(func.getRequest().getRequestBodyAsString()).getAsJsonObject();
			if (!request.has("email") || !request.has("username") || !request.has("password")
					|| !request.has("subscribeEmailNotifications") || !request.has("isUnderageUser"))
				throw new IOException();
		} catch (Exception e) {
			// Bad request
			return response(400, "Bad request", "text/json",
					"{\"error\":\"Invalid request body, expecting a JSON with a 'email', 'username', 'password', 'subscribeEmailNotifications' and 'isUnderageUser' field\"}");
		}

		// Load request into memory
		String email = request.get("email").getAsString();
		String username = request.get("username").getAsString();
		String password = request.get("password").getAsString();
		boolean subscribeEmail = request.get("subscribeEmailNotifications").getAsBoolean();
		boolean isUnderageUser = request.get("isUnderageUser").getAsBoolean();

		// Find account manager
		AccountManager manager = AccountManager.getInstance();

		// Check username validity
		if (!manager.isValidUsername(username)) {
			// Error
			return response(400, "Bad request", "text/json", "{\"error\":\"invalid_username\"}");
		}

		// Check filters
		if (TextFilterService.getInstance().isFiltered(username, true)) {
			// Error
			return response(400, "Bad request", "text/json", "{\"error\":\"inappropriate_username\"}");
		}

		// Check password validity
		if (!manager.isValidPassword(password)) {
			// Error
			return response(400, "Bad request", "text/json", "{\"error\":\"invalid_password\"}");
		}

		// Verify email
		if (!email.toLowerCase().matches(
				"(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")) {
			// Error
			return response(400, "Bad request", "text/json", "{\"error\":\"invalid_email\"}");
		}

		// Check if email is taken
		if (manager.getAccountIDByEmail(email) != null) {
			// Error
			return response(400, "Bad request", "text/json", "{\"error\":\"email_in_use\"}");
		}

		// Check if name is taken
		if (manager.isUsernameTaken(username)) {
			// Error
			return response(400, "Bad request", "text/json", "{\"error\":\"username_in_use\"}");
		}

		// Create account
		AccountObject acc = manager.registerAccount(username, email, password.toCharArray());
		if (acc == null) {
			// Error
			return response(500, "Bad request", "text/json", "{\"error\":\"server_error\"}");
		}

		// Set data
		AccountKvDataContainer cont = acc.getAccountKeyValueContainer().getChildContainer("accountdata");
		cont.setEntry("sendupdates", new JsonPrimitive(subscribeEmail));
		cont.setEntry("isunderage", new JsonPrimitive(isUnderageUser));
		if (isUnderageUser) {
			acc.setChatEnabled(false);
			acc.setStrictChatFilterEnabled(true);
		}

		// Add gems
		AccountKvDataContainer currencyAccWide = acc.getAccountKeyValueContainer().getChildContainer("currency");
		int current = 0;
		if (currencyAccWide.entryExists("gems"))
			current = currencyAccWide.getEntry("gems").getAsInt();
		currencyAccWide.setEntry("gems", new JsonPrimitive(current + 75));

		// Create session token
		SessionToken tkn = new SessionToken();
		tkn.accountID = acc.getAccountID();
		tkn.lastLoginTime = acc.getLastLoginTime();
		tkn.capabilities = new String[] { "api" };

		// Log
		getServerInstance().getLogger().info("API account registration from IP " + func.getClient().getRemoteAddress()
				+ ": registered " + acc.getAccountID() + " (username: " + acc.getUsername() + ")");

		// Create response
		String token = tkn.toTokenString();
		acc.ping(true);

		// Build response
		JsonObject resp = new JsonObject();
		resp.addProperty("token", token);
		resp.addProperty("accountId", acc.getAccountID());
		resp.addProperty("accountUsername", acc.getUsername());
		resp.addProperty("accountEmail", acc.getAccountEmail());
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

				// Find avatar
				JsonElement ele = sv.getSaveData().getEntry("avatar");
				if (ele != null) {
					profile.add("avatar", ele);
				}

				// Rank data
				JsonArray rankData = new JsonArray();
				for (EntityRankInfo rank : AchievementManager.getInstance().getRanks(sv, save)) {
					if (rank.getRank() == null)
						continue;
					JsonObject rObj = new JsonObject();
					rObj.addProperty("rankID", rank.getRank().getID());
					rObj.addProperty("rankGlobalID", rank.getRank().getGlobalID());
					rObj.addProperty("pointTypeID", rank.getRank().getPointTypeID());
					rObj.addProperty("level", AchievementManager.getInstance().getRankIndex(rank.getRank()) + 1);
					rObj.addProperty("points", rank.getTotalScore());
					rankData.add(rObj);
				}
				profile.add("ranks", rankData);

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
			return response(400, "Bad request", "text/json",
					"{\"error\":\"Invalid request body, expecting a JSON with a 'username' and 'password' field\"}");
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
		JsonElement lock = acc.getAccountKeyValueContainer().getChildContainer("accountdata").getEntry("lockedsince");
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
		String token = tkn.toTokenString();
		acc.ping(true);

		// Build response
		JsonObject resp = new JsonObject();
		resp.addProperty("token", token);
		resp.addProperty("accountId", acc.getAccountID());
		resp.addProperty("accountUsername", acc.getUsername());
		resp.addProperty("accountEmail", acc.getAccountEmail());
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

				// Find avatar
				JsonElement ele = sv.getSaveData().getEntry("avatar");
				if (ele != null) {
					profile.add("avatar", ele);
				}

				// Rank data
				JsonArray rankData = new JsonArray();
				for (EntityRankInfo rank : AchievementManager.getInstance().getRanks(sv, save)) {
					if (rank.getRank() == null)
						continue;
					JsonObject rObj = new JsonObject();
					rObj.addProperty("rankID", rank.getRank().getID());
					rObj.addProperty("rankGlobalID", rank.getRank().getGlobalID());
					rObj.addProperty("pointTypeID", rank.getRank().getPointTypeID());
					rObj.addProperty("level", AchievementManager.getInstance().getRankIndex(rank.getRank()) + 1);
					rObj.addProperty("points", rank.getTotalScore());
					rankData.add(rObj);
				}
				profile.add("ranks", rankData);

				profile.addProperty("creationTime", sv.getCreationTime());
				profiles.add(save, profile);
			}
		}
		resp.add("profiles", profiles);

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new AccountAuthenticatedEvent(acc, manager));

		// Return
		return ok("text/json", resp.toString());
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult authenticateGuest(FunctionInfo func) throws IOException {
		// Simple authentication

		// Load request
		JsonObject request;
		try {
			// Parse and check
			request = JsonParser.parseString(func.getRequest().getRequestBodyAsString()).getAsJsonObject();
			if (!request.has("guestID"))
				throw new IOException();
		} catch (Exception e) {
			// Bad request
			return response(400, "Bad request", "text/json",
					"{\"error\":\"Invalid request body, expecting a JSON with a 'guestID' field\"}");
		}

		// Pull guest ID from request
		String guestID = request.get("guestID").getAsString();

		// Find account manager
		AccountManager manager = AccountManager.getInstance();

		// Find account
		AccountObject acc = manager.getGuestAccount(guestID);
		if (acc == null) {
			return response(404, "Guest not found", "text/json", "{\"error\":\"guest_not_found\"}");
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
		String token = tkn.toTokenString();
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

				// Find avatar
				JsonElement ele = sv.getSaveData().getEntry("avatar");
				if (ele != null) {
					profile.add("avatar", ele);
				}

				// Rank data
				JsonArray rankData = new JsonArray();
				for (EntityRankInfo rank : AchievementManager.getInstance().getRanks(sv, save)) {
					if (rank.getRank() == null)
						continue;
					JsonObject rObj = new JsonObject();
					rObj.addProperty("rankID", rank.getRank().getID());
					rObj.addProperty("rankGlobalID", rank.getRank().getGlobalID());
					rObj.addProperty("pointTypeID", rank.getRank().getPointTypeID());
					rObj.addProperty("level", AchievementManager.getInstance().getRankIndex(rank.getRank()) + 1);
					rObj.addProperty("points", rank.getTotalScore());
					rankData.add(rObj);
				}
				profile.add("ranks", rankData);

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

	@Function
	public FunctionResult deleteAccount(FunctionInfo func) throws IOException {
		// Save details

		// Check authentication
		if (!func.getRequest().hasHeader("Authorization")
				|| !func.getRequest().getHeader("Authorization").getValue().toLowerCase().startsWith("bearer ")) {
			// Error
			return response(401, "Unauthorized", "text/json", "{\"error\":\"Missing authorization token\"}");
		}

		// Read token
		String token = func.getRequest().getHeader("Authorization").getValue().substring("bearer ".length());
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(token);
		AccountObject acc = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			return response(401, "Unauthorized", "text/json", "{\"error\":\"Authorization token invalid\"}");
		}

		// Delete
		acc.deleteAccount();

		// Log
		getServerInstance().getLogger()
				.info("Account deleted: " + acc.getAccountID() + " (" + acc.getUsername() + ").");

		// Build response
		JsonObject resp = new JsonObject();
		resp.addProperty("deleted", true);

		// Return
		return ok("text/json", resp.toString());
	}

	@Function
	public FunctionResult getAccountDetails(FunctionInfo func) throws IOException {
		// Account details

		// Check authentication
		if (!func.getRequest().hasHeader("Authorization")
				|| !func.getRequest().getHeader("Authorization").getValue().toLowerCase().startsWith("bearer ")) {
			// Error
			return response(401, "Unauthorized", "text/json", "{\"error\":\"Missing authorization token\"}");
		}

		// Read token
		String token = func.getRequest().getHeader("Authorization").getValue().substring("bearer ".length());
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(token);
		AccountObject acc = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			return response(401, "Unauthorized", "text/json", "{\"error\":\"Authorization token invalid\"}");
		}

		// Build response
		JsonObject resp = new JsonObject();
		resp.addProperty("accountId", acc.getAccountID());
		resp.addProperty("accountUsername", acc.getUsername());
		resp.addProperty("accountEmail", acc.getAccountEmail());
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

				// Find avatar
				JsonElement ele = sv.getSaveData().getEntry("avatar");
				if (ele != null) {
					profile.add("avatar", ele);
				}

				// Rank data
				JsonArray rankData = new JsonArray();
				for (EntityRankInfo rank : AchievementManager.getInstance().getRanks(sv, save)) {
					if (rank.getRank() == null)
						continue;
					JsonObject rObj = new JsonObject();
					rObj.addProperty("rankID", rank.getRank().getID());
					rObj.addProperty("rankGlobalID", rank.getRank().getGlobalID());
					rObj.addProperty("pointTypeID", rank.getRank().getPointTypeID());
					rObj.addProperty("level", AchievementManager.getInstance().getRankIndex(rank.getRank()) + 1);
					rObj.addProperty("points", rank.getTotalScore());
					rankData.add(rObj);
				}
				profile.add("ranks", rankData);

				// Add
				profile.addProperty("creationTime", sv.getCreationTime());
				profiles.add(save, profile);
			}
		}
		resp.add("profiles", profiles);

		// Return
		return ok("text/json", resp.toString());
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult updateAccount(FunctionInfo func) throws IOException {
		// Save details

		// Check authentication
		if (!func.getRequest().hasHeader("Authorization")
				|| !func.getRequest().getHeader("Authorization").getValue().toLowerCase().startsWith("bearer ")) {
			// Error
			return response(401, "Unauthorized", "text/json", "{\"error\":\"Missing authorization token\"}");
		}

		// Read token
		String token = func.getRequest().getHeader("Authorization").getValue().substring("bearer ".length());
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(token);
		AccountObject acc = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			return response(401, "Unauthorized", "text/json", "{\"error\":\"Authorization token invalid\"}");
		}

		// Load request
		JsonObject request;
		try {
			// Parse and check
			request = JsonParser.parseString(func.getRequest().getRequestBodyAsString()).getAsJsonObject();
		} catch (Exception e) {
			// Bad request
			return response(400, "Bad request", "text/json", "{\"error\":\"Invalid request body\"}");
		}

		// Find account manager
		AccountManager manager = AccountManager.getInstance();

		// Update account
		String newUsername = null;
		if (request.has("accountUsername")) {
			String username = request.get("accountUsername").getAsString();

			// Check validity
			if (!manager.isValidUsername(username)) {
				// Invalid name
				return response(400, "Bad request", "text/json", "{\"error\":\"invalid_username\"}");
			}

			// Check filters
			if (TextFilterService.getInstance().isFiltered(username, true)) {
				// Invalid name
				return response(400, "Bad request", "text/json", "{\"error\":\"inappropriate_username\"}");
			}

			// Check if in use
			boolean inUse = false;
			if (!acc.getUsername().equalsIgnoreCase(username) && manager.isUsernameTaken(username)) {
				inUse = true;
			} else {
				// Check if in use by any saves
				AccountObject accF = acc;
				if (Stream.of(acc.getSaveIDs()).map(t -> accF.getSave(t)).anyMatch(t -> {
					return t.getUsername().equalsIgnoreCase(username);
				})) {
					inUse = true;
				}
			}
			if (inUse) {
				return response(400, "Bad request", "text/json", "{\"error\":\"username_in_use\"}");
			}

			// Set for update
			newUsername = username;
		}
		String newPassword = null;
		if (request.has("accountPassword")) {
			String password = request.get("accountPassword").getAsString();

			// Check password validity
			if (!manager.isValidPassword(password)) {
				// Error
				return response(400, "Bad request", "text/json", "{\"error\":\"invalid_password\"}");
			}

			// Set for update
			newPassword = password;
		}
		String newEmail = null;
		if (request.has("accountEmail")) {
			String email = request.get("accountEmail").getAsString();

			// Check if email is taken
			if (manager.getAccountIDByEmail(email) != null) {
				// Error
				return response(400, "Bad request", "text/json", "{\"error\":\"email_in_use\"}");
			}

			// Set for update
			newEmail = email;
		}
		if (request.has("multiplayerEnabled")) {
			acc.setMultiplayerEnabled(request.get("multiplayerEnabled").getAsBoolean());
		}
		if (request.has("chatEnabled")) {
			acc.setChatEnabled(request.get("chatEnabled").getAsBoolean());
		}
		if (request.has("strictChat")) {
			acc.setStrictChatFilterEnabled(request.get("strictChat").getAsBoolean());
		}

		// Update
		if (newUsername != null) {
			// Update
			if (!acc.updateUsername(newUsername))
				return response(500, "Internal server error", "text/json", "{\"error\":\"server_error\"}");
			acc.getAccountKeyValueContainer().getChildContainer("accountdata").setEntry("last_update",
					new JsonPrimitive(System.currentTimeMillis()));
		}
		if (newPassword != null) {
			// Update
			if (!acc.updatePassword(newPassword.toCharArray()))
				return response(500, "Internal server error", "text/json", "{\"error\":\"server_error\"}");
			acc.getAccountKeyValueContainer().getChildContainer("accountdata").setEntry("last_update",
					new JsonPrimitive(System.currentTimeMillis()));
		}
		if (newEmail != null) {
			// Update
			if (!acc.updateEmail(newEmail))
				return response(500, "Internal server error", "text/json", "{\"error\":\"server_error\"}");
			acc.getAccountKeyValueContainer().getChildContainer("accountdata").setEntry("last_update",
					new JsonPrimitive(System.currentTimeMillis()));
		}

		// Build response
		JsonObject resp = new JsonObject();
		resp.addProperty("accountId", acc.getAccountID());
		resp.addProperty("accountUsername", acc.getUsername());
		resp.addProperty("accountEmail", acc.getAccountEmail());
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

				// Find avatar
				JsonElement ele = sv.getSaveData().getEntry("avatar");
				if (ele != null) {
					profile.add("avatar", ele);
				}

				// Rank data
				JsonArray rankData = new JsonArray();
				for (EntityRankInfo rank : AchievementManager.getInstance().getRanks(sv, save)) {
					if (rank.getRank() == null)
						continue;
					JsonObject rObj = new JsonObject();
					rObj.addProperty("rankID", rank.getRank().getID());
					rObj.addProperty("rankGlobalID", rank.getRank().getGlobalID());
					rObj.addProperty("pointTypeID", rank.getRank().getPointTypeID());
					rObj.addProperty("level", AchievementManager.getInstance().getRankIndex(rank.getRank()) + 1);
					rObj.addProperty("points", rank.getTotalScore());
					rankData.add(rObj);
				}
				profile.add("ranks", rankData);

				profile.addProperty("creationTime", sv.getCreationTime());
				profiles.add(save, profile);
			}
		}
		resp.add("profiles", profiles);

		// Return
		return ok("text/json", resp.toString());
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult downloadAccount(FunctionInfo func) {
		// Check authentication
		if (!func.getRequest().hasHeader("Authorization")
				|| !func.getRequest().getHeader("Authorization").getValue().toLowerCase().startsWith("bearer ")) {
			// Error
			return response(401, "Unauthorized", "text/json", "{\"error\":\"Missing authorization token\"}");
		}

		// Read token
		String token = func.getRequest().getHeader("Authorization").getValue().substring("bearer ".length());
		SessionToken tkn = new SessionToken();
		TokenParseResult res = tkn.parseToken(token);
		AccountObject acc = tkn.account;
		if (res != TokenParseResult.SUCCESS) {
			// Error
			return response(401, "Unauthorized", "text/json", "{\"error\":\"Authorization token invalid\"}");
		}

		// Check headers
		if (!getRequest().hasHeader("Upgrade")
				|| !getRequest().getHeaderValue("Upgrade").equals("EDGEBINPROT/EDGESERVICES/DOWNLOADACCOUNT")) {
			return response(400, "Bad request");
		}

		// Set headers
		setResponseHeader("X-Response-ID", UUID.randomUUID().toString());
		setResponseHeader("Upgrade", "EDGEBINPROT/EDGESERVICES/DOWNLOADACCOUNT");

		// Setup
		AsyncTaskManager.runAsync(() -> {
			// Wait for upgrade
			while (func.getClient().isConnected()) {
				// Check
				if (func.getResponse().hasHeader("Upgraded")
						&& func.getResponse().getHeaderValue("Upgraded").equalsIgnoreCase("true"))
					break;

				// Wait
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			}

			// Check
			if ((func.getClient().isConnected())) {
				// Upgraded
				InputStream input = func.getClient().getInputStream();
				OutputStream output = func.getClient().getOutputStream();

				// Write data
				try {
					// Write account headers
					writeString(output, acc.getAccountID());
					writeString(output, acc.getUsername());
					String email = acc.getAccountEmail();
					writeBoolean(output, email != null);
					if (email != null)
						writeString(output, email);
					writeLong(output, acc.getLastLoginTime());
					writeLong(output, acc.getRegistrationTimestamp());
					writeBoolean(output, acc.isGuestAccount());
					writeBoolean(output, acc.isMultiplayerEnabled());
					writeBoolean(output, acc.isChatEnabled());
					writeBoolean(output, acc.isStrictChatFilterEnabled());

					// Write save id headers
					String[] ids = acc.getSaveIDs();

					// Index data
					// The client should say the server is indexing now

					// Index data
					long index = indexData(acc.getAccountKeyValueContainer());

					// Index saves
					for (String id : ids) {
						// Index save
						index++;

						// Index data
						index += indexData(acc.getSave(id).getSaveData());
					}

					// Write index length
					writeLong(output, index);

					// Send account data
					writeData(output, acc.getAccountKeyValueContainer());

					// Send saves
					writeInt(output, ids.length);
					for (String id : ids) {
						AccountSaveContainer save = acc.getSave(id);

						// Write save headers
						writeString(output, id);
						writeString(output, save.getUsername());
						writeLong(output, save.getCreationTime());

						// Write data
						writeData(output, save.getSaveData());
					}
				} catch (IOException e) {
				}

				// Done
				try {
					input.close();
				} catch (IOException e) {
				}
				try {
					output.close();
				} catch (IOException e) {
				}
				func.getClient().closeConnection();
			}
		});

		// Send response
		return response(101, "Switching Protocols");
	}

	private void writeData(OutputStream output, AccountKvDataContainer cont) throws IOException {
		String[] keys = cont.getEntryKeys();
		String[] conts = cont.getChildContainers();

		// Write data
		writeInt(output, (int) Stream.of(keys).filter(t -> !t.equals("chholder")).count());
		for (String key : keys) {
			if (key.equals("chholder"))
				continue;

			// Write key
			writeString(output, key);

			// Write data
			JsonElement ele = cont.getEntry(key);
			writeBoolean(output, ele != null);
			if (ele != null)
				writeString(output, ele.toString());
		}

		// Write containers
		writeInt(output, conts.length);
		for (String contN : conts) {
			// Write key
			writeString(output, contN);

			// Write data
			writeData(output, cont.getChildContainer(contN));
		}
	}

	private long indexData(AccountKvDataContainer cont) throws IOException {
		long i = 0;
		i += (int) Stream.of(cont.getEntryKeys()).filter(t -> !t.equals("chholder")).count();
		i += cont.getChildContainers().length;
		for (String childContName : cont.getChildContainers()) {
			i += indexData(cont.getChildContainer(childContName));
		}
		return i;
	}

	private FunctionResult invalidUserCallback(String username, AccountObject account) throws IOException {
		if (account != null)
			account.getAccountKeyValueContainer().getChildContainer("accountdata").setEntry("lockedsince",
					new JsonPrimitive(System.currentTimeMillis()));
		try {
			Thread.sleep(8000);
		} catch (InterruptedException e) {
		}
		return response(401, "Unauthorized", "text/json", "{\"error\":\"invalid_credentials\"}");
	}

	private void writeInt(OutputStream strm, int val) throws IOException {
		strm.write(ByteBuffer.allocate(4).putInt(val).array());
	}

	private void writeLong(OutputStream strm, long val) throws IOException {
		strm.write(ByteBuffer.allocate(8).putLong(val).array());
	}

	private void writeString(OutputStream strm, String str) throws IOException {
		byte[] data = str.getBytes("UTF-8");
		writeByteArray(strm, data);
	}

	private void writeByteArray(OutputStream strm, byte[] data) throws IOException {
		writeInt(strm, data.length);
		strm.write(data);
	}

	private void writeBoolean(OutputStream strm, boolean b) throws IOException {
		strm.write(b ? 1 : 0);
	}

}
