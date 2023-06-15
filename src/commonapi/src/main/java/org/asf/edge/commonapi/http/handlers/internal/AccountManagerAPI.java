package org.asf.edge.commonapi.http.handlers.internal;

import java.io.IOException;

import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.account.AccountManager;
import org.asf.edge.common.http.apihandlerutils.BaseApiHandler;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.commonapi.EdgeCommonApiServer;

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

}
