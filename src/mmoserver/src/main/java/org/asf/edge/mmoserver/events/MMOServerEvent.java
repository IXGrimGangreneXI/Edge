package org.asf.edge.mmoserver.events;

import org.asf.edge.mmoserver.EdgeMMOServer;
import org.asf.edge.modules.eventbus.EventObject;

/**
 * 
 * Base mmo server event
 * 
 * @author Sky Swimmer
 *
 */
public abstract class MMOServerEvent extends EventObject {
	private EdgeMMOServer server;

	public MMOServerEvent(EdgeMMOServer server) {
		this.server = server;
	}

	/**
	 * Retrieves the server instance
	 * 
	 * @return EdgeMMOServer instance
	 */
	public EdgeMMOServer getServer() {
		return server;
	}
}
