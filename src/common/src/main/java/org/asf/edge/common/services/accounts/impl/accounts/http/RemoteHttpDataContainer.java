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

public class RemoteHttpDataContainer extends AccountDataContainer {

	private String id;
	private RemoteHttpAccountManager mgr;

	private Logger logger = LogManager.getLogger("AccountManager");
	private AccountObject account;

	public RemoteHttpDataContainer(AccountObject account, String id, RemoteHttpAccountManager mgr) {
		this.id = id;
		this.mgr = mgr;
		this.account = account;
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
	protected void create(String key, String root, JsonElement value) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			payload.addProperty("key", key);
			payload.addProperty("root", root);
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

	@Override
	public AccountObject getAccount() {
		return account;
	}

	@Override
	public AccountSaveContainer getSave() {
		return null;
	}

	@Override
	protected String[] getEntryKeys(String key) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			payload.addProperty("key", key);
			JsonObject response = mgr.accountManagerRequest("accounts/getEntryKeys", payload);
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
			payload.addProperty("id", id);
			payload.addProperty("key", key);
			JsonObject response = mgr.accountManagerRequest("accounts/getChildContainers", payload);
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
			payload.addProperty("id", id);
			payload.addProperty("root", root);
			JsonObject response = mgr.accountManagerRequest("accounts/deleteContainer", payload);
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
			payload.addProperty("id", id);
			payload.addProperty("root", root);
			Socket conn = mgr.accountManagerUpgradeRequest("runForDataEntries", payload,
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
					logger.error("Exception occurred while running findDataEntry!", e);
					return false;
				}
				return true;
			}, conn.getInputStream(), conn.getOutputStream());
			binH.start();
			conn.close();
			return (JsonElement) cont.res;
		} catch (IOException e) {
			logger.error("Account server query failure occurred in findDataEntry!", e);
		}
		return null;
	}

	@Override
	protected void runFor(BiFunction<String, JsonElement, Boolean> function, String root) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			payload.addProperty("root", root);
			Socket conn = mgr.accountManagerUpgradeRequest("runForDataEntries", payload,
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
					logger.error("Exception occurred while running runForDataEntries!", e);
					return false;
				}
				return true;
			}, conn.getInputStream(), conn.getOutputStream());
			binH.start();
			conn.close();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in runForDataEntries!", e);
		}
	}

	@Override
	protected void runForChildren(Function<String, Boolean> function, String root) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			payload.addProperty("root", root);
			Socket conn = mgr.accountManagerUpgradeRequest("runForDataChildren", payload,
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
					logger.error("Exception occurred while running runForDataChildren!", e);
					return false;
				}
				return true;
			}, conn.getInputStream(), conn.getOutputStream());
			binH.start();
			conn.close();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in runForDataChildren!", e);
		}
	}

}
