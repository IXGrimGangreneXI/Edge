package org.asf.edge.modules.gridapi.commands.defaultcommands;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.asf.edge.modules.gridapi.commands.CommandContext;
import org.asf.edge.modules.gridapi.commands.IGridServerCommand;
import org.asf.edge.modules.gridapi.utils.PhoenixToken;

public class GenerateGridClientTokenCommand implements IGridServerCommand {

	@Override
	public String id() {
		return "genclienttoken";
	}

	@Override
	public String syntax(CommandContext ctx) {
		return null;
	}

	@Override
	public String description(CommandContext ctx) {
		return "Generates a API Grid client token";
	}

	@Override
	public String run(String[] args, CommandContext ctx, Logger logger, Consumer<String> outputWriteLineCallback,
			Map<String, String> dataBlobs) throws Exception {
		// Build token
		PhoenixToken token = new PhoenixToken();
		token.gameID = "nexusgrid";
		token.identity = new UUID(0, 0).toString();
		token.lastUpdate = -1;
		token.tokenNotBefore = (System.currentTimeMillis() / 1000) + 5;
		token.tokenGenerationTime = System.currentTimeMillis() / 1000;
		ArrayList<String> caps = new ArrayList<String>();
		caps.add("gridclient");
		token.capabilities = caps.toArray(t -> new String[t]);
		return "Token generated: " + token.toTokenString();
	}

}
