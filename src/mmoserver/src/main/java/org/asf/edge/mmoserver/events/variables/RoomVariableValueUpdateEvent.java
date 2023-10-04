package org.asf.edge.mmoserver.events.variables;

import org.asf.edge.mmoserver.entities.smartfox.GameZone;
import org.asf.edge.mmoserver.entities.smartfox.RoomGroup;
import org.asf.edge.mmoserver.entities.smartfox.RoomInfo;
import org.asf.edge.mmoserver.entities.smartfox.RoomVariable;
import org.asf.edge.mmoserver.services.ZoneManager;
import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Room variable value update event - called when variable values are changed
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("rooms.variables.value.update")
public class RoomVariableValueUpdateEvent extends EventObject {

	private GameZone zone;
	private ZoneManager zoneManager;
	private RoomGroup group;
	private RoomInfo room;

	private RoomVariable variable;

	@Override
	public String eventPath() {
		return "rooms.variables.value.update";
	}

	public RoomVariableValueUpdateEvent(ZoneManager zoneManager, GameZone zone, RoomGroup group, RoomInfo room,
			RoomVariable variable) {
		this.zoneManager = zoneManager;
		this.room = room;
		this.zone = zone;
		this.group = group;
		this.variable = variable;
	}

	/**
	 * Retrieves the variable instance
	 * 
	 * @return RoomVariable instance
	 */
	public RoomVariable getVariable() {
		return variable;
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
