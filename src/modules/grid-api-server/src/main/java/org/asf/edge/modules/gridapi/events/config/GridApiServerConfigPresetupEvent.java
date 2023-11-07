package org.asf.edge.modules.gridapi.events.config;

import org.asf.edge.modules.gridapi.config.GridApiServerConfig;
import org.asf.edge.modules.gridapi.events.GridApiServerEvent;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Grid API server config presetup event - called before the server loads the
 * configuration from disk
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("gridapi.config.presetup")
public class GridApiServerConfigPresetupEvent extends GridApiServerEvent {
	private GridApiServerConfig config;

	public GridApiServerConfigPresetupEvent(GridApiServerConfig config) {
		this.config = config;
	}

	/**
	 * Retrieves the configuration object
	 * 
	 * @return GridApiServerConfig instance
	 */
	public GridApiServerConfig getConfig() {
		return config;
	}

	@Override
	public String eventPath() {
		return "gridapi.config.presetup";
	}

}
