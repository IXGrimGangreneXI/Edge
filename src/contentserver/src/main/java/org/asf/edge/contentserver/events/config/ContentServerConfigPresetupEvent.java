package org.asf.edge.contentserver.events.config;

import org.asf.edge.contentserver.config.ContentServerConfig;
import org.asf.edge.contentserver.events.ContentServerEvent;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Content server config presetup event - called before the server loads the
 * configuration from disk
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("contentserver.config.presetup")
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

	@Override
	public String eventPath() {
		return "contentserver.config.presetup";
	}

}
