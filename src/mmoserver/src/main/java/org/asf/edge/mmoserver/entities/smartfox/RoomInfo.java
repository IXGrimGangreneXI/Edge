package org.asf.edge.mmoserver.entities.smartfox;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.permissions.PermissionContext;
import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.mmoserver.events.players.PlayerRoomJoinEvent;
import org.asf.edge.mmoserver.events.players.PlayerRoomJoinSpectatorEvent;
import org.asf.edge.mmoserver.events.players.PlayerRoomLeaveEvent;
import org.asf.edge.mmoserver.events.players.PlayerRoomLeaveSpectatorEvent;
import org.asf.edge.mmoserver.events.sync.SfsUserCreatedEvent;
import org.asf.edge.mmoserver.events.sync.SfsUserDeletedEvent;
import org.asf.edge.mmoserver.events.variables.RoomVariableAddedEvent;
import org.asf.edge.mmoserver.events.variables.RoomVariableRemovedEvent;
import org.asf.edge.mmoserver.io.SequenceWriter;
import org.asf.edge.mmoserver.services.ZoneManager;
import org.asf.nexus.events.EventBus;

/**
 * 
 * Room information container
 * 
 * @author Sky Swimmer
 * 
 */
public class RoomInfo {

	private Logger logger = LogManager.getLogger("ZoneManager");

	private int roomID;
	private String roomName;
	private RoomGroup group;

	private boolean isGame;
	private boolean isHidden;

	private boolean isPasswordProtected;

	private ArrayList<PlayerInfo> players = new ArrayList<PlayerInfo>();
	private HashMap<String, SfsUser> playerSfsObjs = new HashMap<String, SfsUser>();
	private short maxUsers;

	private HashMap<String, RoomVariable> variables = new HashMap<String, RoomVariable>();

	private ArrayList<PlayerInfo> spectators = new ArrayList<PlayerInfo>();
	private short maxSpectators;

	private HashMap<String, Object> memory = new HashMap<String, Object>();

	void update(int roomID, RoomGroup group) {
		this.roomID = roomID;
		this.group = group;
	}

	public RoomInfo(int roomID, String name, RoomGroup group, boolean isGame, boolean isHidden,
			boolean isPasswordProtected, short maxUsers, RoomVariable[] variables, short maxSpectators) {
		this.roomID = roomID;
		this.roomName = name;
		this.group = group;
		this.isGame = isGame;
		this.isHidden = isHidden;
		this.isPasswordProtected = isPasswordProtected;
		this.maxUsers = maxUsers;
		for (RoomVariable var : variables) {
			var.room = this;
			this.variables.put(var.getName(), var);
		}
		this.maxSpectators = maxSpectators;
	}

	/**
	 * Retrieves all SFS user objects
	 * 
	 * @return Array of SfsUser instances
	 */
	public SfsUser[] getSfsUserObjects() {
		synchronized (playerSfsObjs) {
			return playerSfsObjs.values().toArray(t -> new SfsUser[t]);
		}
	}

	/**
	 * Retrieves SFS user objects by ID
	 * 
	 * @param id User ID
	 * @return SfsUser instance or null
	 */
	public SfsUser getSfsUser(String id) {
		synchronized (playerSfsObjs) {
			return playerSfsObjs.get(id);
		}
	}

	/**
	 * Removes SFS user objects by ID
	 * 
	 * @param id User ID
	 */
	public void removeSfsUser(String id) {
		synchronized (playerSfsObjs) {
			if (playerSfsObjs.containsKey(id)) {
				// Remove
				SfsUser inst = playerSfsObjs.remove(id);

				// Sync
				EventBus.getInstance().dispatchEvent(new SfsUserDeletedEvent(inst, this));
			}
		}
	}

