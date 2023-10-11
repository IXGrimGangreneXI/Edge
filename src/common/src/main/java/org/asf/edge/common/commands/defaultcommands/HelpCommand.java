package org.asf.edge.common.commands.defaultcommands;

import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.asf.edge.common.commands.CommandContext;
import org.asf.edge.common.commands.IEdgeServerCommand;
import org.asf.edge.common.commands.TaskBasedCommand;
import org.asf.edge.common.permissions.PermissionLevel;

public class HelpCommand implements IEdgeServerCommand {

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
		return "Retrieves a list of commands";
	}

	@Override
	public PermissionLevel permLevel() {
		return PermissionLevel.GUEST;
	}

	@Override
	public String permNode() {
		return "commands.everyone.help";
	}

	@Override
	public String run(String[] args, CommandContext ctx, Logger logger, Consumer<String> outputWriteLineCallback,
			Map<String, String> dataBlobs) {
		// List commands
		String msg = "List of known commands:";
		for (IEdgeServerCommand cmd : ctx.getRegisteredCommands()) {
			if (!ctx.getPermissions().hasPermission(cmd.permNode(), cmd.permLevel()))
				continue;
			if (cmd instanceof TaskBasedCommand) {
				// Handle tasks
				msg += handleTasks(cmd, ctx, cmd.id() + " ");
			} else {
				msg += "\n - " + cmd.id();
				String syntax = cmd.syntax(ctx);
				if (syntax != null) {
					msg += " " + syntax;
				}
				msg += " - " + cmd.description(ctx);
			}
		}
		return msg;
	}

	private String handleTasks(IEdgeServerCommand cmd, CommandContext ctx, String pref) {
		String msg = "";
		if (cmd instanceof TaskBasedCommand) {
			TaskBasedCommand cmdT = (TaskBasedCommand) cmd;

			// Go through tasks
			for (IEdgeServerCommand tsk : cmdT.tasks()) {
				if (!ctx.getPermissions().hasPermission(tsk.permNode(), tsk.permLevel()))
					continue;
				if (tsk instanceof TaskBasedCommand) {
					// Handle tasks
					msg += handleTasks(tsk, ctx, cmd.id() + " ");
				} else {
					msg += "\n - " + pref + tsk.id();
					String syntax = tsk.syntax(ctx);
					if (syntax != null) {
						msg += " " + syntax;
					}
					msg += " - " + tsk.description(ctx);
				}
			}
		}
		return msg;
	}

}
