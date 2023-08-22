package org.asf.edge.modules.gridapi.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.logging.log4j.Logger;

/**
 *
 * Task-based command abstract
 * 
 * @author Sky Swimmer
 *
 */
public abstract class TaskBasedCommand implements IGridServerCommand {

	/**
	 * Defines the command tasks
	 * 
	 * @return Array of IEdgeServerCommand instances
	 */
	public abstract IGridServerCommand[] tasks();

	@Override
	public String syntax(CommandContext ctx) {
		return "<task> [arguments...]";
	}

	@Override
	public String run(String[] args, CommandContext ctx, Logger logger, Consumer<String> outputWriteLineCallback,
			Map<String, String> dataBlobs) throws Exception {
		// Check arguments
		ArrayList<String> cmd = new ArrayList<String>(Arrays.asList(args));

		// Check
		if (cmd.size() <= 0) {
			return "Error: missing task ID, please use 'help' for a list of commands and tasks";
		}

		// Find command
		String result = "Task not found, please use 'help' for a list of commands and tasks";
		String id = cmd.get(0);
		cmd.remove(0);
		for (IGridServerCommand c : tasks()) {
			if (c.id().equalsIgnoreCase(id)) {
				// Found it
				result = c.run(cmd.toArray(t -> new String[t]), ctx, logger, outputWriteLineCallback, dataBlobs);
				break;
			}
		}

		// Log
		return result;
	}

}
