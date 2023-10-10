package org.asf.edge.gameplayapi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.gameplayapi.http.*;
import org.asf.edge.gameplayapi.http.handlers.gameplayapi.*;
import org.asf.edge.gameplayapi.http.handlers.itemstore.*;
import org.asf.edge.gameplayapi.services.quests.QuestManager;
import org.asf.edge.gameplayapi.services.quests.impl.QuestManagerImpl;
import org.asf.edge.gameplayapi.services.rooms.PlayerRoomManager;
import org.asf.edge.gameplayapi.services.rooms.impl.PlayerRoomManagerImpl;
import org.asf.edge.gameplayapi.util.InventoryUtils;
import org.asf.edge.modules.eventbus.EventBus;

import com.google.gson.JsonPrimitive;

import org.asf.edge.gameplayapi.http.handlers.achievements.*;
import org.asf.edge.common.EdgeServerEnvironment;
import org.asf.edge.common.IBaseServer;
import org.asf.edge.common.permissions.PermissionContext;
import org.asf.edge.common.permissions.PermissionLevel;
import org.asf.edge.common.services.ServiceImplementationPriorityLevels;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.achievements.AchievementManager;
import org.asf.edge.common.services.achievements.impl.AchievementManagerImpl;
import org.asf.edge.common.services.commondata.CommonDataContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.services.config.ConfigProviderService;
import org.asf.edge.common.services.config.impl.ConfigProviderServiceImpl;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.services.items.impl.ItemManagerImpl;
import org.asf.edge.common.services.leaderboard.LeaderboardManager;
import org.asf.edge.common.services.leaderboard.impl.LeaderboardManagerImpl;
import org.asf.edge.common.services.messages.WsMessageService;
import org.asf.edge.common.services.messages.impl.WsMessageServiceImpl;
import org.asf.edge.common.services.minigamedata.MinigameDataManager;
import org.asf.edge.common.services.minigamedata.impl.MinigameDataManagerImpl;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.common.util.LogWindow;
import org.asf.edge.gameplayapi.http.handlers.edgespecific.EdgeApiService;
import org.asf.edge.gameplayapi.events.server.GameplayApiServerSetupEvent;
import org.asf.edge.gameplayapi.events.server.GameplayApiServerStartupEvent;
import org.asf.edge.gameplayapi.commands.CommandContext;
import org.asf.edge.gameplayapi.config.GameplayApiServerConfig;

/**
 * 
 * EDGE Gameplay API server (content api)
 * 
 * @author Sky Swimmer
 *
 */
public class EdgeGameplayApiServer implements IBaseServer {
	public static final String GAMEPLAY_API_VERSION = "a1.5";

	private Logger logger;
	private GameplayApiServerConfig config;

	private ConnectiveHttpServer server;

	@Override
	public String getVersion() {
		return GAMEPLAY_API_VERSION;
	}

	static void printSplash() {
		System.out.println("-------------------------------------------------------------");
		System.out.println("                                                             ");
		System.out.println("    EDGE - Fan-made server software for School of Dragons    ");
		System.out.println("              Gameplay API Server Version: a1.5              ");
		System.out.println("                                                             ");
		System.out.println("-------------------------------------------------------------");
		System.out.println("");
		System.out.println("This server implements the following endpoints:");
		System.out.println(" - contentserver.api.jumpstart.com");
		System.out.println(" - itemstoremission.api.jumpstart.com");
		System.out.println(" - achievement.api.jumpstart.com");
		System.out.println("");
	}

	public EdgeGameplayApiServer(GameplayApiServerConfig config) {
		this.config = config;
		logger = LogManager.getLogger("GAMEPLAYAPI");
	}

	/**
	 * Retrieves the HTTP server instance
	 * 
	 * @return ConnectiveHttpServer instance
	 */
	public ConnectiveHttpServer getServer() {
		return server;
	}

	/**
	 * Retrieves the logger of the gameplay api server
	 * 
	 * @return Logger instance
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Retrieves the gameplay api server configuration
	 * 
	 * @return GameplayApiServerConfig instance
	 */
	public GameplayApiServerConfig getConfiguration() {
		return config;
	}

