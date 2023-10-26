package org.asf.edge.common.commands.defaultcommands.administration;

import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.asf.edge.common.commands.CommandContext;
import org.asf.edge.common.commands.IEdgeServerCommand;
import org.asf.edge.common.permissions.PermissionLevel;
import org.asf.edge.common.services.commondata.CommonKvDataContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import com.google.gson.JsonPrimitive;

public class ReloadCommand implements IEdgeServerCommand {

	@Override
	public String id() {
		return "reloadcontent";
	}

	@Override
	public String syntax(CommandContext ctx) {
		return null;
	}

	@Override
	public String description(CommandContext ctx) {
		return "Reloads server content settings, items, stores and quests (DOES NOT RELOAD MODULES)";
	}

	@Override
	public PermissionLevel permLevel() {
		return PermissionLevel.ADMINISTRATOR;
	}

	@Override
	public String permNode() {
		return "commands.admin.reloadcontent";
	}

	@Override
	public String run(String[] args, CommandContext ctx, Logger logger, Consumer<String> outputWriteLineCallback,
			Map<String, String> dataBlobs) throws Exception {
		// Start reload
		outputWriteLineCallback.accept("Scheduling reload...");

		// Reload ranks
		logger.info("Scheduling text filter reload...");
		CommonKvDataContainer cont = CommonDataManager.getInstance().getKeyValueContainer("TEXTFILTER");
		cont.setEntry("lastreload", new JsonPrimitive(System.currentTimeMillis()));

		// Reload quests
		logger.info("Scheduling quest manager reload...");
		cont = CommonDataManager.getInstance().getKeyValueContainer("QUESTMANAGER");
		cont.setEntry("lastreload", new JsonPrimitive(System.currentTimeMillis()));

		// Reload items
		logger.info("Scheduling item and store manager reload...");
		cont = CommonDataManager.getInstance().getKeyValueContainer("ITEMMANAGER");
		cont.setEntry("lastreload", new JsonPrimitive(System.currentTimeMillis()));

		// Reload ranks
		logger.info("Scheduling achievement and rank manager reload...");
		cont = CommonDataManager.getInstance().getKeyValueContainer("ACHIEVEMENTMANAGER");
		cont.setEntry("lastreload", new JsonPrimitive(System.currentTimeMillis()));

		// Completed
		return "Reload is now in progress! It will complete automatically.";
	}

}
