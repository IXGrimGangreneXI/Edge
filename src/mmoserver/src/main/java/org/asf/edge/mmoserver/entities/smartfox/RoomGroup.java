package org.asf.edge.mmoserver.entities.smartfox;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.mmoserver.events.players.PlayerRoomGroupDesubscribeEvent;
import org.asf.edge.mmoserver.events.players.PlayerRoomGroupSubscribeEvent;
import org.asf.edge.mmoserver.events.zones.RoomCreatedEvent;
import org.asf.edge.mmoserver.events.zones.RoomDeletedEvent;
import org.asf.edge.mmoserver.services.ZoneManager;
import org.asf.edge.modules.eventbus.EventBus;

/**
 * 
 * Room group information container
 * 
 * @author Sky Swimmer
 * 
 */
public class RoomGroup {

	private String name;
	private GameZone zone;
	private HashMap<String, RoomInfo> rooms = new HashMap<String, RoomInfo>();

	private Logger logger = LogManager.getLogger("ZoneManager");
	private ArrayList<PlayerInfo> subscribedPlayers = new ArrayList<PlayerInfo>();

	public RoomGroup(String name, GameZone zone) {
		this.name = name;
		this.zone = zone;
	}

	public RoomGroup(String name, GameZone zone, RoomInfo[] rooms) {
		this.name = name;
		this.zone = zone;
		for (RoomInfo r : rooms) {
			this.rooms.put(r.getName(), r);

			// Create room ID and register
			synchronized (zone.rooms) {
				r.update(GameZone.currentRoomIdGlobal++, this);
				zone.rooms.put(r.getRoomID(), r);
			}
		}
	}

	/**
	 * Retrieves the group name
	 * 
	 * @return Group name string
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves the zone this group is part of
	 * 
	 * @return GameZone instance
	 */
	public GameZone getZone() {
		return zone;
	}

	/**
	 * Retrieves all room names
	 * 
	 * @return Array of room names
	 */
	public String[] getRoomNames() {
		synchronized (rooms) {
			return rooms.values().stream().map(t -> t.getName()).toArray(t -> new String[t]);
		}
	}

	/**
	 * Retrieves all rooms in the group
	 * 
	 * @return Array of RoomInfo instances
	 */
	public RoomInfo[] getRooms() {
		synchronized (rooms) {
			return rooms.values().toArray(t -> new RoomInfo[t]);
		}
	}

	/**
	 * Removes rooms
	 * 
	 * @param room Room to remove
	 */
	public void removeRoom(RoomInfo room) {
		synchronized (rooms) {
			if (rooms.containsKey(room.getName())) {
				// Log
				logger.info("Removing room " + room.getName() + " from group " + name + " of zone " + zone.getName()
						+ "...");

				// Remove
				rooms.remove(room.getName());

				// Dispatch event
				EventBus.getInstance().dispatchEvent(new RoomDeletedEvent(ZoneManager.getInstance(), zone, this, room));
			}
		}
	}

	/**
	 * Adds rooms
	 * 
	 * @param name Room name
	 * @throws IllegalArgumentException If the room already exists
	 */
	public RoomInfo addRoom(String name) throws IllegalArgumentException {
		return addRoom(name, true);
	}

	/**
	 * Adds rooms
	 * 
	 * @param name   Room name
	 * @param isGame True if the room is a game room, false otherwise
	 * @throws IllegalArgumentException If the room already exists
	 */
	public RoomInfo addRoom(String name, boolean isGame) throws IllegalArgumentException {
		return addRoom(name, isGame, false);
	}

	/**
	 * Adds rooms
	 * 
	 * @param name     Room name
	 * @param isGame   True if the room is a game room, false otherwise
	 * @param isHidden True if the room is hidden, false otherwise
	 * @throws IllegalArgumentException If the room already exists
	 */
	public RoomInfo addRoom(String name, boolean isGame, boolean isHidden) throws IllegalArgumentException {
		return addRoom(name, isGame, isHidden, false);
	}