	/**
	 * Adds SFS user objects
	 * 
	 * @param user User object to add
	 */
	public void addSfsUser(SfsUser user) {
		synchronized (playerSfsObjs) {
			if (!playerSfsObjs.containsKey(user.getUserID())) {
				// Update
				user.update(this, user.getPlayerIndex());

				// Save
				playerSfsObjs.put(user.getUserID(), user);

				// Sync
				EventBus.getInstance().dispatchEvent(new SfsUserCreatedEvent(user, this));
			} else {
				// Update settings
				playerSfsObjs.get(user.getUserID()).update(user.getUserNumericID(), user.getUserID(), this,
						user.getPrivilegeID(), user.getPlayerIndex());
			}
		}
	}

	/**
	 * Retrieves all variable names
	 * 
	 * @return Array of variable names
	 */
	public String[] getVariableNames() {
		synchronized (variables) {
			return variables.keySet().toArray(t -> new String[t]);
		}
	}

	/**
	 * Retrieves all room variables
	 * 
	 * @return Array of RoomVariable instances
	 */
	public RoomVariable[] getVariables() {
		synchronized (variables) {
			return variables.values().toArray(t -> new RoomVariable[t]);
		}
	}

	/**
	 * Retrieves variables by name
	 * 
	 * @param name Variable name
	 * @return RoomVariable instance or null
	 */
	public RoomVariable getVariable(String name) {
		synchronized (variables) {
			return variables.get(name);
		}
	}

	/**
	 * Removes variables
	 * 
	 * @param var Variable to remove
	 */
	public void removeVariable(RoomVariable var) {
		synchronized (variables) {
			if (variables.containsKey(var.getName())) {
				// Remove
				variables.remove(var.getName());

				// Dispatch event
				EventBus.getInstance().dispatchEvent(new RoomVariableRemovedEvent(ZoneManager.getInstance(),
						getGroup().getZone(), getGroup(), this, var));
			}
		}
	}

	/**
	 * Adds variables
	 * 
	 * @param name  Variable name
	 * @param value Variable value
	 * @return RoomVariable instance
	 */
	public RoomVariable addVariable(String name, Object value) {
		return addVariable(name, null, value, false);
	}

	/**
	 * Adds variables
	 * 
	 * @param name  Variable name
	 * @param type  Variable type
	 * @param value Variable value
	 * @return RoomVariable instance
	 */
	public RoomVariable addVariable(String name, VariableType type, Object value) {
		return addVariable(name, type, value, false);
	}

	/**
	 * Adds variables
	 * 
	 * @param name      Variable name
	 * @param type      Variable type
	 * @param value     Variable value
	 * @param isPrivate True to mark the variable as private, false otherwise
	 * @return RoomVariable instance
	 */
	public RoomVariable addVariable(String name, VariableType type, Object value, boolean isPrivate) {
		return addVariable(name, type, value, isPrivate, true);
	}

	/**
	 * Adds dynamic variables
	 * 
	 * @param name                 Variable name
	 * @param dynamicAssignmentKey Variable dynamic assignment key
	 * @return RoomVariable instance
	 */
	public RoomVariable addDynamicVariable(String name, String dynamicAssignmentKey) {
		return addDynamicVariable(name, dynamicAssignmentKey, false);
	}

	/**
	 * Adds dynamic variables
	 * 
	 * @param name                 Variable name
	 * @param dynamicAssignmentKey Variable dynamic assignment key
	 * @param isPrivate            True to mark the variable as private, false
	 *                             otherwise
	 * @return RoomVariable instance
	 */
	public RoomVariable addDynamicVariable(String name, String dynamicAssignmentKey, boolean isPrivate) {
		return addDynamicVariable(name, dynamicAssignmentKey, isPrivate, true);
	}

