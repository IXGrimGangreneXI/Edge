package org.asf.edge.gameplayapi.commands.defaultcommands.account.profilecommands;

import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
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
		if (ctx.getPermissions().hasPermission("commands.moderator.profiles.select", PermissionLevel.MODERATOR))
			return "<id> \"[owner]\"";
		else
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
			Map<String, String> dataBlobs) {
		AccountObject acc = ctx.getAccountObject();
		if (args.length >= 2 && ctx.getPermissions().hasPermission("commands.moderator.profiles.select",
				PermissionLevel.MODERATOR)) {
			// Find owner
			String username = args[1];
			String id = AccountManager.getInstance().getAccountIdBySaveUsername(username);
			if (id == null)
				id = AccountManager.getInstance().getAccountID(username);
			if (id == null) {
				outputWriteLineCallback.accept("Error: username not recognized");
				return null;
			}
			acc = AccountManager.getInstance().getAccount(id);
		}
		String id = null;
		AccountSaveContainer save = null;
		if (args.length >= 1) {
			id = args[0];
			save = acc.getSave(id);
			if (save == null)
				id = null;
		}
		if (id == null) {
			outputWriteLineCallback.accept("Error: invalid profile");
			return null;
		}
		ctx.getCommandMemory().put("active_account", acc.getAccountID());
		ctx.getCommandMemory().put("active_profile", save.getSaveID());
		return "Selected profile " + save.getUsername();
	}

}
