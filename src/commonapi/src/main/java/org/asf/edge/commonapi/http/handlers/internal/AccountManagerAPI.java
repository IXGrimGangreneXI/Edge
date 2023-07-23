package org.asf.edge.commonapi.http.handlers.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

import org.asf.connective.processors.HttpPushProcessor;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionResult;
import org.asf.edge.common.http.apihandlerutils.functions.LegacyFunction;
import org.asf.edge.common.http.apihandlerutils.functions.LegacyFunctionInfo;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.util.SimpleBinaryMessageClient;
import org.asf.edge.commonapi.EdgeCommonApiServer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class AccountManagerAPI extends EdgeWebService<EdgeCommonApiServer> {

	private static AccountManager manager;

	public AccountManagerAPI(EdgeCommonApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new AccountManagerAPI(getServerInstance());
	}

	@Override
	public String path() {
		return "/accountmanager";
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void verifyToken(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("token")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		JsonObject resp = new JsonObject();
		resp.addProperty("result", manager.verifyToken(payload.get("token").getAsString()).ordinal());
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void signToken(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("token")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		JsonObject resp = new JsonObject();
		resp.addProperty("result",
				Base64.getEncoder().encodeToString(manager.signToken(payload.get("token").getAsString())));
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void isValidUsername(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("username")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		JsonObject resp = new JsonObject();
		resp.addProperty("result", manager.isValidUsername(payload.get("username").getAsString()));
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void isValidPassword(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("password")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		JsonObject resp = new JsonObject();
		resp.addProperty("result", manager.isValidPassword(payload.get("password").getAsString()));
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void isUsernameTaken(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("username")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		JsonObject resp = new JsonObject();
		resp.addProperty("result", manager.isUsernameTaken(payload.get("username").getAsString()));
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getAccountID(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("username")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		JsonObject resp = new JsonObject();
		String id = manager.getAccountID(payload.get("username").getAsString());
		resp.addProperty("success", id != null);
		if (id != null)
			resp.addProperty("id", id);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getAccountIdBySaveUsername(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("username")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		JsonObject resp = new JsonObject();
		String id = manager.getAccountIdBySaveUsername(payload.get("username").getAsString());
		resp.addProperty("success", id != null);
		if (id != null)
			resp.addProperty("id", id);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getAccountIDByEmail(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("email")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		JsonObject resp = new JsonObject();
		String id = manager.getAccountIDByEmail(payload.get("email").getAsString());
		resp.addProperty("success", id != null);
		if (id != null)
			resp.addProperty("id", id);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void verifyPassword(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("password")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		JsonObject resp = new JsonObject();
		resp.addProperty("result",
				manager.verifyPassword(payload.get("id").getAsString(), payload.get("password").getAsString()));
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void accountExists(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		JsonObject resp = new JsonObject();
		resp.addProperty("result", manager.accountExists(payload.get("id").getAsString()));
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getAccount(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		resp.addProperty("success", acc != null);
		if (acc != null)
			resp.addProperty("username", acc.getUsername());
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getGuestAccount(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("guestID")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getGuestAccount(payload.get("guestID").getAsString());
		JsonObject resp = new JsonObject();
		resp.addProperty("success", acc != null);
		if (acc != null) {
			resp.addProperty("id", acc.getAccountID());
			resp.addProperty("username", acc.getUsername());
		}
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/getDataEntry")
	public void getDataEntry(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("key")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			JsonElement val = acc.getAccountData().unsafeAccessor().get(payload.get("key").getAsString());
			resp.addProperty("success", val != null);
			if (val != null)
				resp.add("entryValue", val);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/setDataEntry")
	public void setDataEntry(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("key") || !payload.has("value")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			acc.getAccountData().unsafeAccessor().set(payload.get("key").getAsString(), payload.get("value"));
			resp.addProperty("success", true);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/createDataEntry")
	public void createDataEntry(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("key") || !payload.has("root") || !payload.has("value")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			acc.getAccountData().unsafeAccessor().create(payload.get("key").getAsString(),
					payload.get("root").getAsString(), payload.get("value"));
			resp.addProperty("success", true);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/dataEntryExists")
	public void dataEntryExists(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("key")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			resp.addProperty("result", acc.getAccountData().unsafeAccessor().exists(payload.get("key").getAsString()));
		} else
			resp.addProperty("result", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/deleteDataEntry")
	public void deleteDataEntry(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("key")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			acc.getAccountData().unsafeAccessor().delete(payload.get("key").getAsString());
			resp.addProperty("success", true);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@Function(allowedMethods = { "POST" }, value = "accounts/getChildContainers")
	public FunctionResult getChildContainers(FunctionInfo func) throws IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			return response(400, "Bad request");
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("key")) {
			return response(400, "Bad request");
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			String[] containers = acc.getAccountData().unsafeAccessor()
					.getChildContainers(payload.get("key").getAsString());
			JsonArray arr = new JsonArray();
			for (String cont : containers)
				arr.add(cont);
			resp.addProperty("success", true);
			resp.add("result", arr);
		} else
			resp.addProperty("success", false);
		return ok("text/json", resp.toString());
	}

	@Function(allowedMethods = { "POST" }, value = "accounts/getEntryKeys")
	public FunctionResult getEntryKeys(FunctionInfo func) throws IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			return response(400, "Bad request");
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("key")) {
			return response(400, "Bad request");
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			String[] keys = acc.getAccountData().unsafeAccessor().getEntryKeys(payload.get("key").getAsString());
			JsonArray arr = new JsonArray();
			for (String cont : keys)
				arr.add(cont);
			resp.addProperty("success", true);
			resp.add("result", arr);
		} else
			resp.addProperty("success", false);
		return ok("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/deleteAccount")
	public void deleteAccount(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			acc.deleteAccount();
			resp.addProperty("success", true);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/updateLastLoginTime")
	public void updateLastLoginTime(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			acc.updateLastLoginTime();
			resp.addProperty("success", true);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/setStrictChatFilterEnabled")
	public void setStrictChatFilterEnabled(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("state")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			acc.setStrictChatFilterEnabled(payload.get("state").getAsBoolean());
			resp.addProperty("success", true);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/setChatEnabled")
	public void setChatEnabled(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("state")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			acc.setChatEnabled(payload.get("state").getAsBoolean());
			resp.addProperty("success", true);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/setMultiplayerEnabled")
	public void setMultiplayerEnabled(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("state")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			acc.setMultiplayerEnabled(payload.get("state").getAsBoolean());
			resp.addProperty("success", true);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/isStrictChatFilterEnabled")
	public void isStrictChatFilterEnabled(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			resp.addProperty("result", acc.isStrictChatFilterEnabled());
		} else
			resp.addProperty("result", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/isChatEnabled")
	public void isChatEnabled(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			resp.addProperty("result", acc.isChatEnabled());
		} else
			resp.addProperty("result", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/isMultiplayerEnabled")
	public void isMultiplayerEnabled(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			resp.addProperty("result", acc.isMultiplayerEnabled());
		} else
			resp.addProperty("result", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/isGuestAccount")
	public void isGuestAccount(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			resp.addProperty("result", acc.isGuestAccount());
		} else
			resp.addProperty("result", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/migrateToNormalAccountFromGuest")
	public void migrateToNormalAccountFromGuest(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("newName") || !payload.has("email") || !payload.has("password")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			resp.addProperty("success", acc.migrateToNormalAccountFromGuest(payload.get("newName").getAsString(),
					payload.get("email").getAsString(), payload.get("password").getAsString().toCharArray()));
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/updatePassword")
	public void updatePassword(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("newPassword")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			resp.addProperty("success", acc.updatePassword(payload.get("newPassword").getAsString().toCharArray()));
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/updateUsername")
	public void updateUsername(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("newName")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			resp.addProperty("success", acc.updateUsername(payload.get("newName").getAsString()));
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/updateEmail")
	public void updateEmail(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("email")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			resp.addProperty("success", acc.updateEmail(payload.get("email").getAsString()));
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/getRegistrationTimestamp")
	public void getRegistrationTimestamp(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			resp.addProperty("time", acc.getRegistrationTimestamp());
		} else
			resp.addProperty("time", -1);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/getLastLoginTime")
	public void getLastLoginTime(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			resp.addProperty("time", acc.getLastLoginTime());
		} else
			resp.addProperty("time", -1);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/getSaveIDs")
	public void getSaveIDs(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			JsonArray saveLst = new JsonArray();
			for (String save : acc.getSaveIDs())
				saveLst.add(save);
			resp.add("saves", saveLst);
		} else
			resp.add("saves", new JsonArray());
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/createSave")
	public void createSave(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("username")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			AccountSaveContainer cont = acc.createSave(payload.get("username").getAsString());
			if (cont != null) {
				resp.addProperty("success", true);
				resp.addProperty("id", cont.getSaveID());
				resp.addProperty("time", cont.getCreationTime());
			} else
				resp.addProperty("success", false);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/getSave")
	public void getSave(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("save")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			AccountSaveContainer cont = acc.getSave(payload.get("save").getAsString());
			if (cont != null) {
				resp.addProperty("success", true);
				resp.addProperty("username", cont.getUsername());
				resp.addProperty("time", cont.getCreationTime());
			} else
				resp.addProperty("success", false);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/updateSaveUsername")
	public void updateSaveUsername(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("save") || !payload.has("newName")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			AccountSaveContainer cont = acc.getSave(payload.get("save").getAsString());
			if (cont != null)
				resp.addProperty("success", cont.updateUsername(payload.get("newName").getAsString()));
			else
				resp.addProperty("success", false);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/deleteSave")
	public void deleteSave(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("save")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			AccountSaveContainer cont = acc.getSave(payload.get("save").getAsString());
			if (cont != null) {
				cont.deleteSave();
				resp.addProperty("success", true);
			} else
				resp.addProperty("success", false);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/getSaveDataEntry")
	public void getSaveDataEntry(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("save") || !payload.has("key")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			AccountSaveContainer cont = acc.getSave(payload.get("save").getAsString());
			if (cont != null) {
				JsonElement val = cont.getSaveData().unsafeAccessor().get(payload.get("key").getAsString());
				resp.addProperty("success", val != null);
				if (val != null)
					resp.add("entryValue", val);
			} else
				resp.addProperty("success", false);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/setSaveDataEntry")
	public void setSaveDataEntry(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("save") || !payload.has("key") || !payload.has("value")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			AccountSaveContainer cont = acc.getSave(payload.get("save").getAsString());
			if (cont != null) {
				cont.getSaveData().unsafeAccessor().set(payload.get("key").getAsString(), payload.get("value"));
				resp.addProperty("success", true);
			} else
				resp.addProperty("success", false);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/createSaveDataEntry")
	public void createSaveDataEntry(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load createSaveDataEntry
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("save") || !payload.has("root") || !payload.has("key")
				|| !payload.has("value")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			AccountSaveContainer cont = acc.getSave(payload.get("save").getAsString());
			if (cont != null) {
				cont.getSaveData().unsafeAccessor().create(payload.get("key").getAsString(),
						payload.get("root").getAsString(), payload.get("value"));
				resp.addProperty("success", true);
			} else
				resp.addProperty("success", false);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/saveDataEntryExists")
	public void saveDataEntryExists(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("save") || !payload.has("key")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			AccountSaveContainer cont = acc.getSave(payload.get("save").getAsString());
			if (cont != null) {
				resp.addProperty("result",
						cont.getSaveData().unsafeAccessor().exists(payload.get("key").getAsString()));
			} else
				resp.addProperty("result", false);
		} else
			resp.addProperty("result", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/deleteSaveDataEntry")
	public void deleteSaveDataEntry(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("save") || !payload.has("key")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			AccountSaveContainer cont = acc.getSave(payload.get("save").getAsString());
			if (cont != null) {
				cont.getSaveData().unsafeAccessor().delete(payload.get("key").getAsString());
				resp.addProperty("success", true);
			} else
				resp.addProperty("success", false);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@Function(allowedMethods = { "POST" }, value = "accounts/deleteSaveContainer")
	public FunctionResult deleteSaveContainer(FunctionInfo func) throws IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			return response(400, "Bad request");
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("save") || !payload.has("root")) {
			return response(400, "Bad request");
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			AccountSaveContainer cont = acc.getSave(payload.get("save").getAsString());
			if (cont != null) {
				cont.getSaveData().unsafeAccessor().deleteContainer(payload.get("root").getAsString());
				resp.addProperty("success", true);
			} else
				resp.addProperty("success", false);
		} else
			resp.addProperty("success", false);
		return ok("text/json", resp.toString());
	}

	@Function(allowedMethods = { "POST" }, value = "accounts/getChildSaveContainers")
	public FunctionResult getChildSaveContainers(FunctionInfo func) throws IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			return response(400, "Bad request");
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("save") || !payload.has("key")) {
			return response(400, "Bad request");
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			AccountSaveContainer cont = acc.getSave(payload.get("save").getAsString());
			if (cont != null) {
				String[] containers = cont.getSaveData().unsafeAccessor()
						.getChildContainers(payload.get("key").getAsString());
				JsonArray arr = new JsonArray();
				for (String conta : containers)
					arr.add(conta);
				resp.addProperty("success", true);
				resp.add("result", arr);
			} else
				resp.addProperty("success", false);
		} else
			resp.addProperty("success", false);
		return ok("text/json", resp.toString());
	}

	@Function(allowedMethods = { "POST" }, value = "accounts/getSaveEntryKeys")
	public FunctionResult getSaveEntryKeys(FunctionInfo func) throws IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			return response(400, "Bad request");
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("save") || !payload.has("key")) {
			return response(400, "Bad request");
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			AccountSaveContainer cont = acc.getSave(payload.get("save").getAsString());
			if (cont != null) {
				String[] keys = acc.getAccountData().unsafeAccessor().getEntryKeys(payload.get("key").getAsString());
				JsonArray arr = new JsonArray();
				for (String conta : keys)
					arr.add(conta);
				resp.addProperty("success", true);
				resp.add("result", arr);
			} else
				resp.addProperty("success", false);
		} else
			resp.addProperty("success", false);
		return ok("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void registerGuestAccount(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("guestID")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.registerGuestAccount(payload.get("guestID").getAsString());
		JsonObject resp = new JsonObject();
		resp.addProperty("success", acc != null);
		if (acc != null) {
			resp.addProperty("id", acc.getAccountID());
			resp.addProperty("username", acc.getUsername());
		}
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void registerAccount(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("username") || !payload.has("email") || !payload.has("password")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.registerAccount(payload.get("username").getAsString(),
				payload.get("email").getAsString(), payload.get("password").getAsString().toCharArray());
		JsonObject resp = new JsonObject();
		resp.addProperty("success", acc != null);
		if (acc != null) {
			resp.addProperty("id", acc.getAccountID());
			resp.addProperty("username", acc.getUsername());
		}
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/getAccountEmail")
	public void getAccountEmail(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			String email = acc.getAccountEmail();
			if (email != null) {
				resp.addProperty("success", true);
				resp.addProperty("email", email);
			} else
				resp.addProperty("success", false);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getSaveByID(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("save")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		JsonObject resp = new JsonObject();
		AccountSaveContainer cont = manager.getSaveByID(payload.get("save").getAsString());
		if (cont != null) {
			resp.addProperty("success", true);
			resp.addProperty("accid", cont.getAccount().getAccountID());
			resp.addProperty("username", cont.getSaveID());
			resp.addProperty("time", cont.getCreationTime());
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getOnlinePlayerIDs(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		JsonObject resp = new JsonObject();
		JsonArray players = new JsonArray();
		for (String save : manager.getOnlinePlayerIDs())
			players.add(save);
		resp.add("players", players);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" }, value = "accounts/isOnline")
	public void isOnline(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			resp.addProperty("result", acc.isOnline());
		} else
			resp.addProperty("result", false);
		setResponseContent("text/json", resp.toString());
	}

	private class FuncResult {
		public boolean continueRun;
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult runForAllAccounts(FunctionInfo func) throws IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Check headers
		if (!getRequest().hasHeader("Upgrade")
				|| !getRequest().getHeaderValue("Upgrade").equals("EDGEBINPROT/ACCMANAGER/RUNFORALLACCOUNTS")) {
			return response(400, "Bad request");
		}

		// Set headers
		setResponseHeader("X-Response-ID", UUID.randomUUID().toString());
		setResponseHeader("Upgrade", "EDGEBINPROT/ACCMANAGER/RUNFORALLACCOUNTS");

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
				SimpleBinaryMessageClient client = new SimpleBinaryMessageClient((packet, cl) -> {
					FuncResult res = new FuncResult();
					res.continueRun = packet.data[0] == 1 ? true : false;
					cl.container = res;
					return true;
				}, func.getClient().getInputStream(), func.getClient().getOutputStream());
				client.startAsync();
				manager.runForAllAccounts(t -> {
					boolean res = true;

					// Run
					client.container = null;
					try {
						client.send(t.getAccountID().getBytes("UTF-8"));
					} catch (IOException e) {
						return false;
					}

					// Wait
					while (client.container == null && client.isConnected()) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							break;
						}
					}
					if (client.container == null)
						return false;

					// Set result
					res = ((FuncResult) client.container).continueRun;

					// Return result
					return res;
				});
				client.stop();
			}
		});

		// Send response
		return response(101, "Switching Protocols");
	}

	@Function(allowedMethods = { "POST" }, value = "accounts/runForDataChildren")
	public FunctionResult runForDataChildren(FunctionInfo func) throws IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Check headers
		if (!getRequest().hasHeader("Upgrade")
				|| !getRequest().getHeaderValue("Upgrade").equals("EDGEBINPROT/ACCMANAGER/RUNFORCHILDREN")) {
			return response(400, "Bad request");
		}

		// Read payload
		if (!getRequest().hasRequestBody()) {
			return response(400, "Bad request");
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("root")) {
			return response(400, "Bad request");
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		if (acc == null)
			return response(404, "Account not found");

		// Set headers
		setResponseHeader("X-Response-ID", UUID.randomUUID().toString());
		setResponseHeader("Upgrade", "EDGEBINPROT/ACCMANAGER/RUNFORCHILDREN");

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
				SimpleBinaryMessageClient client = new SimpleBinaryMessageClient((packet, cl) -> {
					FuncResult res = new FuncResult();
					res.continueRun = packet.data[0] == 1 ? true : false;
					cl.container = res;
					return true;
				}, func.getClient().getInputStream(), func.getClient().getOutputStream());
				client.startAsync();
				try {
					acc.getAccountData().unsafeAccessor().runForChildren(t -> {
						boolean res = true;

						// Run
						client.container = null;
						try {
							client.send(t.getBytes("UTF-8"));
						} catch (IOException e) {
							return false;
						}

						// Wait
						while (client.container == null && client.isConnected()) {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								break;
							}
						}
						if (client.container == null)
							return false;

						// Set result
						res = ((FuncResult) client.container).continueRun;

						// Return result
						return res;
					}, payload.get("root").getAsString());
				} catch (IOException e) {
				}
				client.stop();
			}
		});

		// Send response
		return response(101, "Switching Protocols");
	}

	@Function(allowedMethods = { "POST" }, value = "accounts/runForDataEntries")
	public FunctionResult runForDataEntries(FunctionInfo func) throws IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Check headers
		if (!getRequest().hasHeader("Upgrade")
				|| !getRequest().getHeaderValue("Upgrade").equals("EDGEBINPROT/ACCMANAGER/RUNFOR")) {
			return response(400, "Bad request");
		}

		// Read payload
		if (!getRequest().hasRequestBody()) {
			return response(400, "Bad request");
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("root")) {
			return response(400, "Bad request");
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		if (acc == null)
			return response(404, "Account not found");

		// Set headers
		setResponseHeader("X-Response-ID", UUID.randomUUID().toString());
		setResponseHeader("Upgrade", "EDGEBINPROT/ACCMANAGER/RUNFOR");

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
				SimpleBinaryMessageClient client = new SimpleBinaryMessageClient((packet, cl) -> {
					FuncResult res = new FuncResult();
					res.continueRun = packet.data[0] == 1 ? true : false;
					cl.container = res;
					return true;
				}, func.getClient().getInputStream(), func.getClient().getOutputStream());
				client.startAsync();
				try {
					acc.getAccountData().unsafeAccessor().runFor((name, data) -> {
						boolean res = true;

						// Run
						client.container = null;
						try {
							ByteArrayOutputStream resD = new ByteArrayOutputStream();
							byte[] b = name.getBytes("UTF-8");
							resD.write(ByteBuffer.allocate(4).putInt(b.length).array());
							resD.write(b);
							b = data.toString().getBytes("UTF-8");
							resD.write(ByteBuffer.allocate(4).putInt(b.length).array());
							resD.write(b);
							client.send(resD.toByteArray());
						} catch (IOException e) {
							return false;
						}

						// Wait
						while (client.container == null && client.isConnected()) {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								break;
							}
						}
						if (client.container == null)
							return false;

						// Set result
						res = ((FuncResult) client.container).continueRun;

						// Return result
						return res;
					}, payload.get("root").getAsString());
				} catch (IOException e) {
				}
				client.stop();
			}
		});

		// Send response
		return response(101, "Switching Protocols");
	}

	@Function(allowedMethods = { "POST" }, value = "accounts/runForSaveDataChildren")
	public FunctionResult runForSaveDataChildren(FunctionInfo func) throws IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Check headers
		if (!getRequest().hasHeader("Upgrade")
				|| !getRequest().getHeaderValue("Upgrade").equals("EDGEBINPROT/ACCMANAGER/RUNFORCHILDREN")) {
			return response(400, "Bad request");
		}

		// Read payload
		if (!getRequest().hasRequestBody()) {
			return response(400, "Bad request");
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("root")) {
			return response(400, "Bad request");
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		if (acc == null)
			return response(404, "Account not found");
		AccountSaveContainer cont = acc.getSave(payload.get("save").getAsString());
		if (cont == null)
			return response(404, "Save not found");

		// Set headers
		setResponseHeader("X-Response-ID", UUID.randomUUID().toString());
		setResponseHeader("Upgrade", "EDGEBINPROT/ACCMANAGER/RUNFORCHILDREN");

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
				SimpleBinaryMessageClient client = new SimpleBinaryMessageClient((packet, cl) -> {
					FuncResult res = new FuncResult();
					res.continueRun = packet.data[0] == 1 ? true : false;
					cl.container = res;
					return true;
				}, func.getClient().getInputStream(), func.getClient().getOutputStream());
				client.startAsync();
				try {
					cont.getSaveData().unsafeAccessor().runForChildren(t -> {
						boolean res = true;

						// Run
						client.container = null;
						try {
							client.send(t.getBytes("UTF-8"));
						} catch (IOException e) {
							return false;
						}

						// Wait
						while (client.container == null && client.isConnected()) {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								break;
							}
						}
						if (client.container == null)
							return false;

						// Set result
						res = ((FuncResult) client.container).continueRun;

						// Return result
						return res;
					}, payload.get("root").getAsString());
				} catch (IOException e) {
				}
				client.stop();
			}
		});

		// Send response
		return response(101, "Switching Protocols");
	}

	@Function(allowedMethods = { "POST" }, value = "accounts/runForSaveDataEntries")
	public FunctionResult runForSaveDataEntries(FunctionInfo func) throws IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Check headers
		if (!getRequest().hasHeader("Upgrade")
				|| !getRequest().getHeaderValue("Upgrade").equals("EDGEBINPROT/ACCMANAGER/RUNFOR")) {
			return response(400, "Bad request");
		}

		// Read payload
		if (!getRequest().hasRequestBody()) {
			return response(400, "Bad request");
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("save") || !payload.has("root")) {
			return response(400, "Bad request");
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		if (acc == null)
			return response(404, "Account not found");
		AccountSaveContainer cont = acc.getSave(payload.get("save").getAsString());
		if (cont == null)
			return response(404, "Save not found");

		// Set headers
		setResponseHeader("X-Response-ID", UUID.randomUUID().toString());
		setResponseHeader("Upgrade", "EDGEBINPROT/ACCMANAGER/RUNFOR");

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
				SimpleBinaryMessageClient client = new SimpleBinaryMessageClient((packet, cl) -> {
					FuncResult res = new FuncResult();
					res.continueRun = packet.data[0] == 1 ? true : false;
					cl.container = res;
					return true;
				}, func.getClient().getInputStream(), func.getClient().getOutputStream());
				client.startAsync();
				try {
					cont.getSaveData().unsafeAccessor().runFor((name, data) -> {
						boolean res = true;

						// Run
						client.container = null;
						try {
							ByteArrayOutputStream resD = new ByteArrayOutputStream();
							byte[] b = name.getBytes("UTF-8");
							resD.write(ByteBuffer.allocate(4).putInt(b.length).array());
							resD.write(b);
							b = data.toString().getBytes("UTF-8");
							resD.write(ByteBuffer.allocate(4).putInt(b.length).array());
							resD.write(b);
							client.send(resD.toByteArray());
						} catch (IOException e) {
							return false;
						}

						// Wait
						while (client.container == null && client.isConnected()) {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								break;
							}
						}
						if (client.container == null)
							return false;

						// Set result
						res = ((FuncResult) client.container).continueRun;

						// Return result
						return res;
					}, payload.get("root").getAsString());
				} catch (IOException e) {
				}
				client.stop();
			}
		});

		// Send response
		return response(101, "Switching Protocols");
	}

	@Function(allowedMethods = { "POST" }, value = "accounts/deleteContainer")
	public FunctionResult deleteContainer(FunctionInfo func) throws IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			return response(400, "Bad request");
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("root")) {
			return response(400, "Bad request");
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			acc.getAccountData().unsafeAccessor().deleteContainer(payload.get("root").getAsString());
			resp.addProperty("success", true);
		} else
			resp.addProperty("success", false);
		return ok("text/json", resp.toString());
	}
}
