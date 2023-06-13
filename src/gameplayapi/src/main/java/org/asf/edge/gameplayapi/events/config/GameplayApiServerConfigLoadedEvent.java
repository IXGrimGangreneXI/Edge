package org.asf.edge.gameplayapi.events.config;

import org.asf.edge.gameplayapi.config.GameplayApiServerConfig;
import org.asf.edge.gameplayapi.events.GameplayApiServerEvent;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Gameplay API server config load event - called when the server configuration
 * has been loaded
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("gameplayapi.config.loaded")
public class GameplayApiServerConfigLoadedEvent extends GameplayApiServerEvent {
	private GameplayApiServerConfig config;

	public GameplayApiServerConfigLoadedEvent(GameplayApiServerConfig config) {
		this.config = config;
	}

	/**
	 * Retrieves the configuration object
	 * 
	 * @return GameplayApiServerConfig instance
	 */
	public GameplayApiServerConfig getConfig() {
		return config;
	}

	@Override
	public String eventPath() {
		return "gameplayapi.config.loaded";
	}

}
