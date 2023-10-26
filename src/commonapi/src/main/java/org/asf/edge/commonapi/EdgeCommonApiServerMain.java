package org.asf.edge.commonapi;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.EdgeServerEnvironment;
import org.asf.edge.common.experiments.ExperimentManager;
import org.asf.edge.common.CommonUpdater;
import org.asf.edge.common.services.ServiceImplementationPriorityLevels;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.services.config.ConfigProviderService;
import org.asf.edge.common.services.config.impl.ConfigProviderServiceImpl;
import org.asf.edge.modules.ModuleManager;
import org.asf.edge.modules.eventbus.EventBus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.asf.edge.commonapi.events.config.*;
import org.asf.edge.commonapi.config.CommonApiServerConfig;

public class EdgeCommonApiServerMain {
	public static void main(String[] args) throws IOException, URISyntaxException {
		// Set locale
		Locale.setDefault(Locale.ENGLISH);

		// Splash
		EdgeCommonApiServer.printSplash();

		// Common init
		EdgeServerEnvironment.initAll();
		EdgeServerEnvironment.addServerType("commonapi");

		// Logger
		Logger logger = LogManager.getLogger("COMMONAPI");
		logger.info("Preparing to start...");

		// Config service
		logger.info("Setting up config service...");
		ServiceManager.registerServiceImplementation(ConfigProviderService.class, new ConfigProviderServiceImpl());
		ServiceManager.selectServiceImplementation(ConfigProviderService.class);

		// Run updater if needed
		CommonUpdater.init("commonapi", "stable", EdgeCommonApiServer.COMMON_API_VERSION,
				new File(EdgeCommonApiServerMain.class.getProtectionDomain().getCodeSource().getLocation().toURI()));

		// Load modules
		ModuleManager.init();

		// Save experiment manager config
		ExperimentManager.getInstance().saveConfig();

		// Create config object
		CommonApiServerConfig config = new CommonApiServerConfig();
		EventBus.getInstance().dispatchEvent(new CommonApiServerConfigPresetupEvent(config));

		// Load configuration
		logger.info("Loading server configuration...");
		File configFile = new File("server.json");
		if (!configFile.exists()) {
			logger.debug("Creating server configuration...");
			// Write config
			Files.writeString(configFile.toPath(), "{\n" //
					+ "\n" //
					+ "    \"listenAddress\": \"0.0.0.0\",\n" // listen address
					+ "    \"listenPort\": 16521,\n" // port to listen on
					+ "    \"apiRequestListenPath\": \"/\",\n" // URI to listen on
					+ "\n" //
					+ "    \"https\": false,\n" // use https?
					+ "    \"tlsKeystore\": null,\n" // keystore file
					+ "    \"tlsKeystorePassword\": null,\n" // keystore password
					+ "\n" //
					+ "    \"internalListenAddress\": \"0.0.0.0\",\n" // listen address
					+ "    \"internalListenPort\": 16524,\n" // port to listen on
					+ "\n" //
					+ "    \"httpsInternal\": false,\n" // use https?
					+ "    \"tlsKeystoreInternal\": null,\n" // keystore file
					+ "    \"tlsKeystorePasswordInternal\": null,\n" // keystore password
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
		logger.debug("Loading internal server settings...");
		if (config.internalServer == null) {
			logger.debug("Loading listening settings...");
			config.internalListenAddress = configData.get("internalListenAddress").getAsString();
			config.internalListenPort = configData.get("internalListenPort").getAsInt();
		}
		if (config.internalServer == null) {
			logger.debug("Loading encryption settings...");
			config.httpsInternal = configData.get("httpsInternal").getAsBoolean();
			if (config.httpsInternal) {
				config.tlsKeystoreInternal = configData.get("tlsKeystoreInternal").getAsString();
				config.tlsKeystorePasswordInternal = configData.get("tlsKeystorePasswordInternal").getAsString();
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
		EventBus.getInstance().dispatchEvent(new CommonApiServerConfigLoadedEvent(config));

		// Prepare services
		logger.debug("Loading common data manager implementations...");
		CommonDataManager.initCommonDataManagerServices(ServiceImplementationPriorityLevels.DEFAULT,
				ServiceImplementationPriorityLevels.NORMAL, -5);
		logger.debug("Selecting common data manager implementation...");
		ServiceManager.selectServiceImplementation(CommonDataManager.class);
		logger.debug("Loading common data manager...");
		CommonDataManager.getInstance().loadManager();
		logger.info("Setting up the server...");
		logger.debug("Loading account manager implementations...");
		AccountManager.initAccountManagerServices(ServiceImplementationPriorityLevels.DEFAULT,
				ServiceImplementationPriorityLevels.NORMAL, -5);
		logger.debug("Selecting account manager implementation...");
		ServiceManager.selectServiceImplementation(AccountManager.class);
		logger.debug("Loading account manager...");
		AccountManager.getInstance().loadManager();

		// Setup server
		EdgeCommonApiServer server = new EdgeCommonApiServer(config);
		server.setupServer();

		// Start server
		server.startServer();

		// Call post-init
		ModuleManager.runModulePostInit();

		// Wait for exit
		logger.info("Server is running!");
		server.waitForExit();
		if (EdgeServerEnvironment.restartPending)
			System.exit(237);
		else
			System.exit(0);
	}

}
