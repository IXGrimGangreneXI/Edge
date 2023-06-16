package org.asf.edge.common.account.impl;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.account.AccountManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class RemoteHttpAccountManager extends AccountManager {

	private String urlBase = "http://127.0.0.1:5324/accountmanager/";
	private Logger logger = LogManager.getLogger("AccountManager");

	@Override
	public void initService() {
	}

	@Override
	public void loadManager() {
		// Write/load config
		File configFile = new File("accountmanager.json");
		JsonObject accountManagerConfig = new JsonObject();
		if (configFile.exists()) {
			try {
				accountManagerConfig = JsonParser.parseString(Files.readString(configFile.toPath())).getAsJsonObject();
			} catch (JsonSyntaxException | IOException e) {
				logger.error("Failed to load account manager configuration!", e);
				return;
			}
		}
		JsonObject config = new JsonObject();
		if (!accountManagerConfig.has("remoteHttpManager")) {
			// Generate config
			config.addProperty("url", urlBase);
			accountManagerConfig.add("remoteHttpManager", config);

			// Write config
			try {
				Files.writeString(configFile.toPath(),
						new Gson().newBuilder().setPrettyPrinting().create().toJson(accountManagerConfig));
			} catch (IOException e) {
				logger.error("Failed to write the account manager configuration!", e);
				return;
			}
		} else
			config = accountManagerConfig.get("remoteHttpManager").getAsJsonObject();

		// Load url
		urlBase = config.get("url").getAsString();
		if (!urlBase.endsWith("/"))
			urlBase += "/";
		logger.info("Using remote account manager server, url: " + urlBase);
		logger.warn("Warning: the http-based remote account manager service is not efficient!");
		logger.warn("Warning: its highly recommened to use a different implementation, such as a database server.");
	}

	private JsonObject accountManagerRequest(String function, JsonObject payload) throws IOException {
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

	@Override
	public boolean isValidUsername(String username) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("username", username);
			JsonObject response = accountManagerRequest("isValidUsername", payload);
			return response.get("result").getAsBoolean();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in isValidUsername!", e);
			return false;
		}
	}

	@Override
	public boolean isValidPassword(String password) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("password", password);
			JsonObject response = accountManagerRequest("isValidPassword", payload);
			return response.get("result").getAsBoolean();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in isValidUsername!", e);
			return false;
		}
	}

	@Override
	public boolean isUsernameTaken(String username) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("username", username);
			JsonObject response = accountManagerRequest("isUsernameTaken", payload);
			return response.get("result").getAsBoolean();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in isUsernameTaken!", e);
			return false;
		}
	}

	@Override
	public String getAccountID(String username) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("username", username);
			JsonObject response = accountManagerRequest("getAccountID", payload);
			if (!response.get("success").getAsBoolean())
				return null;
			return response.get("id").getAsString();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in getAccountID!", e);
			return null;
		}
	}

	@Override
	public boolean verifyPassword(String id, String password) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			payload.addProperty("password", password);
			JsonObject response = accountManagerRequest("verifyPassword", payload);
			return response.get("result").getAsBoolean();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in isValidUsername!", e);
			return false;
		}
	}

	@Override
	public boolean accountExists(String id) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			JsonObject response = accountManagerRequest("accountExists", payload);
			return response.get("result").getAsBoolean();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in accountExists!", e);
			return false;
		}
	}

}
