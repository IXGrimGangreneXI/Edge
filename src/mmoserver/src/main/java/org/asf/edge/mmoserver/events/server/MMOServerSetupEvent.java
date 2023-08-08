package org.asf.edge.mmoserver.events.server;

import org.asf.edge.mmoserver.EdgeMMOServer;
import org.asf.edge.mmoserver.config.MMOServerConfig;
import org.asf.edge.mmoserver.events.MMOServerEvent;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * MMO server setup event - called before the server starts
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("mmo.server.setup")
public class MMOServerSetupEvent extends MMOServerEvent {
	private MMOServerConfig config;

	public MMOServerSetupEvent(MMOServerConfig config, EdgeMMOServer server) {
		super(server);
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
		return "mmo.server.setup";
	}

}
