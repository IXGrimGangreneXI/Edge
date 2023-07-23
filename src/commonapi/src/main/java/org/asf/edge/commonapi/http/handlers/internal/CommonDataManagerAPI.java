package org.asf.edge.commonapi.http.handlers.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.asf.connective.processors.HttpPushProcessor;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionResult;
import org.asf.edge.common.http.apihandlerutils.functions.LegacyFunction;
import org.asf.edge.common.http.apihandlerutils.functions.LegacyFunctionInfo;
import org.asf.edge.common.services.commondata.CommonDataContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.util.SimpleBinaryMessageClient;
import org.asf.edge.commonapi.EdgeCommonApiServer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class CommonDataManagerAPI extends EdgeWebService<EdgeCommonApiServer> {

	private CommonDataManager manager;

	public CommonDataManagerAPI(EdgeCommonApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new CommonDataManagerAPI(getServerInstance());
	}

	@Override
	public String path() {
		return "/commondatamanager";
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void setupContainer(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = CommonDataManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("node")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		JsonObject resp = new JsonObject();
		manager.getContainer(payload.get("node").getAsString());
		resp.addProperty("success", true);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getDataEntry(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = CommonDataManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("node") || !payload.has("key")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		CommonDataContainer data = manager.getContainer(payload.get("node").getAsString());
		JsonObject resp = new JsonObject();
		if (data != null) {
			JsonElement val = data.unsafeAccessor().get(payload.get("key").getAsString());
			resp.addProperty("success", val != null);
			if (val != null)
				resp.add("entryValue", val);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void setDataEntry(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = CommonDataManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("node") || !payload.has("key") || !payload.has("value")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		CommonDataContainer data = manager.getContainer(payload.get("node").getAsString());
		JsonObject resp = new JsonObject();
		if (data != null) {
			data.unsafeAccessor().set(payload.get("key").getAsString(), payload.get("value"));
			resp.addProperty("success", true);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void createDataEntry(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = CommonDataManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("node") || !payload.has("key") || !payload.has("parent") || !payload.has("value")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		CommonDataContainer data = manager.getContainer(payload.get("node").getAsString());
		JsonObject resp = new JsonObject();
		if (data != null) {
			data.unsafeAccessor().create(payload.get("key").getAsString(), payload.get("parent").getAsString(),
					payload.get("value"));
			resp.addProperty("success", true);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void dataEntryExists(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = CommonDataManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("node") || !payload.has("key")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		CommonDataContainer data = manager.getContainer(payload.get("node").getAsString());
		JsonObject resp = new JsonObject();
		if (data != null) {
			resp.addProperty("result", data.unsafeAccessor().exists(payload.get("key").getAsString()));
		} else
			resp.addProperty("result", false);
		setResponseContent("text/json", resp.toString());
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void deleteDataEntry(LegacyFunctionInfo func) throws JsonSyntaxException, IOException {
		// Load manager
		if (manager == null)
			manager = CommonDataManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("node") || !payload.has("key")) {
			getResponse().setResponseStatus(400, "Bad request");
			return;
		}

		// Send response
		CommonDataContainer data = manager.getContainer(payload.get("node").getAsString());
		JsonObject resp = new JsonObject();
		if (data != null) {
			data.unsafeAccessor().delete(payload.get("key").getAsString());
			resp.addProperty("success", true);
		} else
			resp.addProperty("success", false);
		setResponseContent("text/json", resp.toString());
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult deleteContainer(FunctionInfo func) throws IOException {
		// Load manager
		if (manager == null)
			manager = CommonDataManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			return response(400, "Bad request");
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("node") || !payload.has("root")) {
			return response(400, "Bad request");
		}

		// Send response
		CommonDataContainer data = manager.getContainer(payload.get("node").getAsString());
		JsonObject resp = new JsonObject();
		if (data != null) {
			data.unsafeAccessor().deleteContainer(payload.get("root").getAsString());
			resp.addProperty("success", true);
		} else
			resp.addProperty("success", false);
		return ok("text/json", resp.toString());
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult getChildContainers(FunctionInfo func) throws IOException {
		// Load manager
		if (manager == null)
			manager = CommonDataManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			return response(400, "Bad request");
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("node") || !payload.has("key")) {
			return response(400, "Bad request");
		}

		// Send response
		CommonDataContainer data = manager.getContainer(payload.get("node").getAsString());
		JsonObject resp = new JsonObject();
		if (data != null) {
			String[] containers = data.unsafeAccessor().getChildContainers(payload.get("key").getAsString());
			JsonArray arr = new JsonArray();
			for (String cont : containers)
				arr.add(cont);
			resp.addProperty("success", true);
			resp.add("result", arr);
		} else
			resp.addProperty("success", false);
		return ok("text/json", resp.toString());
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult getEntryKeys(FunctionInfo func) throws IOException {
		// Load manager
		if (manager == null)
			manager = CommonDataManager.getInstance();

		// Read payload
		if (!getRequest().hasRequestBody()) {
			return response(400, "Bad request");
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("node") || !payload.has("key")) {
			return response(400, "Bad request");
		}

		// Send response
		CommonDataContainer data = manager.getContainer(payload.get("node").getAsString());
		JsonObject resp = new JsonObject();
		if (data != null) {
			String[] keys = data.unsafeAccessor().getEntryKeys(payload.get("key").getAsString());
			JsonArray arr = new JsonArray();
			for (String cont : keys)
				arr.add(cont);
			resp.addProperty("success", true);
			resp.add("result", arr);
		} else
			resp.addProperty("success", false);
		return ok("text/json", resp.toString());
	}

	private class FuncResult {
		public boolean continueRun;
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult runForChildren(FunctionInfo func) throws IOException {
		// Load manager
		if (manager == null)
			manager = CommonDataManager.getInstance();

		// Check headers
		if (!getRequest().hasHeader("Upgrade")
				|| !getRequest().getHeaderValue("Upgrade").equals("EDGEBINPROT/CDATAMANAGER/RUNFORCHILDREN")) {
			return response(400, "Bad request");
		}

		// Read payload
		if (!getRequest().hasRequestBody()) {
			return response(400, "Bad request");
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("node") || !payload.has("root")) {
			return response(400, "Bad request");
		}

		// Set headers
		setResponseHeader("X-Response-ID", UUID.randomUUID().toString());
		setResponseHeader("Upgrade", "EDGEBINPROT/CDATAMANAGER/RUNFORCHILDREN");

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
					manager.getContainer(payload.get("node").getAsString()).unsafeAccessor().runForChildren(t -> {
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

	@Function(allowedMethods = { "POST" })
	public FunctionResult runFor(FunctionInfo func) throws IOException {
		// Load manager
		if (manager == null)
			manager = CommonDataManager.getInstance();

		// Check headers
		if (!getRequest().hasHeader("Upgrade")
				|| !getRequest().getHeaderValue("Upgrade").equals("EDGEBINPROT/CDATAMANAGER/RUNFOR")) {
			return response(400, "Bad request");
		}

		// Read payload
		if (!getRequest().hasRequestBody()) {
			return response(400, "Bad request");
		}
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("node") || !payload.has("root")) {
			return response(400, "Bad request");
		}

		// Set headers
		setResponseHeader("X-Response-ID", UUID.randomUUID().toString());
		setResponseHeader("Upgrade", "EDGEBINPROT/CDATAMANAGER/RUNFOR");

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
					manager.getContainer(payload.get("node").getAsString()).unsafeAccessor().runFor((name, data) -> {
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

}