	/**
	 * Adds dynamic variables
	 * 
	 * @param name                 Variable name
	 * @param dynamicAssignmentKey Variable dynamic assignment key
	 * @param isPrivate            True to mark the variable as private, false
	 *                             otherwise
	 * @param isPersistent         True to mark the variable as persistent, false
	 *                             otherwise
	 * @return RoomVariable instance
	 */
	public RoomVariable addDynamicVariable(String name, String dynamicAssignmentKey, boolean isPrivate,
			boolean isPersistent) {
		synchronized (variables) {
			if (variables.containsKey(name))
				throw new IllegalArgumentException("Variable '" + name + "' is already present");

			// Create
			RoomVariable var = new RoomVariable(this, name, dynamicAssignmentKey, isPrivate, isPersistent);

			// Add
			variables.put(name, var);

			// Dispatch event
			EventBus.getInstance().dispatchEvent(
					new RoomVariableAddedEvent(ZoneManager.getInstance(), getGroup().getZone(), getGroup(), this, var));

			// Return
			return var;
		}
	}

	/**
	 * Adds variables
	 * 
	 * @param name         Variable name
	 * @param type         Variable type
	 * @param value        Variable value
	 * @param isPrivate    True to mark the variable as private, false otherwise
	 * @param isPersistent True to mark the variable as persistent, false otherwise
	 * @return RoomVariable instance
	 */
	public RoomVariable addVariable(String name, VariableType type, Object value, boolean isPrivate,
			boolean isPersistent) {
		synchronized (variables) {
			if (variables.containsKey(name))
				throw new IllegalArgumentException("Variable '" + name + "' is already present");

			// Create
			RoomVariable var = new RoomVariable(this, name, type, value, isPrivate, isPersistent);

			// Add
			variables.put(name, var);

			// Dispatch event
			EventBus.getInstance().dispatchEvent(
					new RoomVariableAddedEvent(ZoneManager.getInstance(), getGroup().getZone(), getGroup(), this, var));

			// Return
			return var;
		}
	}

	// TODO: editing the room and sending said edits to client

	/**
	 * Retrieves all joined players
	 * 
	 * @return Array of PlayerInfo instances
	 */
	public PlayerInfo[] getPlayers() {
		synchronized (players) {
			return players.toArray(t -> new PlayerInfo[t]);
		}
	}

	/**
	 * Checks if specific players are in the room
	 * 
	 * @param player Player to check
	 * @return True if joined, false otherwise
	 */
	public boolean hasPlayer(PlayerInfo player) {
		return hasPlayer(player.getSave().getSaveID());
	}

	/**
	 * Checks if specific players are in the room
	 * 
	 * @param playerID Player ID to check
	 * @return True if joined, false otherwise
	 */
	public boolean hasPlayer(String playerID) {
		synchronized (players) {
			return players.stream().anyMatch(t -> t.getSave().getSaveID().equals(playerID));
		}
	}

	/**
	 * Retrieves all joined spectating players
	 * 
	 * @return Array of PlayerInfo instances
	 */
	public PlayerInfo[] getSpectatorPlayers() {
		synchronized (spectators) {
			return spectators.toArray(t -> new PlayerInfo[t]);
		}
	}

	/**
	 * Checks if specific spectating players are in the room
	 * 
	 * @param player Player to check
	 * @return True if joined, false otherwise
	 */
	public boolean hasSpectatorPlayer(PlayerInfo player) {
		return hasSpectatorPlayer(player.getSave().getSaveID());
	}

	/**
	 * Checks if specific spectating players are in the room
	 * 
	 * @param playerID Player ID to check
	 * @return True if joined, false otherwise
	 */
	public boolean hasSpectatorPlayer(String playerID) {
		synchronized (spectators) {
			return spectators.stream().anyMatch(t -> t.getSave().getSaveID().equals(playerID));
		}
	}