	/**
	 * Called to set up the server
	 * 
	 * @throws IOException If setup fails
	 */
	public void setupServer() throws IOException {
		// Set up the server
		if (config.server == null) {
			// Create adapter
			if (config.https) {
				HashMap<String, String> props = new HashMap<String, String>();
				props.put("address", config.listenAddress);
				props.put("port", Integer.toString(config.listenPort));
				props.put("keystore", config.tlsKeystore);
				props.put("keystore-password", config.tlsKeystorePassword);
				config.server = ConnectiveHttpServer.createNetworked("HTTPS/1.1", props);
				logger.info("Edge gameplay api HTTPS server created with listen address " + config.listenAddress
						+ " and port " + config.listenPort);
			} else {
				HashMap<String, String> props = new HashMap<String, String>();
				props.put("address", config.listenAddress);
				props.put("port", Integer.toString(config.listenPort));
				config.server = ConnectiveHttpServer.createNetworked("HTTP/1.1", props);
				logger.info("Edge gameplay api HTTP server created with listen address " + config.listenAddress
						+ " and port " + config.listenPort);
			}
		}

		// Assign server
		server = config.server;

		// Config service
		logger.info("Setting up config service");
		ServiceManager.registerServiceImplementation(ConfigProviderService.class, new ConfigProviderServiceImpl());
		ServiceManager.selectServiceImplementation(ConfigProviderService.class);

		// Register content source
		logger.debug("Adding case-insensitive content source...");
		server.setContentSource(new CaseInsensitiveContentSource());

		// Call event
		logger.debug("Dispatching event...");
		EventBus.getInstance().dispatchEvent(new GameplayApiServerSetupEvent(config, this));

		// Register handlers
		logger.debug("Configuring server request handlers...");
		server.registerProcessor(new ContentWebServiceV1Processor(this));
		server.registerProcessor(new ContentWebServiceV2Processor(this));
		server.registerProcessor(new ContentWebServiceV3Processor(this));
		server.registerProcessor(new ContentWebServiceV4Processor(this));
		server.registerProcessor(new RatingWebServiceV1Processor(this));
		server.registerProcessor(new RatingWebServiceV2Processor(this));
		server.registerProcessor(new ChallengeWebServiceProcessor(this));
		server.registerProcessor(new InviteFriendWebServiceProcessor(this));
		server.registerProcessor(new MobileStoreWebServiceProcessor(this));
		server.registerProcessor(new PaymentWebServiceV1Processor(this));
		server.registerProcessor(new PaymentWebServiceV2Processor(this));
		server.registerProcessor(new ScoreWebServiceProcessor(this));
		server.registerProcessor(new ItemStoreWebServiceProcessor(this));
		server.registerProcessor(new MissionWebServiceProcessor(this));
		server.registerProcessor(new AchievementWebServiceV1Processor(this));
		server.registerProcessor(new AchievementWebServiceV2Processor(this));
		server.registerProcessor(new EdgeApiService(this));

		// Bind command handler
		logger.info("Binding command handler to GUI terminal...");
		LogWindow.commandCallback = t -> executeConsoleCommand(t);

		// Select item manager
		logger.info("Setting up item manager...");
		ServiceManager.registerServiceImplementation(ItemManager.class, new ItemManagerImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(ItemManager.class);
		InventoryUtils.init();

		// Select quest manager
		logger.info("Setting up quest manager...");
		ServiceManager.registerServiceImplementation(QuestManager.class, new QuestManagerImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(QuestManager.class);

		// Select room manager
		logger.info("Setting up player room manager...");
		ServiceManager.registerServiceImplementation(PlayerRoomManager.class, new PlayerRoomManagerImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(PlayerRoomManager.class);

		// Select achievement manager
		logger.info("Setting up achievement manager...");
		ServiceManager.registerServiceImplementation(AchievementManager.class, new AchievementManagerImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(AchievementManager.class);

		// Select message service
		logger.info("Setting up message service...");
		ServiceManager.registerServiceImplementation(WsMessageService.class, new WsMessageServiceImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(WsMessageService.class);

		// Start leaderboard service
		logger.info("Setting up leaderboard manager...");
		ServiceManager.registerServiceImplementation(LeaderboardManager.class, new LeaderboardManagerImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(LeaderboardManager.class);
		LeaderboardManager.getInstance().registerLeaderboard("UDT");

		// Start game data service
		logger.info("Setting up minigame data manager...");
		ServiceManager.registerServiceImplementation(MinigameDataManager.class, new MinigameDataManagerImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(MinigameDataManager.class);

		// Load filter
		logger.info("Loading text filter...");
		TextFilterService.getInstance();

		// Server watchdog
		logger.info("Starting shutdown and restart watchdog...");
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
						if (isRunning()) {
							stopServer();
							break;
						}
					}
					long restart = cont.getEntry("restart").getAsLong();
					if (restart > lastRestartTime) {
						// Trigger restart
						if (isRunning()) {
							EdgeServerEnvironment.restartPending = true;
							stopServer();
							break;
						}
					}
				} catch (IOException e) {
				}
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
				}
			}
		});
	}

	private CommandContext cmdCtx;
	private String accountID;

	private void executeConsoleCommand(String command) {
		if (command.isEmpty())
			return;

		// Check
		AccountObject acc = null;
		if (accountID != null) {
			acc = AccountManager.getInstance().getAccount(accountID);
		}
		if (acc == null) {
			accountID = null;
			cmdCtx = null;
		}

		// Parse
		List<String> args = parseCommand(command);
		if (args.size() == 0)
			return;
		String cmd = args.remove(0);

		// Check command
		switch (cmd.toLowerCase()) {

		// Make-admin
		case "makeadmin": {
			if (args.size() < 1) {
				logger.error("Usage: makeadmin \"<username>\"");
				return;
			}

			// Find by name
			String id = AccountManager.getInstance().getAccountID(args.get(0));
			if (id == null) {
				logger.error("Username not recognized: " + args.get(0));
				return;
			}

			// Load
			AccountObject acc2 = AccountManager.getInstance().getAccount(id);
			if (acc2 == null) {
				logger.error("Username not recognized: " + args.get(0));
				return;
			}

			// Get permission context
			PermissionContext.getFor(acc2).setPermissionLevel(PermissionLevel.ADMINISTRATOR);
			logger.info("Made " + acc2.getUsername() + " admin.");
			if (acc.getAccountID().equals(acc2.getAccountID())) {
				acc = acc2;
				cmdCtx = CommandContext.getFor(acc);
			}
			break;
		}

		// Make-moderator
		case "makemoderator": {
			if (args.size() < 1) {
				logger.error("Usage: makemoderator \"<username>\"");
				return;
			}

			// Find by name
			String id = AccountManager.getInstance().getAccountID(args.get(0));
			if (id == null) {
				logger.error("Username not recognized: " + args.get(0));
				return;
			}

			// Load
			AccountObject acc2 = AccountManager.getInstance().getAccount(id);
			if (acc2 == null) {
				logger.error("Username not recognized: " + args.get(0));
				return;
			}

			// Get permission context
			PermissionContext.getFor(acc2).setPermissionLevel(PermissionLevel.MODERATOR);
			logger.info("Made " + acc2.getUsername() + " moderator.");
			if (acc.getAccountID().equals(acc2.getAccountID())) {
				acc = acc2;
				cmdCtx = CommandContext.getFor(acc);
			}
			break;
		}

		// Make-trial-moderator
		case "maketrialmod": {
			if (args.size() < 1) {
				logger.error("Usage: maketrialmod \"<username>\"");
				return;
			}

			// Find by name
			String id = AccountManager.getInstance().getAccountID(args.get(0));
			if (id == null) {
				logger.error("Username not recognized: " + args.get(0));
				return;
			}

			// Load
			AccountObject acc2 = AccountManager.getInstance().getAccount(id);
			if (acc2 == null) {
				logger.error("Username not recognized: " + args.get(0));
				return;
			}

			// Get permission context
			PermissionContext.getFor(acc2).setPermissionLevel(PermissionLevel.TRIAL_MODERATOR);
			logger.info("Made " + acc2.getUsername() + " trial moderator.");
			if (acc.getAccountID().equals(acc2.getAccountID())) {
				acc = acc2;
				cmdCtx = CommandContext.getFor(acc);
			}
			break;
		}

		// Make-developer
		case "makedeveloper": {
			if (args.size() < 1) {
				logger.error("Usage: makedeveloper \"<username>\"");
				return;
			}

			// Find by name
			String id = AccountManager.getInstance().getAccountID(args.get(0));
			if (id == null) {
				logger.error("Username not recognized: " + args.get(0));
				return;
			}

			// Load
			AccountObject acc2 = AccountManager.getInstance().getAccount(id);
			if (acc2 == null) {
				logger.error("Username not recognized: " + args.get(0));
				return;
			}

			// Get permission context
			PermissionContext.getFor(acc2).setPermissionLevel(PermissionLevel.DEVELOPER);
			logger.info("Made " + acc2.getUsername() + " developer.");
			if (acc.getAccountID().equals(acc2.getAccountID())) {
				acc = acc2;
				cmdCtx = CommandContext.getFor(acc);
			}
			break;
		}

		// Make-operator
		case "makeoperator": {
			if (args.size() < 1) {
				logger.error("Usage: makeoperator \"<username>\"");
				return;
			}

			// Find by name
			String id = AccountManager.getInstance().getAccountID(args.get(0));
			if (id == null) {
				logger.error("Username not recognized: " + args.get(0));
				return;
			}

			// Load
			AccountObject acc2 = AccountManager.getInstance().getAccount(id);
			if (acc2 == null) {
				logger.error("Username not recognized: " + args.get(0));
				return;
			}

			// Get permission context
			PermissionContext.getFor(acc2).setPermissionLevel(PermissionLevel.OPERATOR);
			logger.info("Made " + acc2.getUsername() + " operator.");
			if (acc.getAccountID().equals(acc2.getAccountID())) {
				acc = acc2;
				cmdCtx = CommandContext.getFor(acc);
			}
			break;
		}

		// Stripperms
		case "stripperms": {
			if (args.size() < 1) {
				logger.error("Usage: stripperms \"<username>\"");
				return;
			}

			// Find by name
			String id = AccountManager.getInstance().getAccountID(args.get(0));
			if (id == null) {
				logger.error("Username not recognized: " + args.get(0));
				return;
			}

			// Load
			AccountObject acc2 = AccountManager.getInstance().getAccount(id);
			if (acc2 == null) {
				logger.error("Username not recognized: " + args.get(0));
				return;
			}

			// Get permission context
			PermissionContext.getFor(acc2)
					.setPermissionLevel(acc2.isGuestAccount() ? PermissionLevel.GUEST : PermissionLevel.PLAYER);
			logger.info("Stripped permissions of " + acc2.getUsername() + ".");
			if (acc.getAccountID().equals(acc2.getAccountID())) {
				acc = acc2;
				cmdCtx = CommandContext.getFor(acc);
			}
			break;
		}

		// Login-as
		case "login-as": {
			if (args.size() < 1) {
				logger.error("Usage: login-as \"<username>\"");
				return;
			}

			// Find by name
			String id = AccountManager.getInstance().getAccountID(args.get(0));
			if (id == null) {
				logger.error("Username not recognized: " + args.get(0));
				return;
			}

			// Load
			acc = AccountManager.getInstance().getAccount(id);
			if (acc == null) {
				logger.error("Username not recognized: " + args.get(0));
				return;
			}

			// Assign
			accountID = id;
			cmdCtx = CommandContext.getFor(acc);
			logger.info("Logged into the console as " + acc.getUsername());
			logger.info("You have access to " + cmdCtx.getPermissions().getPermissionLevel().toString().toLowerCase()
					+ " commands.");

			break;
		}

		// Other commands
		default: {
			// Check
			if (cmdCtx == null) {
				logger.error("Please use 'login-as \"<username>\"' before using commands.");
				return;
			}

			// Run
			cmdCtx.runCommand(command, t -> {
				logger.info(t);
			});
		}

		}
	}

	private ArrayList<String> parseCommand(String args) {
		ArrayList<String> args3 = new ArrayList<String>();
		char[] argarray = args.toCharArray();
		boolean ignorespaces = false;
		boolean hasData = false;
		String last = "";
		int i = 0;
		for (char c : args.toCharArray()) {
			if (c == '"' && (i == 0 || argarray[i - 1] != '\\')) {
				if (ignorespaces)
					ignorespaces = false;
				else {
					hasData = true;
					ignorespaces = true;
				}
			} else if (c == ' ' && !ignorespaces && (i == 0 || argarray[i - 1] != '\\')) {
				if (hasData)
					args3.add(last);
				hasData = false;
				last = "";
			} else if (c != '\\' || (i + 1 < argarray.length && argarray[i + 1] != '"'
					&& (argarray[i + 1] != ' ' || ignorespaces))) {
				hasData = true;
				last += c;
			}

			i++;
		}
		if (!last.isEmpty())
			args3.add(last);
		return args3;
	}

	private long lastRestartTime;
	private long lastShutdownTime;

	/**
	 * Starts the server
	 */
	public void startServer() throws IOException {
		if (server == null)
			throw new IllegalArgumentException("Server has not been set up");
		if (server.isRunning())
			throw new IllegalArgumentException("Server is already running");

		// Start server
		logger.info("Starting the Gameplay API server...");
		server.start();

		// Call event
		EventBus.getInstance().dispatchEvent(new GameplayApiServerStartupEvent(config, this));

		// Log
		logger.info("Gameplay API server started successfully!");
	}

	/**
	 * Stops the server
	 */
	public void stopServer() {
		if (server == null)
			throw new IllegalArgumentException("Server has not been set up");
		if (!server.isRunning())
			throw new IllegalArgumentException("Server is not running");

		// Stop the server
		logger.info("Shutting down the Gameplay API server...");
		try {
			server.stop();
		} catch (IOException e) {
		}
		logger.info("Gameplay API server stopped successfully!");
	}

	/**
	 * Stops the server forcefully
	 */
	public void killServer() {
		if (server == null)
			throw new IllegalArgumentException("Server has not been set up");
		if (!server.isRunning())
			throw new IllegalArgumentException("Server is not running");

		// Kill the server
		logger.info("Forcefully shutting down the Gameplay API server!");
		try {
			server.stopForced();
		} catch (IOException e) {
		}
		logger.info("Gameplay API server stopped successfully!");
	}

	/**
	 * Checks if the server is running
	 * 
	 * @return True if running, false otherwise
	 */
	public boolean isRunning() {
		if (server == null)
			return false;
		return server.isRunning();
	}

	/**
	 * Waits for the server to quit
	 */
	public void waitForExit() {
		// Wait for server to stop
		while (isRunning())
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				break;
			}
	}

}