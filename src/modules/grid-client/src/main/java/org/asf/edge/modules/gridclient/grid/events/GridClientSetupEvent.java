package org.asf.edge.modules.gridclient.grid.events;

import org.asf.edge.modules.gridclient.phoenix.PhoenixClient;
import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Grid Client Setup Event - called when the Phoenix client is being set up
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("grid.client.setup")
public class GridClientSetupEvent extends EventObject {

	private PhoenixClient client;

	public GridClientSetupEvent(PhoenixClient client) {
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
		return "grid.client.setup";
	}

}
