package org.asf.edge.commonapi.http.handlers.internal;

import java.io.IOException;

import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.account.AccountManager;
import org.asf.edge.common.account.AccountObject;
import org.asf.edge.common.http.apihandlerutils.BaseApiHandler;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.commonapi.EdgeCommonApiServer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class AccountManagerAPI extends BaseApiHandler<EdgeCommonApiServer> {

	private AccountManager manager;

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

	@Function(allowedMethods = { "POST" })
	public void isValidUsername(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" })
	public void isValidPassword(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" })
	public void isUsernameTaken(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" })
	public void getAccountID(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" })
	public void verifyPassword(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" })
	public void accountExists(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" })
	public void getAccount(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" })
	public void getGuestAccount(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" }, value = "accounts/getDataEntry")
	public void getDataEntry(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" }, value = "accounts/setDataEntry")
	public void setDataEntry(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" }, value = "accounts/createDataEntry")
	public void createDataEntry(FunctionInfo func) throws JsonSyntaxException, IOException {
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
			acc.getAccountData().unsafeAccessor().create(payload.get("key").getAsString(), payload.get("value"));
			resp.addProperty("success", true);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@Function(allowedMethods = { "POST" }, value = "accounts/dataEntryExists")
	public void dataEntryExists(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" }, value = "accounts/deleteDataEntry")
	public void deleteDataEntry(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" }, value = "accounts/deleteAccount")
	public void deleteAccount(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" }, value = "accounts/updateLastLoginTime")
	public void updateLastLoginTime(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" }, value = "accounts/setStrictChatFilterEnabled")
	public void setStrictChatFilterEnabled(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" }, value = "accounts/setChatEnabled")
	public void setChatEnabled(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" }, value = "accounts/setMultiplayerEnabled")
	public void setMultiplayerEnabled(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" }, value = "accounts/isStrictChatFilterEnabled")
	public void isStrictChatFilterEnabled(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" }, value = "accounts/isChatEnabled")
	public void isChatEnabled(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" }, value = "accounts/isMultiplayerEnabled")
	public void isMultiplayerEnabled(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" }, value = "accounts/isGuestAccount")
	public void isGuestAccount(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" }, value = "accounts/migrateToNormalAccountFromGuest")
	public void migrateToNormalAccountFromGuest(FunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = AccountManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("id") || !payload.has("newName") || !payload.has("password")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		AccountObject acc = manager.getAccount(payload.get("id").getAsString());
		JsonObject resp = new JsonObject();
		if (acc != null) {
			resp.addProperty("success", acc.migrateToNormalAccountFromGuest(payload.get("newName").getAsString(),
					payload.get("password").getAsString().toCharArray()));
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@Function(allowedMethods = { "POST" }, value = "accounts/updatePassword")
	public void updatePassword(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" }, value = "accounts/updateUsername")
	public void updateUsername(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" }, value = "accounts/getRegistrationTimestamp")
	public void getRegistrationTimestamp(FunctionInfo func) throws JsonSyntaxException, IOException {
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

	@Function(allowedMethods = { "POST" }, value = "accounts/getLastLoginTime")
	public void getLastLoginTime(FunctionInfo func) throws JsonSyntaxException, IOException {
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

}
