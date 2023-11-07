package org.asf.edge.commonapi.http.handlers.api.edgespecific;

import java.io.IOException;
import java.util.stream.Stream;

import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.entities.achivements.EntityRankInfo;
import org.asf.edge.common.events.accounts.saves.AccountSaveAuthenticatedEvent;
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

/**
 * 
 * Edge API endpoint for save management, holds public edge-specific API
 * features for save data
 * 
 * @author Sky Swimmer
 *
 */
public class EdgeSaveApiService extends EdgeWebService<EdgeCommonApiServer> {

	public EdgeSaveApiService(EdgeCommonApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new EdgeSaveApiService(getServerInstance());
	}

	@Override
	public String path() {
		return "/API/ProjectEdge/Accounts/";
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult getProfileByID(FunctionInfo func) throws IOException {
		// Load request
		JsonObject request;
		try {
			// Parse and check
			request = JsonParser.parseString(func.getRequest().getRequestBodyAsString()).getAsJsonObject();
			if (!request.has("id"))
				throw new IOException();
		} catch (Exception e) {
			// Bad request
			return response(400, "Bad request", "text/json",
					"{\"error\":\"Invalid request body, expecting a JSON with a 'id' field\"}");
		}

		// Load request into memory
		String id = request.get("id").getAsString();

		// Find account manager
		AccountManager manager = AccountManager.getInstance();

		// Find save
		AccountSaveContainer save = manager.getSaveByID(id);
		if (save == null) {
			// Error
			return response(404, "Not found", "text/json", "{\"error\":\"save_not_found\"}");
		}

		// Build response
		AccountObject acc = save.getAccount();
		JsonObject resp = new JsonObject();
		resp.addProperty("saveId", save.getSaveID());
		resp.addProperty("saveUsername", save.getUsername());
		resp.addProperty("registrationTime", acc.getRegistrationTimestamp());
		resp.addProperty("saveCreationTime", save.getCreationTime());

		// Find avatar
		JsonElement ele = save.getSaveData().getEntry("avatar");
		if (ele != null) {
			resp.add("avatar", ele);
		}

		// Rank data
		JsonArray rankData = new JsonArray();
		for (EntityRankInfo rank : AchievementManager.getInstance().getRanks(save, id)) {
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
		resp.add("ranks", rankData);

		// Return
		return ok("text/json", resp.toString());
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult getProfileByName(FunctionInfo func) throws IOException {
		// Load request
		JsonObject request;
		try {
			// Parse and check
			request = JsonParser.parseString(func.getRequest().getRequestBodyAsString()).getAsJsonObject();
			if (!request.has("name"))
				throw new IOException();
		} catch (Exception e) {
			// Bad request
			return response(400, "Bad request", "text/json",
					"{\"error\":\"Invalid request body, expecting a JSON with a 'name' field\"}");
		}

		// Load request into memory
		String name = request.get("name").getAsString();

		// Find account manager
		AccountManager manager = AccountManager.getInstance();

		// Find save
		String accID = manager.getAccountIdBySaveUsername(name);
		if (accID == null) {
			// Error
			return response(404, "Not found", "text/json", "{\"error\":\"save_not_found\"}");
		}
		AccountObject acc = manager.getAccount(accID);
		if (acc == null) {
			// Error
			return response(404, "Not found", "text/json", "{\"error\":\"save_not_found\"}");
		}
		AccountSaveContainer save = null;
		for (String saveID : acc.getSaveIDs()) {
			AccountSaveContainer sv = acc.getSave(saveID);
			if (sv.getUsername().equalsIgnoreCase(name)) {
				save = sv;
				break;
			}
		}
		if (save == null) {
			// Error
			return response(404, "Not found", "text/json", "{\"error\":\"save_not_found\"}");
		}

		// Build response
		JsonObject resp = new JsonObject();
		resp.addProperty("saveId", save.getSaveID());
		resp.addProperty("saveUsername", save.getUsername());
		resp.addProperty("registrationTime", acc.getRegistrationTimestamp());
		resp.addProperty("saveCreationTime", save.getCreationTime());

		// Find avatar
		JsonElement ele = save.getSaveData().getEntry("avatar");
		if (ele != null) {
			resp.add("avatar", ele);
		}

		// Rank data
		JsonArray rankData = new JsonArray();
		for (EntityRankInfo rank : AchievementManager.getInstance().getRanks(save, save.getSaveID())) {
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
		resp.add("ranks", rankData);

		// Return
		return ok("text/json", resp.toString());
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult authenticateSave(FunctionInfo func) throws IOException {
		// Authenticate for save

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
			if (!request.has("saveID"))
				throw new IOException();
		} catch (Exception e) {
			// Bad request
			return response(400, "Bad request", "text/json",
					"{\"error\":\"Invalid request body, expecting a JSON with a 'saveID' field\"}");
		}

		// Pull save ID
		String saveID = request.get("saveID").getAsString();

		// Locate save
		AccountSaveContainer save = acc.getSave(saveID);
		if (save == null) {
			// Error
			return response(404, "Not found", "text/json", "{\"error\":\"save_not_found\"}");
		}

		// Build save-specific token
		tkn = new SessionToken();
		tkn.accountID = acc.getAccountID();
		tkn.saveID = saveID;
		tkn.lastLoginTime = acc.getLastLoginTime();
		tkn.capabilities = new String[] { "api", "gp" };

		// Log
		getServerInstance().getLogger().info("Viking selected for account " + acc.getAccountID() + ": selected viking '"
				+ save.getUsername() + "' (ID " + save.getSaveID() + ") from API.");
		save.getAccount().ping(true);

		// Dispatch event
		EventBus.getInstance()
				.dispatchEvent(new AccountSaveAuthenticatedEvent(acc, save, AccountManager.getInstance()));

		// Encode
		token = tkn.toTokenString();

		// Build response
		JsonObject resp = new JsonObject();
		resp.addProperty("token", token);
		resp.addProperty("accountId", acc.getAccountID());
		resp.addProperty("accountUsername", acc.getUsername());
		resp.addProperty("saveId", save.getSaveID());
		resp.addProperty("saveUsername", save.getUsername());
		resp.addProperty("lastLoginTime", acc.getLastLoginTime());
		resp.addProperty("registrationTime", acc.getRegistrationTimestamp());
		resp.addProperty("saveCreationTime", save.getCreationTime());
		resp.addProperty("multiplayerEnabled", acc.isMultiplayerEnabled());
		resp.addProperty("chatEnabled", acc.isChatEnabled());
		resp.addProperty("strictChat", acc.isStrictChatFilterEnabled());

		// Find avatar
		JsonElement ele = save.getSaveData().getEntry("avatar");
		if (ele != null) {
			resp.add("avatar", ele);
		}

		// Rank data
		JsonArray rankData = new JsonArray();
		for (EntityRankInfo rank : AchievementManager.getInstance().getRanks(save, saveID)) {
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
		resp.add("ranks", rankData);

		// Return
		return ok("text/json", resp.toString());
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult updateSave(FunctionInfo func) throws IOException {
		// Save details

		// Check authentication
		if (!func.getRequest().hasHeader("Authorization")
				|| !func.getRequest().getHeader("Authorization").getValue().toLowerCase().startsWith("bearer ")) {
			// Error
			return response(401, "Unauthorized", "text/json", "{\"error\":\"Missing authorization token\"}");
		}

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
			if (!request.has("saveID"))
				throw new IOException();
		} catch (Exception e) {
			// Bad request
			return response(400, "Bad request", "text/json",
					"{\"error\":\"Invalid request body, expecting a JSON with at least a 'saveID' field\"}");
		}

		// Pull save ID
		String saveID = request.get("saveID").getAsString();

		// Locate save
		AccountSaveContainer save = acc.getSave(saveID);
		if (save == null) {
			// Error
			return response(404, "Not found", "text/json", "{\"error\":\"save_not_found\"}");
		}

		// Find account manager
		AccountManager manager = AccountManager.getInstance();

		// Update save
		if (request.has("saveUsername")) {
			String username = request.get("saveUsername").getAsString();

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

			// Update
			if (!save.updateUsername(username))
				return response(500, "Internal server error", "text/json", "{\"error\":\"server_error\"}");
		}

		// Build response
		JsonObject resp = new JsonObject();
		resp.addProperty("accountId", acc.getAccountID());
		resp.addProperty("accountUsername", acc.getUsername());
		resp.addProperty("saveId", save.getSaveID());
		resp.addProperty("saveUsername", save.getUsername());
		resp.addProperty("lastLoginTime", acc.getLastLoginTime());
		resp.addProperty("registrationTime", acc.getRegistrationTimestamp());
		resp.addProperty("saveCreationTime", save.getCreationTime());
		resp.addProperty("multiplayerEnabled", acc.isMultiplayerEnabled());
		resp.addProperty("chatEnabled", acc.isChatEnabled());
		resp.addProperty("strictChat", acc.isStrictChatFilterEnabled());

		// Find avatar
		JsonElement ele = save.getSaveData().getEntry("avatar");
		if (ele != null) {
			resp.add("avatar", ele);
		}

		// Rank data
		JsonArray rankData = new JsonArray();
		for (EntityRankInfo rank : AchievementManager.getInstance().getRanks(save, save.getSaveID())) {
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
		resp.add("ranks", rankData);

		// Return
		return ok("text/json", resp.toString());
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult getSaveDetails(FunctionInfo func) throws IOException {
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
			if (!request.has("saveID"))
				throw new IOException();
		} catch (Exception e) {
			// Bad request
			return response(400, "Bad request", "text/json",
					"{\"error\":\"Invalid request body, expecting a JSON with a 'saveID' field\"}");
		}

		// Pull save ID
		String saveID = request.get("saveID").getAsString();

		// Locate save
		AccountSaveContainer save = acc.getSave(saveID);
		if (save == null) {
			// Error
			return response(404, "Not found", "text/json", "{\"error\":\"save_not_found\"}");
		}

		// Build response
		JsonObject resp = new JsonObject();
		resp.addProperty("accountId", acc.getAccountID());
		resp.addProperty("accountUsername", acc.getUsername());
		resp.addProperty("saveId", save.getSaveID());
		resp.addProperty("saveUsername", save.getUsername());
		resp.addProperty("lastLoginTime", acc.getLastLoginTime());
		resp.addProperty("registrationTime", acc.getRegistrationTimestamp());
		resp.addProperty("saveCreationTime", save.getCreationTime());
		resp.addProperty("multiplayerEnabled", acc.isMultiplayerEnabled());
		resp.addProperty("chatEnabled", acc.isChatEnabled());
		resp.addProperty("strictChat", acc.isStrictChatFilterEnabled());

		// Find avatar
		JsonElement ele = save.getSaveData().getEntry("avatar");
		if (ele != null) {
			resp.add("avatar", ele);
		}

		// Rank data
		JsonArray rankData = new JsonArray();
		for (EntityRankInfo rank : AchievementManager.getInstance().getRanks(save, save.getSaveID())) {
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
		resp.add("ranks", rankData);

		// Return
		return ok("text/json", resp.toString());
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult createSave(FunctionInfo func) throws IOException {
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
			if (!request.has("saveUsername"))
				throw new IOException();
		} catch (Exception e) {
			// Bad request
			return response(400, "Bad request", "text/json",
					"{\"error\":\"Invalid request body, expecting a JSON with a 'saveUsername' field\"}");
		}

		// Find account manager
		AccountManager manager = AccountManager.getInstance();

		// Pull save username
		String saveUsername = request.get("saveUsername").getAsString();

		// Check validity
		if (!manager.isValidUsername(saveUsername)) {
			// Invalid name
			return response(400, "Bad request", "text/json", "{\"error\":\"invalid_username\"}");
		}

		// Check filters
		if (TextFilterService.getInstance().isFiltered(saveUsername, true)) {
			// Invalid name
			return response(400, "Bad request", "text/json", "{\"error\":\"inappropriate_username\"}");
		}

		// Check if in use
		boolean inUse = false;
		if (!acc.getUsername().equalsIgnoreCase(saveUsername) && manager.isUsernameTaken(saveUsername)) {
			inUse = true;
		} else {
			// Check if in use by any saves
			AccountObject accF = acc;
			if (Stream.of(accF.getSaveIDs()).map(t -> accF.getSave(t)).anyMatch(t -> {
				return t.getUsername().equalsIgnoreCase(saveUsername);
			})) {
				inUse = true;
			}
		}
		if (inUse) {
			// Invalid name
			return response(400, "Bad request", "text/json", "{\"error\":\"username_in_use\"}");
		}

		// Create save
		AccountSaveContainer save = acc.createSave(saveUsername);
		if (save == null) {
			// Error
			return response(500, "Internal server error", "text/json", "{\"error\":\"save_creation_failure\"}");
		}

		// Build save-specific token
		tkn = new SessionToken();
		tkn.accountID = acc.getAccountID();
		tkn.saveID = save.getSaveID();
		tkn.lastLoginTime = acc.getLastLoginTime();
		tkn.capabilities = new String[] { "api", "gp" };

		// Log
		getServerInstance().getLogger().info("Viking created for account " + acc.getAccountID() + ": selected viking '"
				+ save.getUsername() + "' (ID " + save.getSaveID() + ") from API.");
		save.getAccount().ping(true);

		// Encode
		token = tkn.toTokenString();

		// Build response
		JsonObject resp = new JsonObject();
		resp.addProperty("token", token);
		resp.addProperty("accountId", acc.getAccountID());
		resp.addProperty("accountUsername", acc.getUsername());
		resp.addProperty("saveId", save.getSaveID());
		resp.addProperty("saveUsername", save.getUsername());
		resp.addProperty("lastLoginTime", acc.getLastLoginTime());
		resp.addProperty("registrationTime", acc.getRegistrationTimestamp());
		resp.addProperty("saveCreationTime", save.getCreationTime());
		resp.addProperty("multiplayerEnabled", acc.isMultiplayerEnabled());
		resp.addProperty("chatEnabled", acc.isChatEnabled());
		resp.addProperty("strictChat", acc.isStrictChatFilterEnabled());

		// Find avatar
		JsonElement ele = save.getSaveData().getEntry("avatar");
		if (ele != null) {
			resp.add("avatar", ele);
		}

		// Rank data
		JsonArray rankData = new JsonArray();
		for (EntityRankInfo rank : AchievementManager.getInstance().getRanks(save, save.getSaveID())) {
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
		resp.add("ranks", rankData);

		// Return
		return ok("text/json", resp.toString());
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult deleteSave(FunctionInfo func) throws IOException {
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
			if (!request.has("saveID"))
				throw new IOException();
		} catch (Exception e) {
			// Bad request
			return response(400, "Bad request", "text/json",
					"{\"error\":\"Invalid request body, expecting a JSON with a 'saveID' field\"}");
		}

		// Pull save ID
		String saveID = request.get("saveID").getAsString();

		// Locate save
		AccountSaveContainer save = acc.getSave(saveID);
		if (save == null) {
			// Error
			return response(404, "Not found", "text/json", "{\"error\":\"save_not_found\"}");
		}

		// Delete
		save.deleteSave();

		// Log
		getServerInstance().getLogger().info("Save deleted: " + acc.getAccountID() + ".");

		// Build response
		JsonObject resp = new JsonObject();
		resp.addProperty("deleted", true);

		// Return
		return ok("text/json", resp.toString());
	}

}
