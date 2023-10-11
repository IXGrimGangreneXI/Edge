package org.asf.edge.common.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.commands.defaultcommands.HelpCommand;
import org.asf.edge.common.commands.defaultcommands.account.ProfilesCommand;
import org.asf.edge.common.commands.defaultcommands.administration.ReloadCommand;
import org.asf.edge.common.commands.defaultcommands.administration.RestartCommand;
import org.asf.edge.common.commands.defaultcommands.administration.ShutdownCommand;
import org.asf.edge.common.commands.defaultcommands.debug.DebugCommands;
import org.asf.edge.common.events.commands.CommandSetupEvent;
import org.asf.edge.common.permissions.PermissionContext;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.modules.eventbus.EventBus;

/**
 * 
 * Command context system
 * 
 * @author Sky Swimmer
 *
 */
public class CommandContext {

	private Logger logger = LogManager.getLogger("COMMANDS");
	private HashMap<String, Object> commandMemory = new HashMap<String, Object>();
	private ArrayList<IEdgeServerCommand> commands = new ArrayList<IEdgeServerCommand>();
	private boolean logCommands;

	public void enableCommandLogging() {
		logCommands = true;
	}

	protected static CommandContextImplementationProvider implementationProvider = (acc) -> new CommandContext(acc);

	public static interface CommandContextImplementationProvider {
		public CommandContext getForAccount(AccountObject account);
	}

	private String account;
	private PermissionContext ctx;

	public CommandContext(AccountObject account) {
		this.account = account.getAccountID();

		// Load perms
		ctx = PermissionContext.getFor(account);

		// Register commands
		logger.debug("Registering commands...");
		registerCommand(new ProfilesCommand());
		registerCommand(new ReloadCommand());
		registerCommand(new ShutdownCommand());
		registerCommand(new RestartCommand());
		if (System.getProperty("debugMode") != null)
			registerCommand(new DebugCommands()); // Debug

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new CommandSetupEvent(this));
		registerCommand(new HelpCommand());
	}

	/**
	 * Retrieves the command memory map
	 * 
	 * @return Command memory map
	 */
	public Map<String, Object> getCommandMemory() {
		return commandMemory;
	}

	/**
	 * Retrieves all registered commands
	 * 
	 * @return Array of IEdgeServerCommand instances
	 */
	public IEdgeServerCommand[] getRegisteredCommands() {
		return commands.toArray(t -> new IEdgeServerCommand[t]);
	}

	/**
	 * Registers commands
	 * 
	 * @param command Command to register
	 */
	public void registerCommand(IEdgeServerCommand command) {
		if (commands.stream().anyMatch(t -> t.id().equals(command.id())))
			throw new IllegalArgumentException("Command already registered: " + command.id());
		commands.add(command);
		logger.debug("Registered command: " + command.id());
	}

	/**
	 * Retrieves command context for a account
	 * 
	 * @param account Account to use
	 * @return CommandContext instance
	 */
	public static CommandContext getFor(AccountObject account) {
		return implementationProvider.getForAccount(account);
	}

	/**
	 * Retrieves the account object
	 * 
	 * @return CommandContext instance
	 */
	public AccountObject getAccountObject() {
		return AccountManager.getInstance().getAccount(account);
	}

	/**
	 * Retrieves the command permission context
	 * 
	 * @return PermissionContext instance
	 */
	public PermissionContext getPermissions() {
		return ctx;
	}

	/**
	 * Called to run commands
	 * 
	 * @param command                 Command to run
	 * @param outputWriteLineCallback Command output write callback to use
	 * @return True if successful, false otherwise
	 */
	public boolean runCommand(String command, Consumer<String> outputWriteLineCallback) {
		ArrayList<String> cmd;
		try {
			cmd = parseCommand(command);
		} catch (Exception e) {
			outputWriteLineCallback.accept("Malformed command: " + command);
			return false;
		}

		// Check
		if (cmd.size() <= 0) {
			outputWriteLineCallback.accept(
					"Malformed command: " + command + ": missing command ID, please use 'help' for a list of commands");
			return false;
		}

		// Run command
		String id = cmd.get(0);
		cmd.remove(0);
		return runCommand(id, cmd.toArray(t -> new String[t]), outputWriteLineCallback, Map.of());
	}

	/**
	 * Called to run commands
	 * 
	 * @param id                      Command ID to run
	 * @param args                    Command arguments
	 * @param outputWriteLineCallback Command output write callback to use
	 * @param dataBlobs               Objects to pass to the command
	 * @return True if successful, false otherwise
	 */
	public boolean runCommand(String id, String[] args, Consumer<String> outputWriteLineCallback,
			Map<String, String> dataBlobs) {
		// Find command
		String cmdStr = id;
		for (String arg : args) {
			if (arg.contains(" "))
				cmdStr += " \"" + arg.replace("\\\"", "\\\\\"").replace("\"", "\\\"") + "\"";
			else
				cmdStr += " " + arg.replace("\\\"", "\\\\\"").replace("\"", "\\\"");
		}
		boolean success = false;
		String result = "Command not found, please use 'help' for a list of commands";
		for (IEdgeServerCommand c : commands) {
			if (c.id().equalsIgnoreCase(id)) {
				if (getPermissions().hasPermission(c.permNode(), c.permLevel())) {
					// Found it
					try {
						result = c.run(args, this, logger, t -> {
							outputWriteLineCallback.accept(t);
							if (logCommands)
								logger.info(id + " (" + getAccountObject().getUsername() + ") : " + t);
						}, dataBlobs);
						if (result != null)
							success = true;
						break;
					} catch (Exception e) {
						result = "An internal server error occured while processing the command";
						success = false;
						logger.error("An error occured while processing command '" + cmdStr + "' (issued by "
								+ getAccountObject().getUsername() + ")", e);
						break;
					}
				}
			}
		}

		// Log
		if (result != null) {
			outputWriteLineCallback.accept(result);
			if (logCommands)
				logger.info(getAccountObject().getUsername() + " issued command: " + cmdStr + " : " + result);
		}
		return success;
	}

	// Command parser
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

}
