package org.asf.edge.mmoserver;

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

import org.asf.edge.mmoserver.events.config.*;
import org.asf.edge.mmoserver.config.MMOServerConfig;

public class EdgeMMOServerMain {
	public static void main(String[] args) throws IOException, URISyntaxException {
		// Set locale
		Locale.setDefault(Locale.ENGLISH);

		// Splash
		EdgeMMOServer.printSplash();

		// Common init
		EdgeServerEnvironment.initAll();
		EdgeServerEnvironment.addServerType("mmoserver");

		// Logger
		Logger logger = LogManager.getLogger("MMOSERVER");
		logger.info("Preparing to start...");

		// Config service
		logger.info("Setting up config service...");
		ServiceManager.registerServiceImplementation(ConfigProviderService.class, new ConfigProviderServiceImpl());
		ServiceManager.selectServiceImplementation(ConfigProviderService.class);

		// Run updater if needed
		CommonUpdater.init("mmoserver", "stable", EdgeMMOServer.MMO_SERVER_VERSION,
				new File(EdgeMMOServerMain.class.getProtectionDomain().getCodeSource().getLocation().toURI()));

		// Load modules
		ModuleManager.init();

		// Save experiment manager config
		ExperimentManager.getInstance().saveConfig();

		// Create config object
		MMOServerConfig config = new MMOServerConfig();
		EventBus.getInstance().dispatchEvent(new MMOServerConfigPresetupEvent(config));

		// Load configuration
		logger.info("Loading server configuration...");
		File configFile = new File("server.json");
		if (!configFile.exists()) {
			logger.debug("Creating server configuration...");
			// Write config
			Files.writeString(configFile.toPath(), "{\n" //
					+ "\n" //
					+ "    \"listenAddress\": \"0.0.0.0\",\n" // listen address
					+ "    \"listenPort\": 5323,\n" // port to listen on
					+ "\n" //
					+ "    \"discoveryAddress\": \"localhost\",\n" // discovery Address
					+ "    \"discoveryPort\": 5323,\n" // discovery port
					+ "    \"discoveryRootZone\": \"JumpStart\",\n" // MMO root zone
					+ "    \"isBackupServer\": false,\n" // is this a backup MMO server?
					+ "\n" //
					+ "    \"commonApiUplinkURL\": \"http://127.0.0.1:5324/\",\n" // uplink URL
					+ "\n" //
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
		logger.debug("Loading listening settings...");
		config.listenAddress = configData.get("listenAddress").getAsString();
		config.listenPort = configData.get("listenPort").getAsInt();

		// Load discovery settings
		logger.debug("Loading discovery settings...");
		config.discoveryAddress = configData.get("discoveryAddress").getAsString();
		config.discoveryPort = configData.get("discoveryPort").getAsInt();
		config.commonApiUplinkURL = configData.get("commonApiUplinkURL").getAsString();
		if (configData.has("isBackupServer"))
			config.isBackupServer = configData.get("isBackupServer").getAsBoolean();
		if (configData.has("discoveryRootZone"))
			config.discoveryRootZone = configData.get("discoveryRootZone").getAsString();

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
		EventBus.getInstance().dispatchEvent(new MMOServerConfigLoadedEvent(config));

		// Prepare services
		logger.info("Setting up the server...");
		logger.debug("Loading account manager implementations...");
		AccountManager.initAccountManagerServices(ServiceImplementationPriorityLevels.NORMAL,
				ServiceImplementationPriorityLevels.DEFAULT, -5);
		logger.debug("Selecting account manager implementation...");
		ServiceManager.selectServiceImplementation(AccountManager.class);
		logger.debug("Loading account manager...");
		AccountManager.getInstance().loadManager();
		logger.debug("Loading common data manager implementations...");
		CommonDataManager.initCommonDataManagerServices(ServiceImplementationPriorityLevels.NORMAL,
				ServiceImplementationPriorityLevels.DEFAULT, -5);
		logger.debug("Selecting common data manager implementation...");
		ServiceManager.selectServiceImplementation(CommonDataManager.class);
		logger.debug("Loading common data manager...");
		CommonDataManager.getInstance().loadManager();

		// Setup server
		EdgeMMOServer server = new EdgeMMOServer(config);
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
