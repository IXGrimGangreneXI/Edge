package org.asf.edge.modules.gridapi.events;

import org.asf.edge.modules.gridapi.commands.CommandContext;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Command setup event
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("gridapi.commands.setup")
public class CommandSetupEvent extends GridApiServerEvent {

	private CommandContext context;

	public CommandSetupEvent(CommandContext context) {
		this.context = context;
	}

	@Override
	public String eventPath() {
		return "gridapi.commands.setup";
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
