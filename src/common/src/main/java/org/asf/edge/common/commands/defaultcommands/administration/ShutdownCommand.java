package org.asf.edge.common.commands.defaultcommands.administration;

import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.asf.edge.common.commands.CommandContext;
import org.asf.edge.common.commands.IEdgeServerCommand;
import org.asf.edge.common.permissions.PermissionLevel;
import org.asf.edge.common.services.commondata.CommonDataManager;

import com.google.gson.JsonPrimitive;

public class ShutdownCommand implements IEdgeServerCommand {

	@Override
	public String id() {
		return "shutdownservers";
	}

	@Override
	public String syntax(CommandContext ctx) {
		return null;
	}

	@Override
	public String description(CommandContext ctx) {
		return "Shuts the server network down";
	}

	@Override
	public PermissionLevel permLevel() {
		return PermissionLevel.OPERATOR;
	}

	@Override
	public String permNode() {
		return "commands.operator.shutdown";
	}

	@Override
	public String run(String[] args, CommandContext ctx, Logger logger, Consumer<String> outputWriteLineCallback,
			Map<String, String> dataBlobs) throws Exception {
		CommonDataManager.getInstance().getKeyValueContainer("EDGECOMMON").setEntry("shutdown",
				new JsonPrimitive(System.currentTimeMillis()));
		return "Servers are shutting down, console should be disconnected to prevent errors in the console software.\n"
				+ "Please wait up to 30 seconds, if the console remains open you may need to disconnect from the network manually.";
	}

}
