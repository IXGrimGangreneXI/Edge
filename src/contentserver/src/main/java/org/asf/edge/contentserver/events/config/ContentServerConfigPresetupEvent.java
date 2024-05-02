package org.asf.edge.contentserver.events.config;

import org.asf.edge.contentserver.config.ContentServerConfig;
import org.asf.edge.contentserver.events.ContentServerEvent;

/**
 * 
 * Content server config presetup event - called before the server loads the
 * configuration from disk
 * 
 * @author Sky Swimmer
 *
 */
public class ContentServerConfigPresetupEvent extends ContentServerEvent {
	private ContentServerConfig config;

	public ContentServerConfigPresetupEvent(ContentServerConfig config) {
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

}
