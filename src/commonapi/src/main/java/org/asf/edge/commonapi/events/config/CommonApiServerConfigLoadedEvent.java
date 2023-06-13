package org.asf.edge.commonapi.events.config;

import org.asf.edge.commonapi.config.CommonApiServerConfig;
import org.asf.edge.commonapi.events.CommonApiServerEvent;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Common API server config load event - called when the server configuration
 * has been loaded
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("commonapi.config.loaded")
public class CommonApiServerConfigLoadedEvent extends CommonApiServerEvent {
	private CommonApiServerConfig config;

	public CommonApiServerConfigLoadedEvent(CommonApiServerConfig config) {
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
		return "commonapi.config.loaded";
	}

}
