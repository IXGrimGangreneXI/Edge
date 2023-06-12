package org.asf.edge.globalserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.CommonInit;
import org.asf.edge.contentserver.EdgeContentServer;
import org.asf.edge.contentserver.config.ContentServerConfig;
import org.asf.edge.contentserver.events.config.ContentServerConfigLoadedEvent;
import org.asf.edge.contentserver.events.config.ContentServerConfigPresetupEvent;
import org.asf.edge.modules.ModuleManager;
import org.asf.edge.modules.eventbus.EventBus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class EdgeGlobalServerMain {
	public static final String GLOBAL_SERVER_VERSION = "1.0.0.A1";

	public static void main(String[] args) throws IOException {
		// Print splash
		System.out.println("-------------------------------------------------------------");
		System.out.println("                                                             ");
		System.out.println("    EDGE - Fan-made server software for School of Dragons    ");
		System.out.println("                Full Server Version: 1.0.0.A1                ");
		System.out.println("                                                             ");
		System.out.println("-------------------------------------------------------------");
		System.out.println("");

		// Common init
		CommonInit.initAll();

		// Logger
		Logger logger = LogManager.getLogger("FULLSERVER");
		logger.info("EDGE Global (full) server is starting!");
		logger.info("Content server version: " + EdgeContentServer.CONTENT_SERVER_VERSION);
		logger.info("Global server version: " + GLOBAL_SERVER_VERSION);
		logger.info("Preparing to start...");

		// Load modules
		ModuleManager.init();

		// Create config objects
		ContentServerConfig contentSrvConfig = new ContentServerConfig();
		EventBus.getInstance().dispatchEvent(new ContentServerConfigPresetupEvent(contentSrvConfig));

		// Load configuration
		logger.info("Loading server configuration...");
		File configFile = new File("server.json");
		if (!configFile.exists()) {
			// Write config
			logger.debug("Creating server configuration...");
			Files.writeString(configFile.toPath(), "{\n" //
					+ "\n" //
					+ "    \"contentServer\": {\n" //
					+ "        \"listenAddress\": \"0.0.0.0\",\n" // listen address
					+ "        \"listenPort\": 5319,\n" // port to listen on
					+ "        \"contentRequestListenPath\": \"/\",\n" // URI to listen on
					+ "        \"contentDataPath\": \"./data/contentserver/asset-data\",\n" // Content data path
					+ "        \"allowIndexingAssets\": true,\n" // Defines if indexing is enabled
					+ "\n" //
					+ "        \"https\": false,\n" // use https?
					+ "        \"tlsKeystore\": null,\n" // keystore file
					+ "        \"tlsKeystorePassword\": null\n" // keystore password
					+ "    },\n" //
					+ "\n" //
					+ "    \"modules\": {\n" //
					+ "    }\n" //
					+ "\n" //
					+ "}");
		}

		// Load the json
		logger.debug("Loading configuration file server.json...");
		JsonObject configData = JsonParser.parseString(Files.readString(configFile.toPath())).getAsJsonObject();

		// Load server settings

		// Content server
		logger.debug("Configuring content server settings...");
		JsonObject contentSrvJson = configData.get("contentServer").getAsJsonObject();
		if (contentSrvConfig.server != null) {
			logger.debug("Loading listening settings...");
			contentSrvConfig.listenAddress = contentSrvJson.get("listenAddress").getAsString();
			contentSrvConfig.listenPort = contentSrvJson.get("listenPort").getAsInt();
		}
		logger.debug("Loading IO settings...");
		contentSrvConfig.contentRequestListenPath = contentSrvJson.get("contentRequestListenPath").getAsString();
		contentSrvConfig.contentDataPath = contentSrvJson.get("contentDataPath").getAsString();
		contentSrvConfig.allowIndexingAssets = contentSrvJson.get("allowIndexingAssets").getAsBoolean();
		if (contentSrvConfig.server != null) {
			logger.debug("Loading encryption settings...");
			contentSrvConfig.https = contentSrvJson.get("https").getAsBoolean();
			contentSrvConfig.tlsKeystore = contentSrvJson.get("tlsKeystore").getAsString();
			contentSrvConfig.tlsKeystorePassword = contentSrvJson.get("tlsKeystorePassword").getAsString();
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
		EventBus.getInstance().dispatchEvent(new ContentServerConfigLoadedEvent(contentSrvConfig));

		// Setup servers
		logger.info("Setting up servers...");

		// Content server
		logger.info("Setting up the content server...");
		EdgeContentServer contSrv = new EdgeContentServer(contentSrvConfig);
		contSrv.setupServer();

		// Start servers
		contSrv.startServer();

		// Call post-init
		ModuleManager.runModulePostInit();

		// Wait for exit
		logger.info("EDGE servers are running!");
		contSrv.waitForExit();
	}

}