	/**
	 * Adds rooms
	 * 
	 * @param name                Room name
	 * @param isGame              True if the room is a game room, false otherwise
	 * @param isHidden            True if the room is hidden, false otherwise
	 * @param isPasswordProtected True if the room is password-protected, false
	 *                            otherwise
	 * @throws IllegalArgumentException If the room already exists
	 */
	public RoomInfo addRoom(String name, boolean isGame, boolean isHidden, boolean isPasswordProtected)
			throws IllegalArgumentException {
		return addRoom(name, isGame, isHidden, isPasswordProtected, (short) 10);
	}

	/**
	 * Adds rooms
	 * 
	 * @param name                Room name
	 * @param isGame              True if the room is a game room, false otherwise
	 * @param isHidden            True if the room is hidden, false otherwise
	 * @param isPasswordProtected True if the room is password-protected, false
	 *                            otherwise
	 * @param maxUsers            Max user count of room (-1 to disable)
	 * @throws IllegalArgumentException If the room already exists
	 */
	public RoomInfo addRoom(String name, boolean isGame, boolean isHidden, boolean isPasswordProtected, short maxUsers)
			throws IllegalArgumentException {
		return addRoom(name, isGame, isHidden, isPasswordProtected, maxUsers, new RoomVariable[0]);
	}

	/**
	 * Adds game rooms
	 * 
	 * @param name                Room name
	 * @param isHidden            True if the room is hidden, false otherwise
	 * @param isPasswordProtected True if the room is password-protected, false
	 *                            otherwise
	 * @param maxUsers            Max user count of room (-1 to disable)
	 * @param maxSpectators       Max spectator count
	 * @throws IllegalArgumentException If the room already exists
	 */
	public RoomInfo addGameRoom(String name, boolean isHidden, boolean isPasswordProtected, short maxUsers,
			short maxSpectators) throws IllegalArgumentException {
		return addRoom(name, true, isHidden, isPasswordProtected, maxUsers, new RoomVariable[0], maxSpectators);
	}

	/**
	 * Adds rooms
	 * 
	 * @param name                Room name
	 * @param isHidden            True if the room is hidden, false otherwise
	 * @param isPasswordProtected True if the room is password-protected, false
	 *                            otherwise
	 * @param maxUsers            Max user count of room (-1 to disable)
	 * @param variables           Array of room variables
	 * @param maxSpectators       Max spectator count
	 * @throws IllegalArgumentException If the room already exists
	 */
	public RoomInfo addGameRoom(String name, boolean isHidden, boolean isPasswordProtected, short maxUsers,
			RoomVariable[] variables, short maxSpectators) throws IllegalArgumentException {
		return addRoom(name, true, isHidden, isPasswordProtected, maxUsers, variables, (short) 0);
	}

	/**
	 * Adds rooms
	 * 
	 * @param name                Room name
	 * @param isGame              True if the room is a game room, false otherwise
	 * @param isHidden            True if the room is hidden, false otherwise
	 * @param isPasswordProtected True if the room is password-protected, false
	 *                            otherwise
	 * @param maxUsers            Max user count of room (-1 to disable)
	 * @param variables           Array of room variables
	 * @throws IllegalArgumentException If the room already exists
	 */
	public RoomInfo addRoom(String name, boolean isGame, boolean isHidden, boolean isPasswordProtected, short maxUsers,
			RoomVariable[] variables) throws IllegalArgumentException {
		return addRoom(name, isGame, isHidden, isPasswordProtected, maxUsers, variables, (short) 0);
	}

