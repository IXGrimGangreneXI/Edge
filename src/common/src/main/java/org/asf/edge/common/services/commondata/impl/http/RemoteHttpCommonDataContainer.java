package org.asf.edge.common.services.commondata.impl.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.asf.edge.common.services.commondata.CommonDataContainer;
import org.asf.edge.common.util.SimpleBinaryMessageClient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;

public class RemoteHttpCommonDataContainer extends CommonDataContainer {

	private String rootNodeName;
	private RemoteHttpCommonDataManager mgr;

	private Logger logger = LogManager.getLogger("CommonDataManager");

	public RemoteHttpCommonDataContainer(String rootNodeName, RemoteHttpCommonDataManager mgr) {
		this.rootNodeName = rootNodeName;
		this.mgr = mgr;
	}

	@Override
	protected JsonElement get(String key) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("node", rootNodeName);
			payload.addProperty("key", key);
			JsonObject response = mgr.commonDataManagerRequest("getDataEntry", payload);
			if (!response.get("success").getAsBoolean())
				return null;
			return response.get("entryValue");
		} catch (IOException e) {
			logger.error("Common data manager server query failure occurred in getDataEntry!", e);
			return null;
		}
	}

	@Override
	protected void set(String key, JsonElement value) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("node", rootNodeName);
			payload.addProperty("key", key);
			payload.add("value", value);
			JsonObject response = mgr.commonDataManagerRequest("setDataEntry", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success=false");
		} catch (IOException e) {
			logger.error("Common data manager server query failure occurred in setDataEntry!", e);
		}
	}

	@Override
	protected void create(String key, String parent, JsonElement value) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("node", rootNodeName);
			payload.addProperty("key", key);
			payload.addProperty("parent", parent);
			payload.add("value", value);
			JsonObject response = mgr.commonDataManagerRequest("createDataEntry", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success=false");
		} catch (IOException e) {
			logger.error("Common data manager server query failure occurred in setDataEntry!", e);
		}
	}

	@Override
	protected boolean exists(String key) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("node", rootNodeName);
			payload.addProperty("key", key);
			JsonObject response = mgr.commonDataManagerRequest("dataEntryExists", payload);
			return response.get("result").getAsBoolean();
		} catch (IOException e) {
			logger.error("Common data manager server query failure occurred in dataEntryExists!", e);
			return false;
		}
	}

	@Override
	protected void delete(String key) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("node", rootNodeName);
			payload.addProperty("key", key);
			JsonObject response = mgr.commonDataManagerRequest("deleteDataEntry", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success=false");
		} catch (IOException e) {
			logger.error("Common data manager server query failure occurred in deleteDataEntry!", e);
		}
	}

	@Override
	protected String[] getEntryKeys(String key) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("node", rootNodeName);
			payload.addProperty("key", key);
			JsonObject response = mgr.commonDataManagerRequest("getEntryKeys", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success=false");
			JsonArray arr = response.get("result").getAsJsonArray();
			String[] res = new String[arr.size()];
			int i = 0;
			for (JsonElement ele : arr)
				res[i++] = ele.getAsString();
			return res;
		} catch (IOException e) {
			logger.error("Common data manager server query failure occurred in getEntryKeys!", e);
			return new String[0];
		}
	}

	@Override
	protected String[] getChildContainers(String key) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("node", rootNodeName);
			payload.addProperty("key", key);
			JsonObject response = mgr.commonDataManagerRequest("getChildContainers", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success=false");
			JsonArray arr = response.get("result").getAsJsonArray();
			String[] res = new String[arr.size()];
			int i = 0;
			for (JsonElement ele : arr)
				res[i++] = ele.getAsString();
			return res;
		} catch (IOException e) {
			logger.error("Common data manager server query failure occurred in getChildContainers!", e);
			return new String[0];
		}
	}

	@Override
	protected void deleteContainer(String root) throws IOException {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("node", rootNodeName);
			payload.addProperty("root", root);
			JsonObject response = mgr.commonDataManagerRequest("deleteContainer", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success=false");
		} catch (IOException e) {
			logger.error("Common data manager server query failure occurred in deleteContainer!", e);
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
			payload.addProperty("node", rootNodeName);
			payload.addProperty("root", root);
			Socket conn = mgr.commonDataManagerUpgradeRequest("runFor", payload, "EDGEBINPROT/CDATAMANAGER/RUNFOR",
					"EDGEBINPROT/CDATAMANAGER/RUNFOR");

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
					if (res)
						return false;
				} catch (Exception e) {
					logger.error("Exception occurred while running find!", e);
					return false;
				}
				return true;
			}, conn.getInputStream(), conn.getOutputStream());
			binH.start();
			conn.close();
			return (JsonElement) cont.res;
		} catch (IOException e) {
			logger.error("Common data manager server query failure occurred in find!", e);
		}
		return null;
	}

	@Override
	protected void runFor(BiFunction<String, JsonElement, Boolean> function, String root) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("node", rootNodeName);
			payload.addProperty("root", root);
			Socket conn = mgr.commonDataManagerUpgradeRequest("runFor", payload, "EDGEBINPROT/CDATAMANAGER/RUNFOR",
					"EDGEBINPROT/CDATAMANAGER/RUNFOR");

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
					logger.error("Exception occurred while running runFor!", e);
					return false;
				}
				return true;
			}, conn.getInputStream(), conn.getOutputStream());
			binH.start();
			conn.close();
		} catch (IOException e) {
			logger.error("Common data manager server query failure occurred in runFor!", e);
		}
	}

	@Override
	protected void runForChildren(Function<String, Boolean> function, String root) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("node", rootNodeName);
			payload.addProperty("root", root);
			Socket conn = mgr.commonDataManagerUpgradeRequest("runForChildren", payload,
					"EDGEBINPROT/CDATAMANAGER/RUNFORCHILDREN", "EDGEBINPROT/CDATAMANAGER/RUNFORCHILDREN");

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
					logger.error("Exception occurred while running runForChildren!", e);
					return false;
				}
				return true;
			}, conn.getInputStream(), conn.getOutputStream());
			binH.start();
			conn.close();
		} catch (IOException e) {
			logger.error("Common data manager server query failure occurred in runForChildren!", e);
		}
	}

}
