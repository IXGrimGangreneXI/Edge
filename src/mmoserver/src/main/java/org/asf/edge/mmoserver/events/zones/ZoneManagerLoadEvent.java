package org.asf.edge.mmoserver.events.zones;

import org.asf.edge.mmoserver.services.ZoneManager;
import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Zone manager load event - called after the zone manager has loaded
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("zones.zonemanager.load")
public class ZoneManagerLoadEvent extends EventObject {

	private ZoneManager zoneManager;

	@Override
	public String eventPath() {
		return "items.zonemanager.load";
	}

	public ZoneManagerLoadEvent(ZoneManager zoneManager) {
		this.zoneManager = zoneManager;
	}

	/**
	 * Retrieves the zone manager
	 * 
	 * @return ZoneManager instance
	 */
	public ZoneManager getZoneManager() {
		return zoneManager;
	}

}