	/**
	 * Adds rooms
	 * 
	 * @param name                Room name
	 * @param isGame              True if the room is a game room, false otherwise
	 * @param isHidden            True if the room is hidden, false otherwise
	 * @param isPasswordProtected True if the room is password-protected, false
	 *                            otherwise
	 * @param maxUsers            Max user count of room (-1 to disable)
	 * @param variables           Array of room variables
	 * @param maxSpectators       Max spectator count
	 * @throws IllegalArgumentException If the room already exists
	 */
	public RoomInfo addRoom(String name, boolean isGame, boolean isHidden, boolean isPasswordProtected, short maxUsers,
			RoomVariable[] variables, short maxSpectators) throws IllegalArgumentException {
		synchronized (rooms) {
			if (rooms.containsKey(name))
				throw new IllegalArgumentException("Room already exists: " + name);

			// Log
			logger.info("Creating room " + name + " in group " + this.name + " of zone " + zone.getName() + "...");

			// Create room
			RoomInfo room;
			synchronized (zone.rooms) {
				// Create
				room = new RoomInfo(GameZone.currentRoomIdGlobal++, name, this, isGame, isHidden, isPasswordProtected,
						maxUsers, variables, maxSpectators);

				// Add
				rooms.put(name, room);
				zone.rooms.put(room.getRoomID(), room);
			}

			// Dispatch event
			EventBus.getInstance().dispatchEvent(new RoomCreatedEvent(ZoneManager.getInstance(), zone, this, room));

			// Return
			return room;
		}
	}

	/**
	 * Retrieves rooms by name
	 * 
	 * @param name Room name
	 * @return RoomInfo instance or null
	 */
	public RoomInfo getRoom(String name) {
		synchronized (rooms) {
			return rooms.get(name);
		}
	}

	/**
	 * Retrieves all subscribed players
	 * 
	 * @return Array of PlayerInfo instances
	 */
	public PlayerInfo[] getSubscribedPlayers() {
		synchronized (subscribedPlayers) {
			return subscribedPlayers.toArray(t -> new PlayerInfo[t]);
		}
	}

	/**
	 * Checks if specific players are subscribed to the room group
	 * 
	 * @param player Player to check
	 * @return True if subscribed, false otherwise
	 */
	public boolean isPlayerSubscribed(PlayerInfo player) {
		return isPlayerSubscribed(player.getSave().getSaveID());
	}

	/**
	 * Checks if specific players are subscribed to the room group
	 * 
	 * @param playerID Player ID to check
	 * @return True if subscribed, false otherwise
	 */
	public boolean isPlayerSubscribed(String playerID) {
		synchronized (subscribedPlayers) {
			return subscribedPlayers.stream().anyMatch(t -> t.getSave().getSaveID().equals(playerID));
		}
	}

	/**
	 * Subscribes players to the room group
	 * 
	 * @param player Player to subscribe to the group
	 */
	public void subscribePlayer(PlayerInfo player) {
		synchronized (subscribedPlayers) {
			// Check subscribed
			if (subscribedPlayers.stream().anyMatch(t -> t.getSave().getSaveID().equals(player.getSave().getSaveID())))
				return;

			// Subscribe
			logger.info("Player " + player.getSave().getUsername() + " (" + player.getSave().getSaveID()
					+ ") subscribed to room group " + getName());
			subscribedPlayers.add(player);

			// Dispatch event
			EventBus.getInstance().dispatchEvent(new PlayerRoomGroupSubscribeEvent(player, this));
		}
	}

	/**
	 * De-subscribes players from the room group
	 * 
	 * @param player Player to de-subscribe from the group
	 */
	public void desubscribePlayer(PlayerInfo player) {
		synchronized (subscribedPlayers) {
			// Check subscribed
			if (!subscribedPlayers.stream().anyMatch(t -> t.getSave().getSaveID().equals(player.getSave().getSaveID())))
				return;

			// De-subscribe
			logger.info("Player " + player.getSave().getUsername() + " (" + player.getSave().getSaveID()
					+ ") desubscribed from room group " + getName());
			subscribedPlayers.remove(player);

			// Dispatch event
			EventBus.getInstance().dispatchEvent(new PlayerRoomGroupDesubscribeEvent(player, this));

			// Leave all rooms
			for (RoomInfo room : getRooms()) {
				if (room.hasPlayer(player)) {
					room.removePlayer(player);
				}
				if (room.hasSpectatorPlayer(player)) {
					room.removeSpectatorPlayer(player);
				}
			}
		}
	}

}
