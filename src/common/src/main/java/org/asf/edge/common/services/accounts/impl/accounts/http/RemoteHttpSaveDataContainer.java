package org.asf.edge.common.services.accounts.impl.accounts.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.accounts.impl.RemoteHttpAccountManager;
import org.asf.edge.common.util.SimpleBinaryMessageClient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

	private class ResultContainer {
		public Object res;
	}

	@Override
	protected JsonElement find(BiFunction<String, JsonElement, Boolean> function, String root) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", acc);
			payload.addProperty("save", save.getSaveID());
			payload.addProperty("root", root);
			Socket conn = mgr.accountManagerUpgradeRequest("runForSaveDataEntries", payload,
					"EDGEBINPROT/ACCMANAGER/RUNFOR", "EDGEBINPROT/ACCMANAGER/RUNFOR");

			// Run function
			ResultContainer cont = new ResultContainer();
			SimpleBinaryMessageClient binH = new SimpleBinaryMessageClient((packet, client) -> {
				// Read message
				try {
					// Parse
					ByteArrayInputStream bIn = new ByteArrayInputStream(packet.data);
					byte[] nameB = bIn.readNBytes(ByteBuffer.wrap(bIn.readNBytes(4)).getInt());
					String name = new String(nameB, "UTF-8");
					byte[] dataB = bIn.readNBytes(ByteBuffer.wrap(bIn.readNBytes(4)).getInt());
					String data = new String(dataB, "UTF-8");

					// Handle
					JsonElement d = JsonParser.parseString(data);
					boolean res = function.apply(name, d);
					if (res)
						cont.res = d;

					// Send response
					client.send(new byte[] { !res ? (byte) 1 : (byte) 0 });
					if (!res)
						return false;
				} catch (Exception e) {
					logger.error("Exception occurred while running findSaveDataEntry!", e);
					return false;
				}
				return true;
			}, conn.getInputStream(), conn.getOutputStream());
			binH.start();
			conn.close();
			return (JsonElement) cont.res;
		} catch (IOException e) {
			logger.error("Account server query failure occurred in findSaveDataEntry!", e);
		}
		return null;
	}

	@Override
	protected void runFor(BiFunction<String, JsonElement, Boolean> function, String root) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", acc);
			payload.addProperty("save", save.getSaveID());
			payload.addProperty("root", root);
			Socket conn = mgr.accountManagerUpgradeRequest("runForSaveDataEntries", payload,
					"EDGEBINPROT/ACCMANAGER/RUNFOR", "EDGEBINPROT/ACCMANAGER/RUNFOR");

			// Run function
			SimpleBinaryMessageClient binH = new SimpleBinaryMessageClient((packet, client) -> {
				// Read message
				try {
					// Parse
					ByteArrayInputStream bIn = new ByteArrayInputStream(packet.data);
					byte[] nameB = bIn.readNBytes(ByteBuffer.wrap(bIn.readNBytes(4)).getInt());
					String name = new String(nameB, "UTF-8");
					byte[] dataB = bIn.readNBytes(ByteBuffer.wrap(bIn.readNBytes(4)).getInt());
					String data = new String(dataB, "UTF-8");

					// Handle
					boolean res = function.apply(name, JsonParser.parseString(data));

					// Send response
					client.send(new byte[] { res ? (byte) 1 : (byte) 0 });
					if (!res)
						return false;
				} catch (Exception e) {
					logger.error("Exception occurred while running runForSaveDataEntries!", e);
					return false;
				}
				return true;
			}, conn.getInputStream(), conn.getOutputStream());
			binH.start();
			conn.close();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in runForSaveDataEntries!", e);
		}
	}

	@Override
	protected void runForChildren(Function<String, Boolean> function, String root) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", acc);
			payload.addProperty("save", save.getSaveID());
			payload.addProperty("root", root);
			Socket conn = mgr.accountManagerUpgradeRequest("runForSaveDataChildren", payload,
					"EDGEBINPROT/ACCMANAGER/RUNFORCHILDREN", "EDGEBINPROT/ACCMANAGER/RUNFORCHILDREN");

			// Run function
			SimpleBinaryMessageClient binH = new SimpleBinaryMessageClient((packet, client) -> {
				// Read message
				try {
					String name = new String(packet.data, "UTF-8");

					// Handle
					boolean res = function.apply(name);

					// Send response
					client.send(new byte[] { res ? (byte) 1 : (byte) 0 });
					if (!res)
						return false;
				} catch (Exception e) {
					logger.error("Exception occurred while running runForSaveDataChildren!", e);
					return false;
				}
				return true;
			}, conn.getInputStream(), conn.getOutputStream());
			binH.start();
			conn.close();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in runForSaveDataChildren!", e);
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