	/**
	 * Adds players to the room
	 * 
	 * @param player Player to add to the room
	 */
	public void addPlayer(PlayerInfo player) {
		synchronized (players) {
			// Check joined
			if (players.stream().anyMatch(t -> t.getSave().getSaveID().equals(player.getSave().getSaveID())))
				return;

			// Subscribe to group
			if (!getGroup().isPlayerSubscribed(player))
				player.subscribeToGroup(group);

			// Join
			logger.info("Player " + player.getSave().getUsername() + " (" + player.getSave().getSaveID()
					+ ") joined room " + getName());
			players.add(player);

			// Check permissions
			short priv = 1;
			if (player.getAccount().isGuestAccount())
				priv = 0;
			switch (PermissionContext.getFor(player.getAccount()).getPermissionLevel()) {
			case OPERATOR:
				priv = 3;
				break;
			case DEVELOPER:
				priv = 3;
				break;
			case ADMINISTRATOR:
				priv = 3;
				break;
			case MODERATOR:
				priv = 2;
				break;
			case TRIAL_MODERATOR:
				priv = 2;
				break;
			case PLAYER:
				priv = 1;
				break;
			case GUEST:
				priv = 0;
				break;
			}

			// Create SFS user
			addSfsUser(new SfsUser(player.getClient().getSessionNumericID(), player.getSave().getSaveID(), priv) {
				{
					setObject(PlayerInfo.class, player);
				}
			});

			// Dispatch event
			EventBus.getInstance().dispatchEvent(new PlayerRoomJoinEvent(player, this));
		}
	}

	/**
	 * Removes players from the room
	 * 
	 * @param player Player to remove from the room
	 */
	public void removePlayer(PlayerInfo player) {
		synchronized (players) {
			// Check joined
			if (!players.stream().anyMatch(t -> t.getSave().getSaveID().equals(player.getSave().getSaveID())))
				return;

			// Leave
			logger.info("Player " + player.getSave().getUsername() + " (" + player.getSave().getSaveID()
					+ ") left room " + getName());
			players.remove(player);

			// Remove object
			removeSfsUser(player.getSave().getSaveID());

			// Dispatch event
			EventBus.getInstance().dispatchEvent(new PlayerRoomLeaveEvent(player, this));
		}
	}

	/**
	 * Adds spectators to the room
	 * 
	 * @param player Player to add to the room
	 */
	public void addSpectatorPlayer(PlayerInfo player) {
		synchronized (spectators) {
			// Check joined
			if (spectators.stream().anyMatch(t -> t.getSave().getSaveID().equals(player.getSave().getSaveID())))
				return;

			// Subscribe to group
			if (!getGroup().isPlayerSubscribed(player))
				player.subscribeToGroup(group);

			// Join
			logger.info("Spectating player " + player.getSave().getUsername() + " (" + player.getSave().getSaveID()
					+ ") joined room " + getName());
			spectators.add(player);

			// Check permissions
			short priv = 1;
			if (player.getAccount().isGuestAccount())
				priv = 0;
			switch (PermissionContext.getFor(player.getAccount()).getPermissionLevel()) {
			case OPERATOR:
				priv = 3;
				break;
			case DEVELOPER:
				priv = 3;
				break;
			case ADMINISTRATOR:
				priv = 3;
				break;
			case MODERATOR:
				priv = 2;
				break;
			case TRIAL_MODERATOR:
				priv = 2;
				break;
			case PLAYER:
				priv = 1;
				break;
			case GUEST:
				priv = 0;
				break;
			}

			// Create SFS user
			addSfsUser(new SfsUser(player.getClient().getSessionNumericID(), player.getSave().getSaveID(), priv, 0) {
				{
					setObject(PlayerInfo.class, player);
				}
			});

			// Dispatch event
			EventBus.getInstance().dispatchEvent(new PlayerRoomJoinSpectatorEvent(player, this));
		}
	}

	/**
	 * Removes spectators from the room
	 * 
	 * @param player Player to remove from the room
	 */
	public void removeSpectatorPlayer(PlayerInfo player) {
		synchronized (spectators) {
			// Check joined
			if (!spectators.stream().anyMatch(t -> t.getSave().getSaveID().equals(player.getSave().getSaveID())))
				return;

			// Leave
			logger.info("Spectating player " + player.getSave().getUsername() + " (" + player.getSave().getSaveID()
					+ ") left room " + getName());
			spectators.remove(player);

			// Remove object
			removeSfsUser(player.getSave().getSaveID());

			// Dispatch event
			EventBus.getInstance().dispatchEvent(new PlayerRoomLeaveSpectatorEvent(player, this));
		}
	}

