package org.asf.edge.gameplayapi.commands.defaultcommands.account.profilecommands;

import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.gameplayapi.commands.CommandContext;
import org.asf.edge.gameplayapi.commands.IEdgeServerCommand;
import org.asf.edge.gameplayapi.permissions.PermissionLevel;

public class ProfileListCommand implements IEdgeServerCommand {

	@Override
	public String id() {
		return "list";
	}

	@Override
	public String syntax(CommandContext ctx) {
		if (ctx.getPermissions().hasPermission("commands.moderator.profiles.list", PermissionLevel.MODERATOR))
			return "\"[owner]\"";
		else
			return null;
	}

	@Override
	public String description(CommandContext ctx) {
		return "Lists all profiles";
	}

	@Override
	public PermissionLevel permLevel() {
		return PermissionLevel.GUEST;
	}

	@Override
	public String permNode() {
		return "commands.everyone.profiles.list";
	}

	@Override
	public String run(String[] args, CommandContext ctx, Logger logger, Consumer<String> outputWriteLineCallback,
			String fullCommand) {
		AccountObject acc = ctx.getAccount();
		if (args.length >= 1
				&& ctx.getPermissions().hasPermission("commands.moderator.profiles.list", PermissionLevel.MODERATOR)) {
			// Find owner
			String username = args[0];
			String id = AccountManager.getInstance().getAccountIdBySaveUsername(username);
			if (id == null)
				id = AccountManager.getInstance().getAccountID(username);
			if (id == null) {
				outputWriteLineCallback.accept("Error: username not recognized");
				return null;
			}
			acc = AccountManager.getInstance().getAccount(id);
		}
		String msg = "List of profiles:";
		for (String id : acc.getSaveIDs()) {
			AccountSaveContainer save = acc.getSave(id);
			msg += "\n - " + save.getUsername() + " (id " + id + ")";
		}
		return msg;
	}

}
