package org.asf.edge.mmoserver.events.zones;

import org.asf.edge.mmoserver.entities.smartfox.GameZone;
import org.asf.edge.mmoserver.entities.smartfox.RoomGroup;
import org.asf.edge.mmoserver.entities.smartfox.RoomInfo;
import org.asf.edge.mmoserver.services.ZoneManager;
import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Room deletion event - called when rooms are deleted
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("rooms.delete")
public class RoomDeletedEvent extends EventObject {

	private GameZone zone;
	private ZoneManager zoneManager;
	private RoomGroup group;
	private RoomInfo room;

	@Override
	public String eventPath() {
		return "rooms.delete";
	}

	public RoomDeletedEvent(ZoneManager zoneManager, GameZone zone, RoomGroup group, RoomInfo room) {
		this.zoneManager = zoneManager;
		this.zone = zone;
		this.group = group;
		this.room = room;
	}

	/**
	 * Retrieves the room
	 * 
	 * @return RoomInfo instance
	 */
	public RoomInfo getRoom() {
		return room;
	}

	/**
	 * Retrieves the room group
	 * 
	 * @return RoomGroup instance
	 */
	public RoomGroup getGroup() {
		return group;
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
