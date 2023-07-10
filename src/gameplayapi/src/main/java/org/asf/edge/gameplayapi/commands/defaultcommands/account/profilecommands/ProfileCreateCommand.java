package org.asf.edge.gameplayapi.commands.defaultcommands.account.profilecommands;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.gameplayapi.commands.CommandContext;
import org.asf.edge.gameplayapi.commands.IEdgeServerCommand;
import org.asf.edge.gameplayapi.permissions.PermissionLevel;

import com.google.gson.JsonPrimitive;

public class ProfileCreateCommand implements IEdgeServerCommand {

	@Override
	public String id() {
		return "create";
	}

	@Override
	public String syntax(CommandContext ctx) {
		if (ctx.getPermissions().hasPermission("commands.admin.profiles.create", PermissionLevel.ADMINISTRATOR))
			return "\"<username>\" \"[owner]\"";
		else
			return "\"<username>\"";
	}

	@Override
	public String description(CommandContext ctx) {
		return "Creates profiles";
	}

	@Override
	public PermissionLevel permLevel() {
		return PermissionLevel.GUEST;
	}

	@Override
	public String permNode() {
		return "commands.everyone.profiles.create";
	}

	@Override
	public String run(String[] args, CommandContext ctx, Logger logger, Consumer<String> outputWriteLineCallback,
			Map<String, String> dataBlobs) {
		AccountObject account = ctx.getAccountObject();
		AccountManager manager = AccountManager.getInstance();
		if (args.length >= 2 && ctx.getPermissions().hasPermission("commands.admin.profiles.create",
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
			account = AccountManager.getInstance().getAccount(id);
		}

		// Check args
		if (args.length < 1) {
			outputWriteLineCallback.accept("Error: missing argument: profile name");
			return null;
		}
		String username = args[0];

		// Check validity
		if (!manager.isValidUsername(username)) {
			// Invalid name
			outputWriteLineCallback.accept("Error: invalid profile name");
			return null;
		}

		// Check filters
		// FIXME: implement this, use the same error response as invalid names for this

		// Check if in use
		boolean inUse = false;
		if (!account.getUsername().equalsIgnoreCase(username) && manager.isUsernameTaken(username)) {
			inUse = true;
		} else {
			// Check if in use by any saves
			AccountObject accF = account;
			if (Stream.of(account.getSaveIDs()).map(t -> accF.getSave(t)).anyMatch(t -> {
				try {
					return t.getUsername().equalsIgnoreCase(username) && t.getSaveData().entryExists("avatar");
				} catch (IOException e) {
					return false;
				}
			})) {
				inUse = true;
			}
		}
		if (inUse) {
			// Invalid name
			outputWriteLineCallback.accept("Error: profile name already in use");
			return null;
		}

		// Create
		outputWriteLineCallback.accept("Creating profile...");

		// Try to create save
		AccountSaveContainer save = account.createSave(username);
		if (save == null) {
			// Invalid
			outputWriteLineCallback.accept("Error: profile could not be created as information was invalid");
			return null;
		}

		// Check currency
		if (account.getAccountID().equalsIgnoreCase(ctx.getAccountObject().getAccountID())) {
			try {
				// Check currency
				AccountDataContainer currencyAccWide = account.getAccountData().getChildContainer("currency");
				int current = 0;
				if (currencyAccWide.entryExists("gems"))
					current = currencyAccWide.getEntry("gems").getAsInt();
				if (current < 250) {
					// Error
					outputWriteLineCallback.accept("Error: not enough gems");
					return null;
				}

				// Remove gems
				current -= 250;
				currencyAccWide.setEntry("gems", new JsonPrimitive(current));
			} catch (IOException e) {
				outputWriteLineCallback.accept("Error: failed to modify currency data");
				return null;
			}
		}

		// Create profile slot
		PlayerInventoryItem itm = account.getInventory().getContainer(1).findFirst(7971);
		if (itm == null)
			itm = account.getInventory().getContainer(1).createItem(7971, 0, -1);
		itm.add(1);

		return "Profile created successfully";
	}

}
