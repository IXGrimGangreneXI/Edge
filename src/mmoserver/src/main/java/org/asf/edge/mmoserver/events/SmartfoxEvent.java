package org.asf.edge.mmoserver.events;

import org.asf.edge.mmoserver.networking.SmartfoxClient;
import org.asf.edge.mmoserver.networking.SmartfoxServer;
import org.asf.nexus.events.EventObject;

/**
 * 
 * Smartfox Event Type
 * 
 * @author Sky Swimmer
 *
 */
public abstract class SmartfoxEvent extends EventObject {

	private SmartfoxClient client;
	private SmartfoxServer server;

	public SmartfoxEvent(SmartfoxServer server, SmartfoxClient client) {
		this.server = server;
		this.client = client;
	}

	/**
	 * Retrieves the smartfox client
	 * 
	 * @return SmartfoxClient instance
	 */
	public SmartfoxClient getClient() {
		return client;
	}

	/**
	 * Retrieves the smartfox server
	 * 
	 * @return SmartfoxServer instance
	 */
	public SmartfoxServer getServer() {
		return server;
	}

}
