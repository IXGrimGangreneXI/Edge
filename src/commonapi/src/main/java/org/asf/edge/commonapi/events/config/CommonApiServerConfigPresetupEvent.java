package org.asf.edge.commonapi.events.config;

import org.asf.edge.commonapi.config.CommonApiServerConfig;
import org.asf.edge.commonapi.events.CommonApiServerEvent;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Common API server config presetup event - called before the server loads
 * the configuration from disk
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("commonapi.config.presetup")
public class CommonApiServerConfigPresetupEvent extends CommonApiServerEvent {
	private CommonApiServerConfig config;

	public CommonApiServerConfigPresetupEvent(CommonApiServerConfig config) {
		this.config = config;
	}

	/**
	 * Retrieves the configuration object
	 * 
	 * @return CommonApiServerConfig instance
	 */
	public CommonApiServerConfig getConfig() {
		return config;
	}

	@Override
	public String eventPath() {
		return "commonapi.config.presetup";
	}

}
