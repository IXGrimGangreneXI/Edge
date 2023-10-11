package org.asf.edge.common;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.commands.CommandContext;
import org.asf.edge.common.experiments.EdgeExperimentManager;
import org.asf.edge.common.permissions.PermissionContext;
import org.asf.edge.common.permissions.PermissionLevel;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.util.LogWindow;

import com.google.gson.JsonParser;

/**
 * 
 * Edge common server environment
 * 
 * @author Sky Swimmer
 *
 */
public class EdgeServerEnvironment {
	private static final String EDGE_VERSION = "a1.6";

	private static boolean logInited;
	private static boolean serverIDInited;
	private static boolean experimentsInited;
	private static boolean debugMode;

	private static ArrayList<String> serverTypes = new ArrayList<String>();

	private static String serverID;
	public static boolean restartPending;

	/**
	 * Adds server types
	 * 
	 * @param type Server type name to add, eg. <code>gameplayapi</code>
	 */
	public static void addServerType(String type) {
		if (!hasServerType(type))
			serverTypes.add(type.toLowerCase());
	}

	/**
	 * Checks if a server type is present in the environment
	 * 
	 * @param type Server type name, eg. <code>gameplayapi</code>
	 * @return True if present, false otherwise
	 */
	public static boolean hasServerType(String type) {
		return serverTypes.contains(type.toLowerCase());
	}

	/**
	 * Retrieves all server types present in the environment
	 * 
	 * @return Array of server type strings
	 */
	public static String[] getServerTypes() {
		return serverTypes.toArray(t -> new String[t]);
	}

	/**
	 * Retrieves the Edge version
	 * 
	 * @return Edge version string
	 */
	public static String getEdgeVersion() {
		return EDGE_VERSION;
	}

	/**
	 * Calls all init methods
	 */
	public static void initAll() {
		initLogging();
		initExperiments();
		initServerID();
	}

	/**
	 * Inits the experiment manager
	 */
	public static void initExperiments() {
		if (experimentsInited)
			return;
		experimentsInited = true;
		EdgeExperimentManager.bindManager();
	}

	/**
	 * Initializes the server ID system
	 */
	public static void initServerID() {
		if (serverIDInited)
			return;
		serverIDInited = true;

		// Setup ID
		File idFile = new File("scalability.json");
		try {
			if (!idFile.exists()) {
				serverID = UUID.randomUUID().toString();
				Files.writeString(idFile.toPath(), "{\n"
						//
						+ "    \"__COMMENT1__\": \"This file controls scalability settings, specifically server identification.\",\n"
						//
						+ "    \"__COMMENT2__\": \"The UUID below is used to identify this specific server set, it MUST be shared with a common/gameplay/smartfox server to make sure that multi-server functionality does not degrade.\",\n"
						//
						+ "    \"__COMMENT3__\": \"If the ID below does not have a common/gameplay/smartfox counterpart server active, memory leaks can occur as the load balancer will fail to determine which server the user is active on.\",\n"
						//
						+ "    \"__COMMENT4__\": \"It is imperative for multi-server configurations to have a common, gameplay and smartfox server with the same server set ID.\",\n"
						//
						+ "    \"serverSetID\": \"" + serverID + "\"\n"
						//
						+ "}\n");
			}
			serverID = JsonParser.parseString(Files.readString(idFile.toPath())).getAsJsonObject().get("serverSetID")
					.getAsString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Checks if the servers are in debug (IDE) mode
	 * 
	 * @return True if in debug mode, false otherwise
	 */
	public static boolean isInDebugMode() {
		return debugMode;
	}

	/**
	 * Initializes logging
	 */
	public static void initLogging() {
		if (logInited)
			return;
		logInited = true;

		// Setup logging
		if (System.getProperty("debugMode") != null) {
			System.setProperty("log4j2.configurationFile",
					EdgeServerEnvironment.class.getResource("/log4j2-ide.xml").toString());
			debugMode = true;
		} else {
			System.setProperty("log4j2.configurationFile",
					EdgeServerEnvironment.class.getResource("/log4j2.xml").toString());
		}

		// Open graphical
		if (System.getProperty("openGuiLog") != null && !System.getProperty("openGuiLog").equalsIgnoreCase("false")) {
			if (!GraphicsEnvironment.isHeadless()) {
				LogWindow.WindowAppender.showWindow();
				Runtime.getRuntime().addShutdownHook(new Thread(() -> LogWindow.WindowAppender.closeWindow()));
			}
		}

		// Load logger
		logger = LogManager.getLogger("EDGE");
	}

	/**
	 * Retrieves the server ID
	 * 
	 * @return Server ID string
	 */
	public static String getServerID() {
		return serverID;
	}

	private static CommandContext cmdCtx;
	private static String accountID;
	private static Logger logger;

	/**
	 * Executes console commands
	 * 
	 * @param command Command to execute
	 */
	public static void executeConsoleCommand(String command) {
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

	private static ArrayList<String> parseCommand(String args) {
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

}
