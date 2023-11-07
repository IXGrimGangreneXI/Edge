package org.asf.edge.gameplayapi.events.config;

import org.asf.edge.gameplayapi.config.GameplayApiServerConfig;
import org.asf.edge.gameplayapi.events.GameplayApiServerEvent;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Gameplay API server config presetup event - called before the server loads
 * the configuration from disk
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("gameplayapi.config.presetup")
public class GameplayApiServerConfigPresetupEvent extends GameplayApiServerEvent {
	private GameplayApiServerConfig config;

	public GameplayApiServerConfigPresetupEvent(GameplayApiServerConfig config) {
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
		return "gameplayapi.config.presetup";
	}

}
