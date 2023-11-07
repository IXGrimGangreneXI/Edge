package org.asf.edge.modules.gridclient.phoenix.events;

import org.asf.edge.modules.gridclient.phoenix.PhoenixClient;
import org.asf.nexus.events.EventObject;

/**
 * 
 * Phoenix Event Type
 * 
 * @author Sky Swimmer
 *
 */
public abstract class PhoenixEvent extends EventObject {

	private PhoenixClient client;

	public PhoenixEvent(PhoenixClient client) {
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

}
