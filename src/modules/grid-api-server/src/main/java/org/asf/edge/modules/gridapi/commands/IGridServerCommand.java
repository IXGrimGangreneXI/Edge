package org.asf.edge.modules.gridapi.commands;

import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;

/**
 * 
 * Grid Server Command Interface, should be registered to CommandContext for use
 * 
 * @author Sky Swimmer
 *
 */
public interface IGridServerCommand {

	/**
	 * Defines the command ID
	 * 
	 * @return Command ID string
	 */
	public String id();

	/**
	 * Defines the command syntax (for the help message, null if the command has no
	 * arguments)
	 * 
	 * @param ctx CommandContext instance
	 * @return Command syntax string or null if there are no arguments
	 */
	public String syntax(CommandContext ctx);

	/**
	 * Defines the command description (for the help message)
	 * 
	 * @param ctx CommandContext instance
	 * @return Command description string
	 */
	public String description(CommandContext ctx);

	/**
	 * Called to run the command
	 * 
	 * @param args                    Command arguments
	 * @param ctx                     CommandContext instance
	 * @param logger                  Logger instance
	 * @param outputWriteLineCallback Callback to write to chat/terminal output
	 * @param dataBlobs               Objects passed to the command
	 * @return Command output string, null to fail
	 */
	public String run(String[] args, CommandContext ctx, Logger logger, Consumer<String> outputWriteLineCallback,
			Map<String, String> dataBlobs) throws Exception;

}
