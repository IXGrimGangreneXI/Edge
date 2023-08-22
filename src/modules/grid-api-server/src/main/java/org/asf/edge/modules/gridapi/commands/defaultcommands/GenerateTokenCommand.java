package org.asf.edge.modules.gridapi.commands.defaultcommands;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.modules.gridapi.commands.CommandContext;
import org.asf.edge.modules.gridapi.commands.IGridServerCommand;
import org.asf.edge.modules.gridapi.identities.IdentityDef;
import org.asf.edge.modules.gridapi.utils.IdentityUtils;
import org.asf.edge.modules.gridapi.utils.PhoenixToken;

import com.google.gson.JsonPrimitive;

public class GenerateTokenCommand implements IGridServerCommand {

	@Override
	public String id() {
		return "gentoken";
	}

	@Override
	public String syntax(CommandContext ctx) {
		return "\"<identity-id>\" <expiry-in-days> [capabilities...]";
	}

	@Override
	public String description(CommandContext ctx) {
		return "Generates a API identity token";
	}

	@Override
	public String run(String[] args, CommandContext ctx, Logger logger, Consumer<String> outputWriteLineCallback,
			Map<String, String> dataBlobs) throws Exception {
		// Check arguments
		if (args.length < 1) {
			outputWriteLineCallback.accept("Missing argument: identity-id");
			return null;
		} else if (args.length < 2) {
			outputWriteLineCallback.accept("Missing argument: expiry-in-days");
			return null;
		} else if (!args[1].matches("^-?[0-9]+$")) {
			outputWriteLineCallback.accept("Invalid argument: expiry-in-days: expected a number");
			return null;
		}

		// Find identity
		long lastUpdateTime = -1;
		IdentityDef id = IdentityUtils.getIdentity(args[0]);
		if (id != null)
			lastUpdateTime = id.lastUpdateTime;
		else {
			// Find account
			AccountObject acc = AccountManager.getInstance().getAccount(args[0]);
			if (acc != null) {
				AccountDataContainer data = acc.getAccountData().getChildContainer("accountdata");
				if (!data.entryExists("last_update"))
					data.setEntry("last_update", new JsonPrimitive(System.currentTimeMillis()));
				lastUpdateTime = data.getEntry("last_update").getAsLong();
			} else
				return "Error: identity not found";
		}

		// Build token
		PhoenixToken token = new PhoenixToken();
		token.gameID = "nexusgrid";
		token.identity = id.identity;
		token.lastUpdate = lastUpdateTime;
		token.tokenNotBefore = (System.currentTimeMillis() / 1000) + 5;
		token.tokenExpiryTime = Integer.parseInt(args[1]) == -1 ? -1
				: (System.currentTimeMillis() / 1000) + (Integer.parseInt(args[1]) * 24 * 60 * 60);
		token.tokenGenerationTime = System.currentTimeMillis() / 1000;
		ArrayList<String> caps = new ArrayList<String>();
		for (int i = 2; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("master") || args[i].equalsIgnoreCase("login")
					|| args[i].equalsIgnoreCase("play"))
				continue;
			caps.add(args[i]);
		}
		token.capabilities = caps.toArray(t -> new String[t]);
		return "Token generated: " + token.toTokenString();
	}

}
