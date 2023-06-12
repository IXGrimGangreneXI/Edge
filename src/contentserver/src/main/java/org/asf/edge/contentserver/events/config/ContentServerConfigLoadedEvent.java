package org.asf.edge.contentserver.events.config;

import org.asf.edge.contentserver.config.ContentServerConfig;
import org.asf.edge.contentserver.events.ContentServerEvent;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Content server config load event - called when the server configuration has
 * been loaded
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("contentserver.config.loaded")
public class ContentServerConfigLoadedEvent extends ContentServerEvent {
	private ContentServerConfig config;

	public ContentServerConfigLoadedEvent(ContentServerConfig config) {
		this.config = config;
	}

	/**
	 * Retrieves the configuration object
	 * 
	 * @return ContentServerConfig instance
	 */
	public ContentServerConfig getConfig() {
		return config;
	}

	@Override
	public String eventPath() {
		return "contentserver.config.loaded";
	}

}
