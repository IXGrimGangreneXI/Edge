package org.asf.edge.common.commands.defaultcommands.account.profilecommands;

import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.asf.edge.common.commands.CommandContext;
import org.asf.edge.common.commands.IEdgeServerCommand;
import org.asf.edge.common.permissions.PermissionLevel;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;

public class ProfileSelectCommand implements IEdgeServerCommand {

	@Override
	public String id() {
		return "select";
	}

	@Override
	public String syntax(CommandContext ctx) {
		if (ctx.getPermissions().hasPermission("commands.moderator.profiles.select", PermissionLevel.MODERATOR))
			return "\"<name>\" \"[owner]\"";
		else
			return "\"<id>\"";
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
		String n = null;
		AccountSaveContainer save = null;
		if (args.length >= 1) {
			n = args[0];
			for (String sID : acc.getSaveIDs()) {
				AccountSaveContainer sv = acc.getSave(sID);
				if (sv.getUsername().equalsIgnoreCase(n)) {
					save = sv;
					break;
				}
			}
			if (save == null)
				n = null;
		}
		if (n == null) {
			outputWriteLineCallback.accept("Error: profile name not recognized");
			return null;
		}
		ctx.getCommandMemory().put("active_account", acc.getAccountID());
		ctx.getCommandMemory().put("active_profile", save.getSaveID());
		return "Selected profile " + save.getUsername();
	}

}
