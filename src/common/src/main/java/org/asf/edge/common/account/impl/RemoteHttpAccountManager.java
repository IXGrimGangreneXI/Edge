package org.asf.edge.common.account.impl;

import java.io.File;
import java.io.IOException;
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
		}

		// Load url
		urlBase = config.get("url").getAsString();
		if (!urlBase.endsWith("/"))
			urlBase += "/";
		logger.info("Using remote account manager server, url: " + urlBase);
		logger.warn("Warning: the http-based remote account manager service is not efficient!");
		logger.warn("Warning: its highly recommened to use a different implementation, such as a database server.");
	}

	@Override
	public boolean isValidUsername(String username) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isUsernameTaken(String username) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getAccountID(String username) {
		// TODO Auto-generated method stub
		return null;
	}

}
