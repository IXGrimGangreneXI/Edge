package org.asf.edge.gameplayapi.commands.defaultcommands.administration;

import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.asf.edge.common.permissions.PermissionLevel;
import org.asf.edge.common.services.achievements.AchievementManager;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.gameplayapi.commands.CommandContext;
import org.asf.edge.gameplayapi.commands.IEdgeServerCommand;
import org.asf.edge.gameplayapi.services.quests.QuestManager;

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
		outputWriteLineCallback.accept("Reloading... this can take a while...");

		// Reload ranks
		logger.info("Reloading text filter...");
		TextFilterService.getInstance().reload();

		// Reload quests
		logger.info("Reloading quest manager...");
		QuestManager.getInstance().reload();

		// Reload items
		logger.info("Reloading item and store manager...");
		ItemManager.getInstance().reload();

		// Reload ranks
		logger.info("Reloading achievement and rank manager...");
		AchievementManager.getInstance().reload();

		// Completed
		return "Reload completed";
	}

}
