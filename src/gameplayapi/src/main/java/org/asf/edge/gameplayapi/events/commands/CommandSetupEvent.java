package org.asf.edge.gameplayapi.events.commands;

import org.asf.edge.gameplayapi.commands.CommandContext;
import org.asf.edge.gameplayapi.events.GameplayApiServerEvent;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Command setup event
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("gameplayapi.commands.setup")
public class CommandSetupEvent extends GameplayApiServerEvent {

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
