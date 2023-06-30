package org.asf.edge.gameplayapi.commands.defaultcommands.account;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.asf.edge.gameplayapi.commands.CommandContext;
import org.asf.edge.gameplayapi.commands.IEdgeServerCommand;
import org.asf.edge.gameplayapi.commands.defaultcommands.account.profilecommands.ProfileCreateCommand;
import org.asf.edge.gameplayapi.commands.defaultcommands.account.profilecommands.ProfileDeleteCommand;
import org.asf.edge.gameplayapi.commands.defaultcommands.account.profilecommands.ProfileListCommand;
import org.asf.edge.gameplayapi.commands.defaultcommands.account.profilecommands.ProfileSelectCommand;
import org.asf.edge.gameplayapi.commands.defaultcommands.account.profilecommands.ProfilesHelpCommand;
import org.asf.edge.gameplayapi.permissions.PermissionLevel;

public class ProfilesCommand implements IEdgeServerCommand {
	private ArrayList<IEdgeServerCommand> subCommands = new ArrayList<IEdgeServerCommand>();

	public ProfilesCommand() {
		// Add commands
		subCommands.add(new ProfileListCommand());
		subCommands.add(new ProfileCreateCommand());
		subCommands.add(new ProfileDeleteCommand());
		subCommands.add(new ProfileSelectCommand());

		// Add help command
		subCommands.add(new ProfilesHelpCommand(subCommands));
	}

	@Override
	public String id() {
		return "profiles";
	}

	@Override
	public String syntax(CommandContext ctx) {
		return "<task> [arguments...]";
	}

	@Override
	public String description(CommandContext ctx) {
		return "profile selection and management";
	}

	@Override
	public PermissionLevel permLevel() {
		return PermissionLevel.GUEST;
	}

	@Override
	public String permNode() {
		return "commands.admin.profilemanagement";
	}

	@Override
	public String run(String[] args, CommandContext ctx, Logger logger, Consumer<String> outputWriteLineCallback,
			String fullCommand) {
		// Find command
		ArrayList<String> cmd = new ArrayList<String>(Arrays.asList(args));

		// Check
		if (cmd.size() <= 0) {
			return "Malformed command: " + fullCommand + ": missing task ID, please use 'help' for a list of commands";
		}

		// Find command
		String result = "Command not found, please use 'help' for a list of commands";
		String id = cmd.get(0);
		cmd.remove(0);
		for (IEdgeServerCommand c : subCommands) {
			if (c.id().equalsIgnoreCase(id)) {
				// Found it
				if (ctx.getPermissions().hasPermission(c.permNode(), c.permLevel())) {
					result = c.run(cmd.toArray(t -> new String[t]), ctx, logger, outputWriteLineCallback, fullCommand);
					break;
				}
			}
		}

		// Log
		return result;
	}

}
