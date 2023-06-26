package org.asf.edge.common.services.accounts.impl.accounts;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.impl.RemoteHttpAccountManager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class RemoteHttpSaveDataContainer extends AccountDataContainer {

	private String id;
	private String acc;
	private RemoteHttpAccountManager mgr;

	private Logger logger = LogManager.getLogger("AccountManager");

	public RemoteHttpSaveDataContainer(String id, String acc, RemoteHttpAccountManager mgr) {
		this.id = id;
		this.acc = acc;
		this.mgr = mgr;
	}

	@Override
	protected JsonElement get(String key) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", acc);
			payload.addProperty("save", id);
			payload.addProperty("key", key);
			JsonObject response = mgr.accountManagerRequest("accounts/getSaveDataEntry", payload);
			if (!response.get("success").getAsBoolean())
				return null;
			return response.get("entryValue");
		} catch (IOException e) {
			logger.error("Account server query failure occurred in getSaveDataEntry!", e);
			return null;
		}
	}

	@Override
	protected void set(String key, JsonElement value) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", acc);
			payload.addProperty("save", id);
			payload.addProperty("key", key);
			payload.add("value", value);
			JsonObject response = mgr.accountManagerRequest("accounts/setSaveDataEntry", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success=false");
		} catch (IOException e) {
			logger.error("Account server query failure occurred in setSaveDataEntry!", e);
		}
	}

	@Override
	protected void create(String key, JsonElement value) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", acc);
			payload.addProperty("save", id);
			payload.addProperty("key", key);
			payload.add("value", value);
			JsonObject response = mgr.accountManagerRequest("accounts/createSaveDataEntry", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success=false");
		} catch (IOException e) {
			logger.error("Account server query failure occurred in createSaveDataEntry!", e);
		}
	}

	@Override
	protected boolean exists(String key) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", acc);
			payload.addProperty("save", id);
			payload.addProperty("key", key);
			JsonObject response = mgr.accountManagerRequest("accounts/saveDataEntryExists", payload);
			return response.get("result").getAsBoolean();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in saveDataEntryExists!", e);
			return false;
		}
	}

	@Override
	protected void delete(String key) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", acc);
			payload.addProperty("save", id);
			payload.addProperty("key", key);
			JsonObject response = mgr.accountManagerRequest("accounts/deleteSaveDataEntry", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success=false");
		} catch (IOException e) {
			logger.error("Account server query failure occurred in deleteSaveDataEntry!", e);
		}
	}

}
