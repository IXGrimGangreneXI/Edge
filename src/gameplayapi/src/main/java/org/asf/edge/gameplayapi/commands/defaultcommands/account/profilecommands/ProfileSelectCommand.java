package org.asf.edge.gameplayapi.commands.defaultcommands.account.profilecommands;

import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.asf.edge.gameplayapi.commands.CommandContext;
import org.asf.edge.gameplayapi.commands.IEdgeServerCommand;
import org.asf.edge.gameplayapi.permissions.PermissionLevel;

public class ProfileSelectCommand implements IEdgeServerCommand {

	@Override
	public String id() {
		return "select";
	}

	@Override
	public String syntax(CommandContext ctx) {
		return "<id>";
	}

	@Override
	public String description(CommandContext ctx) {
		return "Selects profiles";
	}

	@Override
	public PermissionLevel permLevel() {
		return PermissionLevel.GUEST;
	}

	@Override
	public String permNode() {
		return "commands.everyone.profiles.select";
	}

	@Override
	public String run(String[] args, CommandContext ctx, Logger logger, Consumer<String> outputWriteLineCallback,
			String fullCommand) {
		String id = null;
		if (args.length >= 1) {
			id = args[0];
			if (ctx.getAccount().getSave(id) == null)
				id = null;
		}
		if (id == null) {
			outputWriteLineCallback.accept("Error: invalid profile");
			return null;
		}
		ctx.getCommandMemory().put("active_profile", id);
		return "Selected profile " + ctx.getAccount().getSave(id).getUsername();
	}

}
