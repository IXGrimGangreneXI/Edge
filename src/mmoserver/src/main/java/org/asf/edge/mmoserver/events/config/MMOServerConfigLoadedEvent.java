package org.asf.edge.mmoserver.events.config;

import org.asf.edge.mmoserver.config.MMOServerConfig;
import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * MMO server config load event - called when the server configuration has been
 * loaded
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("mmo.config.loaded")
public class MMOServerConfigLoadedEvent extends EventObject {
	private MMOServerConfig config;

	public MMOServerConfigLoadedEvent(MMOServerConfig config) {
		this.config = config;
	}

	/**
	 * Retrieves the configuration object
	 * 
	 * @return MMOServerConfig instance
	 */
	public MMOServerConfig getConfig() {
		return config;
	}

	@Override
	public String eventPath() {
		return "mmo.config.loaded";
	}

}
