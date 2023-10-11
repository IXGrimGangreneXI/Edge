package org.asf.edge.gameplayapi.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.common.EdgeServerEnvironment;
import org.asf.edge.common.commands.CommandContext;
import org.asf.edge.common.commands.IEdgeServerCommand;
import org.asf.edge.common.permissions.PermissionLevel;
import org.asf.edge.common.services.ServiceImplementationPriorityLevels;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.commondata.CommonDataContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.services.items.impl.ItemManagerImpl;
import org.asf.edge.gameplayapi.config.GameplayApiServerConfig;
import org.asf.edge.gameplayapi.events.config.GameplayApiServerConfigLoadedEvent;
import org.asf.edge.gameplayapi.events.config.GameplayApiServerConfigPresetupEvent;
import org.asf.edge.gameplayapi.services.quests.QuestManager;
import org.asf.edge.gameplayapi.services.quests.impl.QuestManagerImpl;
import org.asf.edge.modules.ModuleManager;
import org.asf.edge.modules.eventbus.EventBus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class ServerCommandTerminal {

	private static boolean exit;

	private static long lastRestartTime;
	private static long lastShutdownTime;

	public static void main(String[] args) throws IOException {
		@SuppressWarnings("resource")
		Scanner sc = new Scanner(System.in);

		// Common init
		EdgeServerEnvironment.initAll();

		// Logger
		Logger logger = LogManager.getLogger("TERMINAL");
		logger.info("Preparing to start terminal...");

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
		if (configData.has("gameplayApiServer"))
			configData = configData.get("gameplayApiServer").getAsJsonObject(); // Global server

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

		// Prepare services
		logger.info("Setting up the terminal...");
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

		// Select item manager
		logger.info("Setting up item manager...");
		ServiceManager.registerServiceImplementation(ItemManager.class, new ItemManagerImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(ItemManager.class);

		// Select quest manager
		logger.info("Setting up quest manager...");
		ServiceManager.registerServiceImplementation(QuestManager.class, new QuestManagerImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(QuestManager.class);

		// Login prompt
		System.out.print("Login username: ");
		String username = sc.nextLine();
		System.out.print("Login password: ");
		var con = System.console();
		char[] passwd;
		if (con == null) {
			passwd = sc.nextLine().toCharArray();
		} else
			passwd = con.readPassword();

		// Login
		System.out.println("Attempting to log into user: " + username + "...");
		String id = AccountManager.getInstance().getAccountID(username);
		if (id == null || !AccountManager.getInstance().verifyPassword(id, new String(passwd))) {
			System.err.println("Failed to log into your Edge account!");
			return;
		}

		// Get account
		AccountObject acc = AccountManager.getInstance().getAccount(id);
		if (acc == null) {
			System.err.println("Failed to log into your Edge account!");
			return;
		}
		CommandContext ctx = CommandContext.getFor(acc);
		ctx.registerCommand(new IEdgeServerCommand() {

			@Override
			public String id() {
				return "exit";
			}

			@Override
			public String syntax(CommandContext ctx) {
				return null;
			}

			@Override
			public String description(CommandContext ctx) {
				return "Closes the terminal";
			}

			@Override
			public PermissionLevel permLevel() {
				return PermissionLevel.GUEST;
			}

			@Override
			public String permNode() {
				return "any";
			}

			@Override
			public String run(String[] args, CommandContext ctx, Logger logger,
					Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) {
				exit = true;
				return "Exiting terminal...";
			}

		});
		CommonDataContainer cont = CommonDataManager.getInstance().getContainer("EDGECOMMON");
		try {
			if (!cont.entryExists("shutdown")) {
				lastShutdownTime = System.currentTimeMillis();
				cont.setEntry("shutdown", new JsonPrimitive(lastShutdownTime));
			} else
				lastShutdownTime = cont.getEntry("shutdown").getAsLong();
			if (!cont.entryExists("restart")) {
				lastRestartTime = System.currentTimeMillis();
				cont.setEntry("restart", new JsonPrimitive(lastRestartTime));
			} else
				lastRestartTime = cont.getEntry("restart").getAsLong();
		} catch (IOException e) {
		}
		AsyncTaskManager.runAsync(() -> {
			while (true) {
				// Check restart and shutdown
				try {
					long shutdown = cont.getEntry("shutdown").getAsLong();
					if (shutdown > lastShutdownTime) {
						// Trigger shutdown
						System.exit(0);
					}
					long restart = cont.getEntry("restart").getAsLong();
					if (restart > lastRestartTime) {
						// Trigger restart
						System.exit(237);
					}
				} catch (IOException e) {
				}
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
				}
			}
		});
		System.out.println();
		System.out.println("Edge interactive terminal, waiting for commands.");
		System.out.println("You have access to " + ctx.getPermissions().getPermissionLevel().toString().toLowerCase()
				+ " commands.");
		while (!exit) {
			System.out.print("> ");
			String cmd = sc.nextLine();
			ctx.runCommand(cmd, t -> System.out.println(t));
			System.out.println();
		}
	}

}
