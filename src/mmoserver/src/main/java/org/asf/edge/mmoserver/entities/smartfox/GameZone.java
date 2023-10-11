package org.asf.edge.mmoserver.entities.smartfox;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.mmoserver.events.zones.RoomGroupCreatedEvent;
import org.asf.edge.mmoserver.events.zones.RoomGroupDeletedEvent;
import org.asf.edge.mmoserver.services.ZoneManager;
import org.asf.edge.modules.eventbus.EventBus;

/**
 * 
 * Game Zone Information Container
 * 
 * @author Sky Swimmer
 * 
 */
public class GameZone {

	private String zoneName;
	private boolean zoneActive = true;
	private HashMap<String, RoomGroup> roomGroups = new HashMap<String, RoomGroup>();
	private Logger logger = LogManager.getLogger("ZoneManager");
	HashMap<Integer, RoomInfo> rooms = new HashMap<Integer, RoomInfo>();
	static int currentRoomIdGlobal;

	public GameZone(String name) {
		this.zoneName = name;
	}

	public GameZone(String name, boolean active) {
		this.zoneName = name;
		this.zoneActive = active;
	}

	public GameZone(String name, RoomGroup[] roomGroups) {
		this.zoneName = name;
		for (RoomGroup g : roomGroups)
			this.roomGroups.put(g.getName(), g);
	}

	public GameZone(String name, boolean active, RoomGroup[] roomGroups) {
		this.zoneName = name;
		this.zoneActive = active;
		for (RoomGroup g : roomGroups)
			this.roomGroups.put(g.getName(), g);
	}

	/**
	 * Retrieves the zone name
	 * 
	 * @return Zone name string
	 */
	public String getName() {
		return zoneName;
	}

	/**
	 * Checks if the zone is active
	 * 
	 * @return True if active, false otherwise
	 */
	public boolean isActive() {
		return zoneActive;
	}

	/**
	 * Activates the zone
	 */
	public void activate() {
		zoneActive = true;
	}

	/**
	 * Deactivates the zone
	 */
	public void deactivate() {
		zoneActive = false;
	}

	/**
	 * Retrieves all room names
	 * 
	 * @return Array of group names
	 */
	public String[] getAllRoomNames() {
		synchronized (rooms) {
			return rooms.values().stream().map(t -> t.getName()).toArray(t -> new String[t]);
		}
	}

	/**
	 * Retrieves all rooms in the zone
	 * 
	 * @return Array of RoomInfo instances
	 */
	public RoomInfo[] getAllRooms() {
		synchronized (rooms) {
			return rooms.values().toArray(t -> new RoomInfo[t]);
		}
	}

	/**
	 * Retrieves rooms by ID
	 * 
	 * @param id Room ID
	 * @return RoomInfo instance or null
	 */
	public RoomInfo getRoomByID(int id) {
		synchronized (rooms) {
			return rooms.get(id);
		}
	}

	/**
	 * Retrieves all room group names
	 * 
	 * @return Array of group names
	 */
	public String[] getRoomGroupNames() {
		synchronized (roomGroups) {
			return roomGroups.values().stream().map(t -> t.getName()).toArray(t -> new String[t]);
		}
	}

	/**
	 * Retrieves all room groups in the zone
	 * 
	 * @return Array of RoomGroup instances
	 */
	public RoomGroup[] getRoomGroups() {
		synchronized (roomGroups) {
			return roomGroups.values().toArray(t -> new RoomGroup[t]);
		}
	}

	/**
	 * Removes room groups
	 * 
	 * @param group Room group to remove
	 */
	public void removeRoomGroup(RoomGroup group) {
		synchronized (roomGroups) {
			if (roomGroups.containsKey(group.getName())) {
				// Log
				logger.info("Removing group " + group.getName() + " from zone " + zoneName + "...");

				// Desub all players
				for (PlayerInfo player : group.getSubscribedPlayers())
					group.desubscribePlayer(player);

				// Remove all rooms in group
				for (RoomInfo room : group.getRooms())
					group.removeRoom(room);

				// Remove
				roomGroups.remove(group.getName());

				// Dispatch event
				EventBus.getInstance().dispatchEvent(new RoomGroupDeletedEvent(ZoneManager.getInstance(), this, group));
			}
		}
	}

	/**
	 * Adds room groups
	 * 
	 * @param name Group name
	 * @throws IllegalArgumentException If the group already exists
	 */
	public RoomGroup addRoomGroup(String name) throws IllegalArgumentException {
		return addRoomGroup(name, new RoomInfo[0]);
	}

	/**
	 * Adds room groups
	 * 
	 * @param name  Group name
	 * @param rooms Rooms in the group
	 * @throws IllegalArgumentException If the group already exists
	 */
	public RoomGroup addRoomGroup(String name, RoomInfo[] rooms) throws IllegalArgumentException {
		return addRoomGroup(new RoomGroup(name, this, rooms));
	}

	/**
	 * Adds room groups
	 * 
	 * @param group Room group to add
	 * @throws IllegalArgumentException If the group already exists
	 */
	public RoomGroup addRoomGroup(RoomGroup group) throws IllegalArgumentException {
		synchronized (roomGroups) {
			if (roomGroups.containsKey(group.getName()))
				throw new IllegalArgumentException("Group already exists: " + group.getName());

			// Log
			logger.info("Creating group " + group.getName() + " in zone " + zoneName + "...");

			// Add
			roomGroups.put(group.getName(), group);

			// Dispatch event
			EventBus.getInstance().dispatchEvent(new RoomGroupCreatedEvent(ZoneManager.getInstance(), this, group));

			// Return
			return group;
		}
	}

	/**
	 * Retrieves room groups by name
	 * 
	 * @param name Group name
	 * @return RoomGroup instance or null
	 */
	public RoomGroup getRoomGroup(String name) {
		synchronized (roomGroups) {
			return roomGroups.get(name);
		}
	}

}
