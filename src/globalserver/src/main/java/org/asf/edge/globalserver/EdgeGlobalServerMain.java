package org.asf.edge.globalserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.CommonInit;
import org.asf.edge.common.services.ServiceImplementationPriorityLevels;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.contentserver.EdgeContentServer;
import org.asf.edge.contentserver.config.ContentServerConfig;
import org.asf.edge.contentserver.events.config.ContentServerConfigLoadedEvent;
import org.asf.edge.contentserver.events.config.ContentServerConfigPresetupEvent;
import org.asf.edge.gameplayapi.events.config.GameplayApiServerConfigLoadedEvent;
import org.asf.edge.gameplayapi.events.config.GameplayApiServerConfigPresetupEvent;
import org.asf.edge.gameplayapi.EdgeGameplayApiServer;
import org.asf.edge.gameplayapi.config.GameplayApiServerConfig;
import org.asf.edge.commonapi.events.config.CommonApiServerConfigLoadedEvent;
import org.asf.edge.commonapi.events.config.CommonApiServerConfigPresetupEvent;
import org.asf.edge.commonapi.EdgeCommonApiServer;
import org.asf.edge.commonapi.config.CommonApiServerConfig;
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
		logger.info("Common API version: " + EdgeCommonApiServer.COMMON_API_VERSION);
		logger.info("Gameplay API server version: " + EdgeGameplayApiServer.GAMEPLAY_API_VERSION);
		logger.info("Global server version: " + GLOBAL_SERVER_VERSION);
		logger.info("Preparing to start...");

		// Load modules
		ModuleManager.init();

		// Create config objects
		ContentServerConfig contentSrvConfig = new ContentServerConfig();
		EventBus.getInstance().dispatchEvent(new ContentServerConfigPresetupEvent(contentSrvConfig));
		GameplayApiServerConfig gpApiConfig = new GameplayApiServerConfig();
		EventBus.getInstance().dispatchEvent(new GameplayApiServerConfigPresetupEvent(gpApiConfig));
		CommonApiServerConfig cApiConfig = new CommonApiServerConfig();
		EventBus.getInstance().dispatchEvent(new CommonApiServerConfigPresetupEvent(cApiConfig));

		// Load configuration
		logger.info("Loading server configuration...");
		File configFile = new File("server.json");
		if (!configFile.exists()) {
			// Write config
			logger.debug("Creating server configuration...");
			Files.writeString(configFile.toPath(), "{\n" //
					+ "\n" //
					+ "    \"contentServer\": {\n" //
					+ "        \"disabled\": false,\n" // defines if the server is disabled
					+ "        \"listenAddress\": \"0.0.0.0\",\n" // listen address
					+ "        \"listenPort\": 5319,\n" // port to listen on
					+ "        \"contentRequestListenPath\": \"/\",\n" // URI to listen on
					+ "        \"contentDataPath\": \"./data/contentserver/asset-data\",\n" // Content data path
					+ "        \"allowIndexingAssets\": true,\n" // Defines if indexing is enabled
					+ "\n" //
					+ "        \"serverTestEndpoint\": null,\n" // test endpoint
					+ "        \"fallbackAssetServerEndpoint\": null,\n" // proxy endpoint
					+ "        \"fallbackAssetServerManifestModifications\": {},\n" // proxy modifications
					+ "\n" //
					+ "        \"storeFallbackAssetDownloads\": false," // downloading fallback to disk
					+ "\n" //
					+ "        \"https\": false,\n" // use https?
					+ "        \"tlsKeystore\": null,\n" // keystore file
					+ "        \"tlsKeystorePassword\": null\n" // keystore password
					+ "    },\n" //
					+ "\n" //
					+ "    \"gameplayApiServer\": {\n" //
					+ "        \"disabled\": false,\n" // defines if the server is disabled
					+ "        \"listenAddress\": \"0.0.0.0\",\n" // listen address
					+ "        \"listenPort\": 5320,\n" // port to listen on
					+ "        \"apiRequestListenPath\": \"/\",\n" // URI to listen on
					+ "\n" //
					+ "        \"https\": false,\n" // use https?
					+ "        \"tlsKeystore\": null,\n" // keystore file
					+ "        \"tlsKeystorePassword\": null\n" // keystore password
					+ "    },\n" //
					+ "\n" //
					+ "    \"commonApiServer\": {\n" //
					+ "        \"disabled\": false,\n" // defines if the server is disabled
					+ "        \"listenAddress\": \"0.0.0.0\",\n" // listen address
					+ "        \"listenPort\": 5321,\n" // port to listen on
					+ "        \"apiRequestListenPath\": \"/\",\n" // URI to listen on
					+ "\n" //
					+ "        \"https\": false,\n" // use https?
					+ "        \"tlsKeystore\": null,\n" // keystore file
					+ "        \"tlsKeystorePassword\": null,\n" // keystore password
					+ "\n" //
					+ "        \"internalListenAddress\": \"127.0.0.1\",\n" // listen address
					+ "        \"internalListenPort\": 5324,\n" // port to listen on
					+ "\n" //
					+ "        \"httpsInternal\": false,\n" // use https?
					+ "        \"tlsKeystoreInternal\": null,\n" // keystore file
					+ "        \"tlsKeystorePasswordInternal\": null\n" // keystore password
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
		boolean contentDisabled = contentSrvJson.has("disabled") && contentSrvJson.get("disabled").getAsBoolean();
		if (!contentDisabled) {
			if (contentSrvConfig.server == null) {
				logger.debug("Loading listening settings...");
				contentSrvConfig.listenAddress = contentSrvJson.get("listenAddress").getAsString();
				contentSrvConfig.listenPort = contentSrvJson.get("listenPort").getAsInt();
			}
			logger.debug("Loading IO settings...");
			if (contentSrvJson.has("serverTestEndpoint") && !contentSrvJson.get("serverTestEndpoint").isJsonNull())
				contentSrvConfig.serverTestEndpoint = contentSrvJson.get("serverTestEndpoint").getAsString();
			if (contentSrvJson.has("fallbackAssetServerEndpoint")
					&& !contentSrvJson.get("fallbackAssetServerEndpoint").isJsonNull())
				contentSrvConfig.fallbackAssetServerEndpoint = contentSrvJson.get("fallbackAssetServerEndpoint")
						.getAsString();
			if (contentSrvJson.has("fallbackAssetServerManifestModifications")
					&& !contentSrvJson.get("fallbackAssetServerManifestModifications").isJsonNull())
				contentSrvConfig.fallbackAssetServerManifestModifications = contentSrvJson
						.get("fallbackAssetServerManifestModifications").getAsJsonObject();
			if (contentSrvJson.has("storeFallbackAssetDownloads"))
				contentSrvConfig.storeFallbackAssetDownloads = contentSrvJson.get("storeFallbackAssetDownloads")
						.getAsBoolean();
			contentSrvConfig.contentRequestListenPath = contentSrvJson.get("contentRequestListenPath").getAsString();
			contentSrvConfig.contentDataPath = contentSrvJson.get("contentDataPath").getAsString();
			contentSrvConfig.allowIndexingAssets = contentSrvJson.get("allowIndexingAssets").getAsBoolean();
			if (contentSrvConfig.server == null) {
				logger.debug("Loading encryption settings...");
				contentSrvConfig.https = contentSrvJson.get("https").getAsBoolean();
				if (contentSrvConfig.https) {
					contentSrvConfig.tlsKeystore = contentSrvJson.get("tlsKeystore").getAsString();
					contentSrvConfig.tlsKeystorePassword = contentSrvJson.get("tlsKeystorePassword").getAsString();
				}
			}
		}

		// Common api server
		logger.debug("Configuring Common API server settings...");
		JsonObject cApiJson = configData.get("commonApiServer").getAsJsonObject();
		boolean capiDisabled = cApiJson.has("disabled") && cApiJson.get("disabled").getAsBoolean();
		if (!capiDisabled) {
			if (cApiConfig.server == null) {
				logger.debug("Loading listening settings...");
				cApiConfig.listenAddress = cApiJson.get("listenAddress").getAsString();
				cApiConfig.listenPort = cApiJson.get("listenPort").getAsInt();
			}
			logger.debug("Loading IO settings...");
			cApiConfig.apiRequestListenPath = cApiJson.get("apiRequestListenPath").getAsString();
			if (cApiConfig.server == null) {
				logger.debug("Loading encryption settings...");
				cApiConfig.https = cApiJson.get("https").getAsBoolean();
				if (cApiConfig.https) {
					cApiConfig.tlsKeystore = cApiJson.get("tlsKeystore").getAsString();
					cApiConfig.tlsKeystorePassword = cApiJson.get("tlsKeystorePassword").getAsString();
				}
			}
			logger.debug("Loading internal server settings...");
			if (cApiConfig.internalServer == null) {
				logger.debug("Loading listening settings...");
				cApiConfig.internalListenAddress = cApiJson.get("internalListenAddress").getAsString();
				cApiConfig.internalListenPort = cApiJson.get("internalListenPort").getAsInt();
			}
			if (cApiConfig.internalServer == null) {
				logger.debug("Loading encryption settings...");
				cApiConfig.httpsInternal = cApiJson.get("httpsInternal").getAsBoolean();
				if (cApiConfig.httpsInternal) {
					cApiConfig.tlsKeystoreInternal = cApiJson.get("tlsKeystoreInternal").getAsString();
					cApiConfig.tlsKeystorePasswordInternal = cApiJson.get("tlsKeystorePasswordInternal").getAsString();
				}
			}
		}

		// Gameplay api server
		logger.debug("Configuring Gameplay API server settings...");
		JsonObject gpApiJson = configData.get("gameplayApiServer").getAsJsonObject();
		boolean gapiDisabled = gpApiJson.has("disabled") && gpApiJson.get("disabled").getAsBoolean();
		if (!gapiDisabled) {
			if (gpApiConfig.server == null) {
				logger.debug("Loading listening settings...");
				gpApiConfig.listenAddress = gpApiJson.get("listenAddress").getAsString();
				gpApiConfig.listenPort = gpApiJson.get("listenPort").getAsInt();
			}
			logger.debug("Loading IO settings...");
			gpApiConfig.apiRequestListenPath = gpApiJson.get("apiRequestListenPath").getAsString();
			if (gpApiConfig.server == null) {
				logger.debug("Loading encryption settings...");
				gpApiConfig.https = gpApiJson.get("https").getAsBoolean();
				if (gpApiConfig.https) {
					gpApiConfig.tlsKeystore = gpApiJson.get("tlsKeystore").getAsString();
					gpApiConfig.tlsKeystorePassword = gpApiJson.get("tlsKeystorePassword").getAsString();
				}
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
		if (!contentDisabled)
			EventBus.getInstance().dispatchEvent(new ContentServerConfigLoadedEvent(contentSrvConfig));
		if (!capiDisabled)
			EventBus.getInstance().dispatchEvent(new GameplayApiServerConfigLoadedEvent(gpApiConfig));
		if (!gapiDisabled)
			EventBus.getInstance().dispatchEvent(new CommonApiServerConfigLoadedEvent(cApiConfig));

		// Setup servers
		logger.info("Setting up servers...");
		logger.debug("Loading account manager implementations...");
		AccountManager.initAccountManagerServices(ServiceImplementationPriorityLevels.DEFAULT,
				ServiceImplementationPriorityLevels.NORMAL);
		logger.debug("Selecting account manager implementation...");
		ServiceManager.selectServiceImplementation(AccountManager.class);
		logger.debug("Loading account manager...");
		AccountManager.getInstance().loadManager();
		logger.debug("Loading common data manager implementations...");
		CommonDataManager.initCommonDataManagerServices(ServiceImplementationPriorityLevels.DEFAULT,
				ServiceImplementationPriorityLevels.NORMAL, -5);
		logger.debug("Selecting common data manager implementation...");
		ServiceManager.selectServiceImplementation(CommonDataManager.class);
		logger.debug("Loading common data manager...");
		CommonDataManager.getInstance().loadManager();

		// Content server
		EdgeContentServer contSrv = null;
		if (!contentDisabled) {
			logger.info("Setting up the content server...");
			contSrv = new EdgeContentServer(contentSrvConfig);
			contSrv.setupServer();
		}

		// Common server
		EdgeCommonApiServer cApiSrv = null;
		if (!capiDisabled) {
			logger.info("Setting up the Common API server...");
			cApiSrv = new EdgeCommonApiServer(cApiConfig);
			cApiSrv.setupServer();
		}

		// Gameplay server
		EdgeGameplayApiServer gpApiSrv = null;
		if (!gapiDisabled) {
			logger.info("Setting up the Gameplay API server...");
			gpApiSrv = new EdgeGameplayApiServer(gpApiConfig);
			gpApiSrv.setupServer();
		}

		// Start servers
		if (!contentDisabled)
			contSrv.startServer();
		if (!capiDisabled)
			cApiSrv.startServer();
		if (!gapiDisabled)
			gpApiSrv.startServer();

		// Call post-init
		ModuleManager.runModulePostInit();

		// Wait for exit
		logger.info("EDGE servers are running!");
		if (!capiDisabled)
			cApiSrv.waitForExit();
		if (!gapiDisabled)
			gpApiSrv.waitForExit();
		if (CommonInit.restartPending)
			System.exit(237);
		else
			System.exit(0);
	}

}