	/**
	 * Retrieves the room spectator count
	 * 
	 * @return Room spectator count
	 */
	public short getSpectatorCount() {
		synchronized (playerSfsObjs) {
			return (short) playerSfsObjs.values().stream().filter(t -> t.getPlayerIndex() <= 0).count();
		}
	}

	/**
	 * Retrieves the room spectator limit
	 * 
	 * @return Room spectator limit
	 */
	public short getSpectatorLimit() {
		return maxSpectators;
	}

	/**
	 * Retrieves the room user count
	 * 
	 * @return Room user count
	 */
	public short getUserCount() {
		synchronized (playerSfsObjs) {
			return (short) playerSfsObjs.values().stream().filter(t -> t.getPlayerIndex() > 0).count();
		}
	}

	/**
	 * Retrieves the room user limit
	 * 
	 * @return Room user limit
	 */
	public short getUserLimit() {
		return maxUsers;
	}

	/**
	 * Checks if the room is a game room
	 * 
	 * @return True if the room is a game room, false otherwise
	 */
	public boolean isGame() {
		return isGame;
	}

	/**
	 * Checks if the room is a hidden
	 * 
	 * @return True if the room is hidden, false otherwise
	 */
	public boolean isHidden() {
		return isHidden;
	}

	/**
	 * Checks if the room is a password-protected
	 * 
	 * @return True if the room is password-protected, false otherwise
	 */
	public boolean isPasswordProtected() {
		return isPasswordProtected;
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
	 * Retrieves the room name
	 * 
	 * @return Room name
	 */
	public String getName() {
		return roomName;
	}

	/**
	 * Retrieves the room numeric ID
	 * 
	 * @return Room ID
	 */
	public int getRoomID() {
		return roomID;
	}

	/**
	 * Writes the room object to a SequenceWriter instance
	 * 
	 * @param writer             SequenceWriter instance
	 * @param includePrivateVars True to include private variables, false otherwise
	 */
	public void writeTo(SequenceWriter writer, boolean includePrivateVars) {
		// Write headers
		writer.writeInt(roomID);
		writer.writeString(roomName);
		writer.writeString(group.getName());

		// Write properties
		writer.writeBoolean(isGame);
		writer.writeBoolean(isHidden);
		writer.writeBoolean(isPasswordProtected);

		// Write users
		writer.writeShort(getUserCount());
		writer.writeShort(maxUsers);

		// Write variables
		int i = 0;
		Object[] varsO = new Object[variables.size()];
		for (RoomVariable var : variables.values()) {
			// Check
			if (!includePrivateVars && var.isPrivate())
				continue;

			// Write variable
			varsO[i++] = var.toObjectArray();
		}

		// Write variables
		writer.writeObjectArray(varsO);

		// Write spectators
		if (isGame) {
			writer.writeShort(getSpectatorCount());
			writer.writeShort(maxSpectators);
		}
	}

	/**
	 * Retrieves session memory objects
	 * 
	 * @param <T>  Object type
	 * @param type Object class
	 * @return Object instance or null
	 */
	@SuppressWarnings("unchecked")
	public <T> T getObject(Class<T> type) {
		return (T) memory.get(type.getTypeName());
	}

	/**
	 * Stores session memory objects
	 * 
	 * @param <T>    Object type
	 * @param type   Object class
	 * @param object Object instance
	 */
	public <T> void setObject(Class<T> type, T object) {
		memory.put(type.getTypeName(), object);
	}

	/**
	 * Removes session memory objects
	 * 
	 * @param <T>  Object type
	 * @param type Object class
	 */
	public <T> void removeObject(Class<T> type) {
		memory.remove(type.getTypeName());
	}

}
