package org.asf.edge.modules.gridapi.events.config;

import org.asf.edge.modules.gridapi.config.GridApiServerConfig;
import org.asf.edge.modules.gridapi.events.GridApiServerEvent;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Grid API server config load event - called when the server configuration has
 * been loaded
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("gridapi.config.loaded")
public class GridApiServerConfigLoadedEvent extends GridApiServerEvent {
	private GridApiServerConfig config;

	public GridApiServerConfigLoadedEvent(GridApiServerConfig config) {
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
		return "gridapi.config.loaded";
	}

}
