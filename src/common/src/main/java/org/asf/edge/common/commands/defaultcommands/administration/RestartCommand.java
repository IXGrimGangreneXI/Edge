package org.asf.edge.common.commands.defaultcommands.administration;

import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.asf.edge.common.commands.CommandContext;
import org.asf.edge.common.commands.IEdgeServerCommand;
import org.asf.edge.common.permissions.PermissionLevel;
import org.asf.edge.common.services.commondata.CommonDataManager;

import com.google.gson.JsonPrimitive;

public class RestartCommand implements IEdgeServerCommand {

	@Override
	public String id() {
		return "restartservers";
	}

	@Override
	public String syntax(CommandContext ctx) {
		return null;
	}

	@Override
	public String description(CommandContext ctx) {
		return "Restarts the server network";
	}

	@Override
	public PermissionLevel permLevel() {
		return PermissionLevel.OPERATOR;
	}

	@Override
	public String permNode() {
		return "commands.operator.restart";
	}

	@Override
	public String run(String[] args, CommandContext ctx, Logger logger, Consumer<String> outputWriteLineCallback,
			Map<String, String> dataBlobs) throws Exception {
		CommonDataManager.getInstance().getContainer("EDGECOMMON")
				.setEntry("restart",
				new JsonPrimitive(System.currentTimeMillis()));
		return "Servers are restarting, console should be disconnected to prevent errors in the console software.\n"
				+ "Please wait up to 30 seconds, if the console remains open you may need to disconnect from the network manually.";
	}

}
