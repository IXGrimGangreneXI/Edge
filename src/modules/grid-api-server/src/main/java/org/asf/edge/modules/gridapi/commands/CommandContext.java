package org.asf.edge.modules.gridapi.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.modules.eventbus.EventBus;
import org.asf.edge.modules.gridapi.commands.defaultcommands.AccountCommands;
import org.asf.edge.modules.gridapi.commands.defaultcommands.GenerateMasterTokenCommand;
import org.asf.edge.modules.gridapi.commands.defaultcommands.GenerateTokenCommand;
import org.asf.edge.modules.gridapi.commands.defaultcommands.HelpCommand;
import org.asf.edge.modules.gridapi.commands.defaultcommands.IdentityCommands;
import org.asf.edge.modules.gridapi.commands.defaultcommands.ServerCommands;
import org.asf.edge.modules.gridapi.events.CommandSetupEvent;

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
	private ArrayList<IGridServerCommand> commands = new ArrayList<IGridServerCommand>();
	private boolean logCommands;

	public void enableCommandLogging() {
		logCommands = true;
	}

	protected static CommandContextImplementationProvider implementationProvider = () -> new CommandContext();

	public static interface CommandContextImplementationProvider {
		public CommandContext openSession();
	}

	public CommandContext() {
		// Register commands
		logger.info("Registering commands...");
		registerCommand(new GenerateMasterTokenCommand());
		registerCommand(new IdentityCommands());
		registerCommand(new AccountCommands());
		registerCommand(new ServerCommands());
		registerCommand(new GenerateTokenCommand());

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
	public IGridServerCommand[] getRegisteredCommands() {
		return commands.toArray(t -> new IGridServerCommand[t]);
	}

	/**
	 * Registers commands
	 * 
	 * @param command Command to register
	 */
	public void registerCommand(IGridServerCommand command) {
		if (commands.stream().anyMatch(t -> t.id().equals(command.id())))
			throw new IllegalArgumentException("Command already registered: " + command.id());
		commands.add(command);
		logger.info("Registered command: " + command.id());
	}

	/**
	 * Retrieves command context for a account
	 * 
	 * @return CommandContext instance
	 */
	public static CommandContext openSession() {
		return implementationProvider.openSession();
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
		for (IGridServerCommand c : commands) {
			if (c.id().equalsIgnoreCase(id)) {
				// Found it
				try {
					result = c.run(args, this, logger, t -> {
						outputWriteLineCallback.accept(t);
						if (logCommands)
							logger.info(id + " : " + t);
					}, dataBlobs);
					if (result != null)
						success = true;
					break;
				} catch (Exception e) {
					result = "An internal server error occured while processing the command";
					success = false;
					logger.error("An error occured while processing command '" + cmdStr + "'", e);
					break;
				}
			}
		}

		// Log
		if (result != null) {
			outputWriteLineCallback.accept(result);
			if (logCommands)
				logger.info("Issued command: " + cmdStr + " : " + result);
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
