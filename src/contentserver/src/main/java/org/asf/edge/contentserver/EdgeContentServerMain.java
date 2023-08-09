package org.asf.edge.contentserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.CommonInit;
import org.asf.edge.modules.ModuleManager;
import org.asf.edge.modules.eventbus.EventBus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.asf.edge.contentserver.config.ContentServerConfig;
import org.asf.edge.contentserver.events.config.*;

public class EdgeContentServerMain {
	public static void main(String[] args) throws IOException {
		// Splash
		EdgeContentServer.printSplash();

		// Common init
		CommonInit.initAll();

		// Logger
		Logger logger = LogManager.getLogger("CONTENTSERVER");
		logger.info("Preparing to start...");

		// Load modules
		ModuleManager.init();

		// Create config object
		ContentServerConfig config = new ContentServerConfig();
		EventBus.getInstance().dispatchEvent(new ContentServerConfigPresetupEvent(config));

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
						+ "    \"listenPort\": 5319,\n" // port to listen on
						+ "    \"contentRequestListenPath\": \"/\",\n" // URI to listen on
						+ "    \"contentDataPath\": \"./asset-data\",\n" // Content data path
						+ "    \"allowIndexingAssets\": true,\n" // Defines if indexing is enabled
						+ "\n" //
						+ "    \"serverTestEndpoint\": null,\n" // test endpoint
						+ "    \"fallbackAssetServerEndpoint\": null,\n" // proxy endpoint
						+ "    \"fallbackAssetServerManifestModifications\": {},\n" // proxy modifications
						+ "\n" //
						+ "    \"storeFallbackAssetDownloads\": false," // downloading fallback to disk
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
						+ "    \"contentRequestListenPath\": \"/\",\n" // URI to listen on
						+ "    \"contentDataPath\": \"./asset-data\",\n" // Content data path
						+ "    \"allowIndexingAssets\": true,\n" // Defines if indexing is enabled
						+ "\n" //
						+ "    \"serverTestEndpoint\": null,\n" // test endpoint
						+ "    \"assetProxyServerEndpoint\": null,\n" // proxy endpoint
						+ "    \"assetProxyManifestModifications\": {},\n" // proxy modifications
						+ "\n" //
						+ "    \"storeFallbackAssetDownloads\": false," // downloading fallback to disk
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
		config.contentRequestListenPath = configData.get("contentRequestListenPath").getAsString();
		config.contentDataPath = configData.get("contentDataPath").getAsString();
		config.allowIndexingAssets = configData.get("allowIndexingAssets").getAsBoolean();
		if (configData.has("fallbackAssetServerEndpoint")
				&& !configData.get("fallbackAssetServerEndpoint").isJsonNull())
			config.fallbackAssetServerEndpoint = configData.get("fallbackAssetServerEndpoint").getAsString();
		if (configData.has("fallbackAssetServerManifestModifications")
				&& !configData.get("fallbackAssetServerManifestModifications").isJsonNull())
			config.fallbackAssetServerManifestModifications = configData.get("fallbackAssetServerManifestModifications")
					.getAsJsonObject();
		if (configData.has("storeFallbackAssetDownloads")
				&& !configData.get("storeFallbackAssetDownloads").isJsonNull())
			config.storeFallbackAssetDownloads = configData.get("storeFallbackAssetDownloads").getAsBoolean();
		if (configData.has("serverTestEndpoint") && !configData.get("serverTestEndpoint").isJsonNull())
			config.serverTestEndpoint = configData.get("serverTestEndpoint").getAsString();
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
		EventBus.getInstance().dispatchEvent(new ContentServerConfigLoadedEvent(config));

		// Setup server
		EdgeContentServer server = new EdgeContentServer(config);
		server.setupServer();

		// Start server
		server.startServer();

		// Call post-init
		ModuleManager.runModulePostInit();

		// Wait for exit
		logger.info("Server is running!");
		server.waitForExit();
		if (CommonInit.restartPending)
			System.exit(237);
		else
			System.exit(0);
	}

}
