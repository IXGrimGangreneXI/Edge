package org.asf.edge.common.commands.defaultcommands.account.profilecommands;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.asf.edge.common.commands.CommandContext;
import org.asf.edge.common.commands.IEdgeServerCommand;
import org.asf.edge.common.permissions.PermissionLevel;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;

import com.google.gson.JsonPrimitive;

public class ProfileDeleteCommand implements IEdgeServerCommand {

	@Override
	public String id() {
		return "delete";
	}

	@Override
	public String syntax(CommandContext ctx) {
		if (ctx.getPermissions().hasPermission("commands.admin.profiles.delete", PermissionLevel.ADMINISTRATOR))
			return "<id> \"[owner]\"";
		else
			return "<id>";
	}

	@Override
	public String description(CommandContext ctx) {
		return "Deletes profiles";
	}

	@Override
	public PermissionLevel permLevel() {
		return PermissionLevel.GUEST;
	}

	@Override
	public String permNode() {
		return "commands.everyone.profiles.delete";
	}

	@Override
	public String run(String[] args, CommandContext ctx, Logger logger, Consumer<String> outputWriteLineCallback,
			Map<String, String> dataBlobs) {
		AccountObject acc = ctx.getAccountObject();
		if (args.length >= 2 && ctx.getPermissions().hasPermission("commands.admin.profiles.delete",
				PermissionLevel.ADMINISTRATOR)) {
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
		if (args.length >= 1) {
			id = args[0];
			if (ctx.getAccountObject().getSave(id) == null)
				id = null;
		}
		if (id == null) {
			outputWriteLineCallback.accept("Error: invalid profile");
			return null;
		}

		// Check currency
		if (acc.getAccountID().equalsIgnoreCase(ctx.getAccountObject().getAccountID())) {
			try {
				// Check currency
				AccountDataContainer currencyAccWide = acc.getAccountData().getChildContainer("currency");
				int current = 0;
				if (currencyAccWide.entryExists("gems"))
					current = currencyAccWide.getEntry("gems").getAsInt();
				if (current < 50) {
					// Error
					outputWriteLineCallback.accept("Error: not enough gems");
					return null;
				}

				// Remove gems
				current -= 50;
				currencyAccWide.setEntry("gems", new JsonPrimitive(current));
			} catch (IOException e) {
				outputWriteLineCallback.accept("Error: failed to modify currency data");
				return null;
			}
		}

		// Delete
		outputWriteLineCallback.accept("Deleting profile...");
		acc.getSave(id).deleteSave();
		return "Profile deleted successfully";
	}

}
