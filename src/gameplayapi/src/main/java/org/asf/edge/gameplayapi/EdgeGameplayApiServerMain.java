package org.asf.edge.gameplayapi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.CommonInit;
import org.asf.edge.common.account.AccountManager;
import org.asf.edge.common.services.ServiceImplementationPriorityLevels;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.modules.ModuleManager;
import org.asf.edge.modules.eventbus.EventBus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.asf.edge.gameplayapi.events.config.*;
import org.asf.edge.gameplayapi.config.GameplayApiServerConfig;

public class EdgeGameplayApiServerMain {
	public static void main(String[] args) throws IOException {
		// Splash
		EdgeGameplayApiServer.printSplash();

		// Common init
		CommonInit.initAll();

		// Logger
		Logger logger = LogManager.getLogger("GAMEPLAYAPI");
		logger.info("Preparing to start...");

		// Load modules
		ModuleManager.init();

		// Create config object
		GameplayApiServerConfig config = new GameplayApiServerConfig();
		EventBus.getInstance().dispatchEvent(new GameplayApiServerConfigPresetupEvent(config));

		// Load configuration
		logger.info("Loading server configuration...");
		File configFile = new File("server.json");
		if (!configFile.exists()) {
			logger.debug("Creating server configuration...");
			if (config.server == null) {
				// Write config
				Files.writeString(configFile.toPath(), "{\n" //
						+ "\n" //
						+ "    \"listenAddress\": \"0.0.0.0\",\n" // listen address
						+ "    \"listenPort\": 5320,\n" // port to listen on
						+ "    \"apiRequestListenPath\": \"/\",\n" // URI to listen on
						+ "\n" //
						+ "    \"https\": false,\n" // use https?
						+ "    \"tlsKeystore\": null,\n" // keystore file
						+ "    \"tlsKeystorePassword\": null,\n" // keystore password
						+ "\n" //
						+ "    \"modules\": {\n" //
						+ "    }\n" //
						+ "\n" //
						+ "}");
			} else {
				// Write config
				Files.writeString(configFile.toPath(), "{\n" //
						+ "\n" //
						+ "    \"apiRequestListenPath\": \"/\",\n" // URI to listen on
						+ "\n" //
						+ "    \"modules\": {\n" //
						+ "    }\n" //
						+ "\n" //
						+ "}");
			}
		}

		// Load the json
		logger.debug("Loading configuration file server.json...");
		JsonObject configData = JsonParser.parseString(Files.readString(configFile.toPath())).getAsJsonObject();

		// Load server settings
		if (config.server == null) {
			logger.debug("Loading listening settings...");
			config.listenAddress = configData.get("listenAddress").getAsString();
			config.listenPort = configData.get("listenPort").getAsInt();
		}
		logger.debug("Loading IO settings...");
		config.apiRequestListenPath = configData.get("apiRequestListenPath").getAsString();
		if (config.server == null) {
			logger.debug("Loading encryption settings...");
			config.https = configData.get("https").getAsBoolean();
			if (config.https) {
				config.tlsKeystore = configData.get("tlsKeystore").getAsString();
				config.tlsKeystorePassword = configData.get("tlsKeystorePassword").getAsString();
			}
		}

		// Load module settings
		if (configData.has("modules")) {
			logger.debug("Loading module configurations...");
			JsonObject moduleSettings = configData.get("modules").getAsJsonObject();

			// Load each module
			for (String id : moduleSettings.keySet()) {
				// Load module config object
				JsonObject conf = moduleSettings.get(id).getAsJsonObject();
				LinkedHashMap<String, String> mp = new LinkedHashMap<String, String>();
				for (String key : conf.keySet())
					mp.put(key, conf.get(key).getAsString());
				ModuleManager.loadedModuleConfig(id, mp);
			}
		}

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new GameplayApiServerConfigLoadedEvent(config));

		// Prepare service
		logger.info("Setting up the server...");
		logger.debug("Loading account manager implementations...");
		AccountManager.initAccountManagerServices(ServiceImplementationPriorityLevels.NORMAL,
				ServiceImplementationPriorityLevels.DEFAULT);
		logger.debug("Selecting account manager implementation...");
		ServiceManager.selectServiceImplementation(AccountManager.class);
		logger.debug("Loading account manager...");
		AccountManager.getInstance().loadManager();

		// Setup server
		EdgeGameplayApiServer server = new EdgeGameplayApiServer(config);
		server.setupServer();

		// Start server
		server.startServer();

		// Call post-init
		ModuleManager.runModulePostInit();

		// Wait for exit
		logger.info("Server is running!");
		server.waitForExit();
	}

}
