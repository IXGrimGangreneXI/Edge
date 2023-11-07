package org.asf.edge.mmoserver.events.zones;

import org.asf.edge.mmoserver.entities.smartfox.GameZone;
import org.asf.edge.mmoserver.services.ZoneManager;
import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Zone deletion event - called when zones are deleted
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("zones.delete")
public class ZoneDeletedEvent extends EventObject {

	private GameZone zone;
	private ZoneManager zoneManager;

	@Override
	public String eventPath() {
		return "zones.delete";
	}

	public ZoneDeletedEvent(ZoneManager zoneManager, GameZone zone) {
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
