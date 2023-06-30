package org.asf.edge.gameplayapi.commands.defaultcommands.account.profilecommands;

import java.util.ArrayList;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.asf.edge.gameplayapi.commands.CommandContext;
import org.asf.edge.gameplayapi.commands.IEdgeServerCommand;
import org.asf.edge.gameplayapi.permissions.PermissionLevel;

public class ProfilesHelpCommand implements IEdgeServerCommand {

	private ArrayList<IEdgeServerCommand> cmds;

	public ProfilesHelpCommand(ArrayList<IEdgeServerCommand> commands) {
		cmds = commands;
	}

	@Override
	public String id() {
		return "help";
	}

	@Override
	public String syntax(CommandContext ctx) {
		return null;
	}

	@Override
	public String description(CommandContext ctx) {
		return "Retrieves a list of tasks";
	}

	@Override
	public PermissionLevel permLevel() {
		return PermissionLevel.GUEST;
	}

	@Override
	public String permNode() {
		return "commands.everyone.profiles.help";
	}

	@Override
	public String run(String[] args, CommandContext ctx, Logger logger, Consumer<String> outputWriteLineCallback,
			String fullCommand) {
		// List commands
		String msg = "List of known tasks:";
		for (IEdgeServerCommand cmd : cmds) {
			if (!ctx.getPermissions().hasPermission(cmd.permNode(), cmd.permLevel()))
				continue;
			msg += "\n - profiles " + cmd.id();
			String syntax = cmd.syntax(ctx);
			if (syntax != null) {
				msg += " " + syntax;
			}
			msg += " - " + cmd.description(ctx);
		}
		return msg;
	}

}
