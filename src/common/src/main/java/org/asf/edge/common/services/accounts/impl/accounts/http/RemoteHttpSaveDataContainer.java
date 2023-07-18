package org.asf.edge.common.services.accounts.impl.accounts.http;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.accounts.impl.RemoteHttpAccountManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class RemoteHttpSaveDataContainer extends AccountDataContainer {

	private String acc;
	private AccountSaveContainer save;
	private RemoteHttpAccountManager mgr;

	private Logger logger = LogManager.getLogger("AccountManager");
	private AccountObject account;

	public RemoteHttpSaveDataContainer(AccountObject account, AccountSaveContainer save, String acc,
			RemoteHttpAccountManager mgr) {
		this.account = account;
		this.save = save;
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
			payload.addProperty("save", save.getSaveID());
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
			payload.addProperty("save", save.getSaveID());
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
	protected void create(String key, String root, JsonElement value) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", acc);
			payload.addProperty("save", save.getSaveID());
			payload.addProperty("key", key);
			payload.addProperty("root", root);
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
			payload.addProperty("save", save.getSaveID());
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
			payload.addProperty("save", save.getSaveID());
			payload.addProperty("key", key);
			JsonObject response = mgr.accountManagerRequest("accounts/deleteSaveDataEntry", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success=false");
		} catch (IOException e) {
			logger.error("Account server query failure occurred in deleteSaveDataEntry!", e);
		}
	}

	@Override
	protected String[] getEntryKeys(String key) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", acc);
			payload.addProperty("save", save.getSaveID());
			payload.addProperty("key", key);
			JsonObject response = mgr.accountManagerRequest("accounts/getSaveEntryKeys", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success=false");
			JsonArray arr = response.get("result").getAsJsonArray();
			String[] res = new String[arr.size()];
			int i = 0;
			for (JsonElement ele : arr)
				res[i++] = ele.getAsString();
			return res;
		} catch (IOException e) {
			logger.error("Account server query failure occurred in getEntryKeys!", e);
			return new String[0];
		}
	}

	@Override
	protected String[] getChildContainers(String key) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", acc);
			payload.addProperty("save", save.getSaveID());
			payload.addProperty("key", key);
			JsonObject response = mgr.accountManagerRequest("accounts/getChildSaveContainers", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success=false");
			JsonArray arr = response.get("result").getAsJsonArray();
			String[] res = new String[arr.size()];
			int i = 0;
			for (JsonElement ele : arr)
				res[i++] = ele.getAsString();
			return res;
		} catch (IOException e) {
			logger.error("Account server query failure occurred in getChildContainers!", e);
			return new String[0];
		}
	}

	@Override
	protected void deleteContainer(String root) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", acc);
			payload.addProperty("save", save.getSaveID());
			payload.addProperty("root", root);
			JsonObject response = mgr.accountManagerRequest("accounts/deleteSaveContainer", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success=false");
		} catch (IOException e) {
			logger.error("Account server query failure occurred in deleteContainer!", e);
		}
	}

	@Override
	public AccountObject getAccount() {
		return account;
	}

	@Override
	public AccountSaveContainer getSave() {
		return save;
	}

}
