package org.asf.edge.gameplayapi.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.CommonInit;
import org.asf.edge.common.services.ServiceImplementationPriorityLevels;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.services.items.impl.ItemManagerImpl;
import org.asf.edge.gameplayapi.commands.CommandContext;
import org.asf.edge.gameplayapi.commands.IEdgeServerCommand;
import org.asf.edge.gameplayapi.config.GameplayApiServerConfig;
import org.asf.edge.gameplayapi.events.config.GameplayApiServerConfigLoadedEvent;
import org.asf.edge.gameplayapi.events.config.GameplayApiServerConfigPresetupEvent;
import org.asf.edge.gameplayapi.permissions.PermissionLevel;
import org.asf.edge.gameplayapi.services.quests.QuestManager;
import org.asf.edge.gameplayapi.services.quests.impl.QuestManagerImpl;
import org.asf.edge.modules.ModuleManager;
import org.asf.edge.modules.eventbus.EventBus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ServerCommandTerminal {

	private static boolean exit;

	public static void main(String[] args) throws IOException {
		@SuppressWarnings("resource")
		Scanner sc = new Scanner(System.in);

		// Common init
		CommonInit.initAll();

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
		logger.info("Setting up the terminal...");
		logger.debug("Loading account manager implementations...");
		AccountManager.initAccountManagerServices(ServiceImplementationPriorityLevels.NORMAL,
				ServiceImplementationPriorityLevels.DEFAULT);
		logger.debug("Selecting account manager implementation...");
		ServiceManager.selectServiceImplementation(AccountManager.class);
		logger.debug("Loading account manager...");
		AccountManager.getInstance().loadManager();

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
					Consumer<String> outputWriteLineCallback, String fullCommand) {
				exit = true;
				return "Exiting terminal...";
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
