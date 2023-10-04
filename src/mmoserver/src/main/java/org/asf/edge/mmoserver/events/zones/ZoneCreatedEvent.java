package org.asf.edge.mmoserver.events.zones;

import org.asf.edge.mmoserver.entities.smartfox.GameZone;
import org.asf.edge.mmoserver.services.ZoneManager;
import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Zone creation event - called when zones are created
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("zones.create")
public class ZoneCreatedEvent extends EventObject {

	private GameZone zone;
	private ZoneManager zoneManager;

	@Override
	public String eventPath() {
		return "zones.create";
	}

	public ZoneCreatedEvent(ZoneManager zoneManager, GameZone zone) {
		this.zoneManager = zoneManager;
		this.zone = zone;
	}

	/**
	 * Retrieves the game zone
	 * 
	 * @return GameZone instance
	 */
	public GameZone getZone() {
		return zone;
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
