package org.asf.edge.mmoserver.events.config;

import org.asf.edge.mmoserver.config.MMOServerConfig;
import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

/**
 * 
 * MMO server config presetup event - called before the server loads the
 * configuration from disk
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("mmo.config.presetup")
public class MMOServerConfigPresetupEvent extends EventObject {
	private MMOServerConfig config;

	public MMOServerConfigPresetupEvent(MMOServerConfig config) {
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
		return "mmo.config.presetup";
	}

}
