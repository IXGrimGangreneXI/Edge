package org.asf.edge.contentserver.events.server;

import org.asf.edge.contentserver.EdgeContentServer;
import org.asf.edge.contentserver.config.ContentServerConfig;
import org.asf.edge.contentserver.events.ContentServerEvent;

/**
 * 
 * Content server startup event - called when the server starts
 * 
 * @author Sky Swimmer
 *
 */
public class ContentServerStartupEvent extends ContentServerEvent {
	private ContentServerConfig config;
	private EdgeContentServer server;

	public ContentServerStartupEvent(ContentServerConfig config, EdgeContentServer server) {
		this.config = config;
		this.server = server;
	}

	/**
	 * Retrieves the server instance
	 * 
	 * @return EdgeContentServer instance
	 */
	public EdgeContentServer getServer() {
		return server;
	}

	/**
	 * Retrieves the configuration object
	 * 
	 * @return ContentServerConfig instance
	 */
	public ContentServerConfig getConfig() {
		return config;
	}

}
