package org.asf.edge.common.events.commands;

import org.asf.edge.common.commands.CommandContext;
import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Command setup event
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("gameplayapi.commands.setup")
public class CommandSetupEvent extends EventObject {

	private CommandContext context;

	public CommandSetupEvent(CommandContext context) {
		this.context = context;
	}

	@Override
	public String eventPath() {
		return "gameplayapi.commands.setup";
	}

	/**
	 * Retrieves the command context
	 * 
	 * @return CommandContext instance
	 */
	public CommandContext getCommandContext() {
		return context;
	}

}
