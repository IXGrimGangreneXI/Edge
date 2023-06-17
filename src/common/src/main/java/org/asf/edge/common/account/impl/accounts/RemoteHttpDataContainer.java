package org.asf.edge.common.account.impl.accounts;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.account.AccountDataContainer;
import org.asf.edge.common.account.impl.RemoteHttpAccountManager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class RemoteHttpDataContainer extends AccountDataContainer {

	private String id;
	private RemoteHttpAccountManager mgr;

	private Logger logger = LogManager.getLogger("AccountManager");

	public RemoteHttpDataContainer(String id, RemoteHttpAccountManager mgr) {
		this.id = id;
		this.mgr = mgr;
	}

	@Override
	protected JsonElement get(String key) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			payload.addProperty("key", key);
			JsonObject response = mgr.accountManagerRequest("accounts/getDataEntry", payload);
			if (!response.get("success").getAsBoolean())
				return null;
			return response.get("entryValue");
		} catch (IOException e) {
			logger.error("Account server query failure occurred in getDataEntry!", e);
			return null;
		}
	}

	@Override
	protected void set(String key, JsonElement value) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			payload.addProperty("key", key);
			payload.add("value", value);
			JsonObject response = mgr.accountManagerRequest("accounts/setDataEntry", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success=false");
		} catch (IOException e) {
			logger.error("Account server query failure occurred in setDataEntry!", e);
		}
	}

	@Override
	protected void create(String key, JsonElement value) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			payload.addProperty("key", key);
			payload.add("value", value);
			JsonObject response = mgr.accountManagerRequest("accounts/createDataEntry", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success=false");
		} catch (IOException e) {
			logger.error("Account server query failure occurred in setDataEntry!", e);
		}
	}

	@Override
	protected boolean exists(String key) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			payload.addProperty("key", key);
			JsonObject response = mgr.accountManagerRequest("accounts/dataEntryExists", payload);
			return response.get("result").getAsBoolean();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in dataEntryExists!", e);
			return false;
		}
	}

	@Override
	protected void delete(String key) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			payload.addProperty("key", key);
			JsonObject response = mgr.accountManagerRequest("accounts/deleteDataEntry", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success=false");
		} catch (IOException e) {
			logger.error("Account server query failure occurred in deleteDataEntry!", e);
		}
	}

}
