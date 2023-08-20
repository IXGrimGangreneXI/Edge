package org.asf.edge.common.services.commondata.impl.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.commondata.CommonDataContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.services.config.ConfigProviderService;
import org.asf.edge.common.util.HttpUpgradeUtil;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RemoteHttpCommonDataManager extends CommonDataManager {

	public String urlBase = "http://127.0.0.1:5324/commondatamanager/";
	private Logger logger = LogManager.getLogger("CommonDataManager");

	@Override
	public void initService() {
	}

	@Override
	public void loadManager() {
		// Write/load config
		JsonObject commonDataManagerConfig;
		try {
			commonDataManagerConfig = ConfigProviderService.getInstance().loadConfig("server", "commondata");
		} catch (IOException e) {
			logger.error("Failed to load common data manager configuration!", e);
			return;
		}
		if (commonDataManagerConfig == null) {
			commonDataManagerConfig = new JsonObject();
		}
		JsonObject config = new JsonObject();
		if (!commonDataManagerConfig.has("remoteHttpManager")) {
			// Generate config
			config.addProperty("url", urlBase);
			commonDataManagerConfig.add("remoteHttpManager", config);

			// Write config
			try {
				ConfigProviderService.getInstance().saveConfig("server", "commondata", commonDataManagerConfig);
			} catch (IOException e) {
				logger.error("Failed to write the common data manager configuration!", e);
				return;
			}
		} else
			config = commonDataManagerConfig.get("remoteHttpManager").getAsJsonObject();

		// Load url
		urlBase = config.get("url").getAsString();
		if (!urlBase.endsWith("/"))
			urlBase += "/";
		logger.info("Using remote common data manager server, url: " + urlBase);
		logger.warn("Warning: the http-based remote common data manager service is not efficient!");
		logger.warn("Warning: its highly recommened to use a different implementation, such as a database server.");
	}

	/**
	 * Creates common data manager requests
	 * 
	 * @param function Function name
	 * @param payload  Payload json
	 * @return Response object
	 * @throws IOException If contacting the server fails
	 */
	public JsonObject commonDataManagerRequest(String function, JsonObject payload) throws IOException {
		// Build url
		String url = urlBase;
		url += function;

		// Open connection
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);

		// Write request
		conn.getOutputStream().write(payload.toString().getBytes("UTF-8"));

		// Check response
		if (conn.getResponseCode() != 200)
			throw new IOException("Server returned HTTP " + conn.getResponseCode() + " " + conn.getResponseMessage());

		// Read response
		try {
			return JsonParser.parseString(new String(conn.getInputStream().readAllBytes(), "UTF-8")).getAsJsonObject();
		} catch (Exception e) {
			throw new IOException("Server returned a non-json response");
		}
	}

	/**
	 * Creates common data manager protocol upgrade requests
	 * 
	 * @param function Function name
	 * @param payload  Payload json
	 * @return Socket instance
	 * @throws IOException If contacting the server fails
	 */
	public Socket commonDataManagerUpgradeRequest(String function, JsonObject payload, String upgradeProtocol,
			String expectedResponseProtocol) throws IOException {
		// Build url
		String url = urlBase;
		url += function;

		// Open connection
		byte[] body = payload.toString().getBytes("UTF-8");
		return HttpUpgradeUtil.upgradeRequest(url, "POST", new ByteArrayInputStream(body), body.length,
				Map.of("X-Request-ID", UUID.randomUUID().toString()), new HashMap<String, String>(), upgradeProtocol,
				expectedResponseProtocol);
	}

	@Override
	protected CommonDataContainer getContainerInternal(String rootNodeName) {
		return new RemoteHttpCommonDataContainer(rootNodeName, this);
	}

	@Override
	protected void setupContainer(String rootNodeName) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("node", rootNodeName);
			JsonObject response = commonDataManagerRequest("setupContainer", payload);
			if (!response.get("success").getAsBoolean())
				throw new IOException("Server returned success = false");
		} catch (IOException e) {
			logger.error("Common data manager server query failure occurred in setupContainer!", e);
		}
	}

}
