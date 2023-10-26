package org.asf.edge.globalserver;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.EdgeServerEnvironment;
import org.asf.edge.common.SentinelUpdateManager;
import org.asf.edge.common.experiments.EdgeDefaultExperiments;
import org.asf.edge.common.experiments.ExperimentManager;
import org.asf.edge.common.CommonUpdater;
import org.asf.edge.common.services.ServiceImplementationPriorityLevels;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.services.config.ConfigProviderService;
import org.asf.edge.common.services.config.impl.ConfigProviderServiceImpl;
import org.asf.edge.contentserver.EdgeContentServer;
import org.asf.edge.contentserver.config.ContentServerConfig;
import org.asf.edge.contentserver.events.config.ContentServerConfigLoadedEvent;
import org.asf.edge.contentserver.events.config.ContentServerConfigPresetupEvent;
import org.asf.edge.gameplayapi.events.config.GameplayApiServerConfigLoadedEvent;
import org.asf.edge.gameplayapi.events.config.GameplayApiServerConfigPresetupEvent;
import org.asf.edge.mmoserver.EdgeMMOServer;
import org.asf.edge.mmoserver.config.MMOServerConfig;
import org.asf.edge.mmoserver.events.config.MMOServerConfigPresetupEvent;
import org.asf.edge.mmoserver.events.config.MMOServerConfigLoadedEvent;
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
	public static final String GLOBAL_SERVER_VERSION = "a1.6";

	public static void main(String[] args) throws IOException, URISyntaxException {
		// Set locale
		Locale.setDefault(Locale.ENGLISH);

		// Print splash
		System.out.println("-------------------------------------------------------------");
		System.out.println("                                                             ");
		System.out.println("    EDGE - Fan-made server software for School of Dragons    ");
		System.out.println("                   AIO Server Version a1.6                   ");
		System.out.println("                                                             ");
		System.out.println("-------------------------------------------------------------");
		System.out.println("");

		// Common init
		EdgeServerEnvironment.initAll();
		EdgeServerEnvironment.addServerType("globalserver");
		EdgeServerEnvironment.addServerType("contentserver");
		EdgeServerEnvironment.addServerType("commonapi");
		EdgeServerEnvironment.addServerType("gameplayapi");
		EdgeServerEnvironment.addServerType("mmoserver");

		// Logger
		Logger logger = LogManager.getLogger("AOISERVER");
		logger.info("EDGE Global (All in One) server is starting!");
		logger.info("Edge version: " + EdgeServerEnvironment.getEdgeVersion());
		logger.info("Content server version: " + EdgeContentServer.CONTENT_SERVER_VERSION);
		logger.info("Common API version: " + EdgeCommonApiServer.COMMON_API_VERSION);
		logger.info("Gameplay API server version: " + EdgeGameplayApiServer.GAMEPLAY_API_VERSION);
		logger.info("MMO server version: " + EdgeMMOServer.MMO_SERVER_VERSION);
		logger.info("Global server version: " + GLOBAL_SERVER_VERSION);
		logger.info("Preparing to start...");

		// Config service
		logger.info("Setting up config service...");
		ServiceManager.registerServiceImplementation(ConfigProviderService.class, new ConfigProviderServiceImpl());
		ServiceManager.selectServiceImplementation(ConfigProviderService.class);

		// Run updater if needed
		CommonUpdater.init("globalserver", "stable", EdgeGlobalServerMain.GLOBAL_SERVER_VERSION,
				new File(EdgeGlobalServerMain.class.getProtectionDomain().getCodeSource().getLocation().toURI()));

		// Start sentinel update manager
		if (System.getProperty("enableSentinelLauncherUpdateManager") != null
				&& !System.getProperty("enableSentinelLauncherUpdateManager").equalsIgnoreCase("false")) {
			// Load sentinel properties
			String listUrl = System.getProperty("sentinelLauncherEdgeSoftwareUpdateList");
			String currentVersion = System.getProperty("sentinelLauncherEdgeSoftwareVersion");
			if (listUrl != null && currentVersion != null) {
				SentinelUpdateManager.init(listUrl, currentVersion);
			}
		}

		// Load modules
		ModuleManager.init();

		// Save experiment manager config
		ExperimentManager.getInstance().saveConfig();

		// Create config objects
		ContentServerConfig contentSrvConfig = new ContentServerConfig();
		EventBus.getInstance().dispatchEvent(new ContentServerConfigPresetupEvent(contentSrvConfig));
		GameplayApiServerConfig gpApiConfig = new GameplayApiServerConfig();
		EventBus.getInstance().dispatchEvent(new GameplayApiServerConfigPresetupEvent(gpApiConfig));
		CommonApiServerConfig cApiConfig = new CommonApiServerConfig();
		EventBus.getInstance().dispatchEvent(new CommonApiServerConfigPresetupEvent(cApiConfig));
		MMOServerConfig mmoSrvConfig = new MMOServerConfig();
		EventBus.getInstance().dispatchEvent(new MMOServerConfigPresetupEvent(mmoSrvConfig));

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
					+ "        \"listenPort\": 16519,\n" // port to listen on
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
					+ "        \"listenPort\": 16520,\n" // port to listen on
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
					+ "        \"listenPort\": 16521,\n" // port to listen on
					+ "        \"apiRequestListenPath\": \"/\",\n" // URI to listen on
					+ "\n" //
					+ "        \"https\": false,\n" // use https?
					+ "        \"tlsKeystore\": null,\n" // keystore file
					+ "        \"tlsKeystorePassword\": null,\n" // keystore password
					+ "\n" //
					+ "        \"internalListenAddress\": \"127.0.0.1\",\n" // listen address
					+ "        \"internalListenPort\": 16524,\n" // port to listen on
					+ "\n" //
					+ "        \"httpsInternal\": false,\n" // use https?
					+ "        \"tlsKeystoreInternal\": null,\n" // keystore file
					+ "        \"tlsKeystorePasswordInternal\": null\n" // keystore password
					+ "    },\n" //
					+ "\n" //
					+ "    \"mmoServer\": {\n" //
					+ "        \"disabled\": false,\n" // defines if the server is disabled
					+ "        \"listenAddress\": \"0.0.0.0\",\n" // listen address
					+ "        \"listenPort\": 16523,\n" // port to listen on
					+ "\n" //
					+ "        \"discoveryAddress\": \"localhost\",\n" // discovery Address
					+ "        \"discoveryPort\": 16523,\n" // discovery port
					+ "        \"discoveryRootZone\": \"JumpStart\",\n" // MMO root zone
					+ "        \"isBackupServer\": false,\n" // is this a backup MMO server?
					+ "\n" //
					+ "        \"commonApiUplinkURL\": \"http://127.0.0.1:16524/\",\n" // uplink URL
					+ "\n" //
					+ "        \"roomUserLimit\": 30,\n" //
					+ "        \"roomUserLimits\": {\n" //
					+ "            \"HubSchoolDO\": 40" //
					+ "        }\n" //
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
		boolean contentDisabled = (contentSrvJson.has("disabled") && contentSrvJson.get("disabled").getAsBoolean())
				|| (System.getProperty("disableContentServer") != null
						&& !System.getProperty("disableContentServer").equalsIgnoreCase("false"));
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
		boolean capiDisabled = cApiJson.has("disabled") && cApiJson.get("disabled").getAsBoolean()
				|| (System.getProperty("disableCommonApiServer") != null
						&& !System.getProperty("disableCommonApiServer").equalsIgnoreCase("false"));
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
		boolean gapiDisabled = gpApiJson.has("disabled") && gpApiJson.get("disabled").getAsBoolean()
				|| (System.getProperty("disableGameplayApiServer") != null
						&& !System.getProperty("disableGameplayApiServer").equalsIgnoreCase("false"));
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

		// MMO server
		logger.debug("Configuring MMO server settings...");
		JsonObject mmoSrvJson = configData.get("mmoServer").getAsJsonObject();
		boolean mmoSrvDisabled = mmoSrvJson.has("disabled") && mmoSrvJson.get("disabled").getAsBoolean()
				|| (System.getProperty("disableMMOServer") != null
						&& !System.getProperty("disableMMOServer").equalsIgnoreCase("false"));
		if (System.getProperty("disableMmoUnlessExperimentEnabled") != null
				&& !ExperimentManager.getInstance().isExperimentEnabled(EdgeDefaultExperiments.MMO_SERVER_SUPPORT))
			mmoSrvDisabled = true;
		if (!mmoSrvDisabled) {
			if (mmoSrvConfig.server == null) {
				logger.debug("Loading listening settings...");
				mmoSrvConfig.listenAddress = mmoSrvJson.get("listenAddress").getAsString();
				mmoSrvConfig.listenPort = mmoSrvJson.get("listenPort").getAsInt();
			}
			logger.debug("Loading discovery settings...");
			mmoSrvConfig.discoveryAddress = mmoSrvJson.get("discoveryAddress").getAsString();
			mmoSrvConfig.discoveryPort = mmoSrvJson.get("discoveryPort").getAsInt();
			mmoSrvConfig.commonApiUplinkURL = mmoSrvJson.get("commonApiUplinkURL").getAsString();
			if (mmoSrvJson.has("isBackupServer"))
				mmoSrvConfig.isBackupServer = mmoSrvJson.get("isBackupServer").getAsBoolean();
			if (mmoSrvJson.has("discoveryRootZone"))
				mmoSrvConfig.discoveryRootZone = mmoSrvJson.get("discoveryRootZone").getAsString();
			logger.debug("Loading MMO settings...");
			if (mmoSrvJson.has("roomUserLimit"))
				mmoSrvConfig.roomUserLimit = mmoSrvJson.get("roomUserLimit").getAsShort();
			if (mmoSrvJson.has("roomUserLimits")) {
				JsonObject limits = mmoSrvJson.get("roomUserLimits").getAsJsonObject();
				for (String key : limits.keySet())
					mmoSrvConfig.roomUserLimits.put(key, limits.get(key).getAsShort());
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
		if (!mmoSrvDisabled)
			EventBus.getInstance().dispatchEvent(new MMOServerConfigLoadedEvent(mmoSrvConfig));

		// Setup servers
		logger.info("Setting up servers...");
		logger.debug("Loading common data manager implementations...");
		CommonDataManager.initCommonDataManagerServices(ServiceImplementationPriorityLevels.DEFAULT,
				ServiceImplementationPriorityLevels.NORMAL, -5);
		logger.debug("Selecting common data manager implementation...");
		ServiceManager.selectServiceImplementation(CommonDataManager.class);
		logger.debug("Loading common data manager...");
		CommonDataManager.getInstance().loadManager();
		logger.debug("Loading account manager implementations...");
		AccountManager.initAccountManagerServices(ServiceImplementationPriorityLevels.DEFAULT,
				ServiceImplementationPriorityLevels.NORMAL, -5);
		logger.debug("Selecting account manager implementation...");
		ServiceManager.selectServiceImplementation(AccountManager.class);
		logger.debug("Loading account manager...");
		AccountManager.getInstance().loadManager();

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

		// MMO server
		EdgeMMOServer mmoSrv = null;
		if (!mmoSrvDisabled) {
			logger.info("Setting up the MMO server...");
			mmoSrv = new EdgeMMOServer(mmoSrvConfig);
			mmoSrv.setupServer();
		}

		// Start servers
		if (!contentDisabled)
			contSrv.startServer();
		if (!capiDisabled)
			cApiSrv.startServer();
		if (!gapiDisabled)
			gpApiSrv.startServer();
		if (!mmoSrvDisabled)
			mmoSrv.startServer();

		// Call post-init
		ModuleManager.runModulePostInit();

		// Wait for exit
		logger.info("EDGE servers are running!");
		if (!capiDisabled)
			cApiSrv.waitForExit();
		if (!gapiDisabled)
			gpApiSrv.waitForExit();
		if (!mmoSrvDisabled)
			mmoSrv.waitForExit();
		if (EdgeServerEnvironment.restartPending)
			System.exit(237);
		else
			System.exit(0);
	}

}
