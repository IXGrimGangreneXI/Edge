package org.asf.edge.modules.gridapi.events.server;

import org.asf.edge.modules.gridapi.EdgeGridApiServer;
import org.asf.edge.modules.gridapi.config.GridApiServerConfig;
import org.asf.edge.modules.gridapi.events.GridApiServerEvent;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Grid API server startup event - called when the server starts
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("gridapi.server.startup")
public class GridApiServerStartupEvent extends GridApiServerEvent {
	private GridApiServerConfig config;
	private EdgeGridApiServer server;

	public GridApiServerStartupEvent(GridApiServerConfig config, EdgeGridApiServer server) {
		this.config = config;
		this.server = server;
	}

	/**
	 * Retrieves the server instance
	 * 
	 * @return EdgeGridApiServer instance
	 */
	public EdgeGridApiServer getServer() {
		return server;
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
		return "gridapi.server.startup";
	}

}
