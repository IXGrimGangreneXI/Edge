package org.asf.edge.common.commands.defaultcommands.account.profilecommands;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.asf.edge.common.commands.CommandContext;
import org.asf.edge.common.commands.IEdgeServerCommand;
import org.asf.edge.common.permissions.PermissionLevel;
import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountSaveContainer;

import com.google.gson.JsonObject;

public class ProfileResetLocationCommand implements IEdgeServerCommand {

	@Override
	public String id() {
		return "resetlocation";
	}

	@Override
	public String syntax(CommandContext ctx) {
		return "";
	}

	@Override
	public String description(CommandContext ctx) {
		return "Resets where the player spawns (use this to escape broken scene loads)";
	}

	@Override
	public PermissionLevel permLevel() {
		return PermissionLevel.GUEST;
	}

	@Override
	public String permNode() {
		return "commands.everyone.profiles.resetlocation";
	}

	@Override
	public String run(String[] args, CommandContext ctx, Logger logger, Consumer<String> outputWriteLineCallback,
			Map<String, String> dataBlobs) throws IOException {
		// Check
		if (!ctx.getCommandMemory().containsKey("active_profile")) {
			outputWriteLineCallback
					.accept("Error: no active profile, please use 'profiles select' before using this command");
			return null;
		}
		AccountSaveContainer save = AccountManager.getInstance()
				.getAccount(ctx.getCommandMemory().get("active_account").toString())
				.getSave(ctx.getCommandMemory().get("active_profile").toString());
		AccountKvDataContainer data = save.getSaveData();
		data = data.getChildContainer("keyvaluedata");

		// Set entries
		JsonObject pairData = new JsonObject();
		if (data.entryExists("pairs-2017")) {
			pairData = data.getEntry("pairs-2017").getAsJsonObject();
			if (pairData.has("sceneName"))
				pairData.remove("sceneName");
			data.setEntry("pairs-2017", pairData);
		}
		return "Location resetted successfully";
	}

}
