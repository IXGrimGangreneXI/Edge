package org.asf.edge.modules.gridclient.grid.events;

import org.asf.edge.modules.gridclient.phoenix.PhoenixClient;
import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Grid Client Connected Event - called when the Grid client has connected
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("grid.client.connected")
public class GridClientConnectedEvent extends EventObject {

	private PhoenixClient client;

	public GridClientConnectedEvent(PhoenixClient client) {
		this.client = client;
	}

	/**
	 * Retrieves the phoenix client
	 * 
	 * @return PhoenixClient instance
	 */
	public PhoenixClient getClient() {
		return client;
	}

	@Override
	public String eventPath() {
		return "grid.client.connected";
	}

}
