package org.asf.edge.mmoserver.entities.smartfox;

import java.util.HashMap;

import org.asf.edge.mmoserver.events.variables.RoomVariableAddedEvent;
import org.asf.edge.mmoserver.events.variables.RoomVariableRemovedEvent;
import org.asf.edge.mmoserver.io.SequenceWriter;
import org.asf.edge.mmoserver.services.ZoneManager;
import org.asf.edge.modules.eventbus.EventBus;

/**
 * 
 * Room information container
 * 
 * @author Sky Swimmer
 * 
 */
public class RoomInfo {

	private int roomID;
	private String roomName;
	private RoomGroup group;

	private boolean isGame;
	private boolean isHidden;

	private boolean isPasswordProtected;

	private short userCount;
	private short maxUsers;

	private HashMap<String, RoomVariable> variables = new HashMap<String, RoomVariable>();

	private short spectatorCount;
	private short maxSpectators;

	private HashMap<String, Object> memory = new HashMap<String, Object>();

	void update(int roomID, RoomGroup group) {
		this.roomID = roomID;
		this.group = group;
	}

	public RoomInfo(int roomID, String name, RoomGroup group, boolean isGame, boolean isHidden,
			boolean isPasswordProtected, short userCount, short maxUsers, RoomVariable[] variables,
			short spectatorCount, short maxSpectators) {
		this.roomID = roomID;
		this.roomName = name;
		this.group = group;
		this.isGame = isGame;
		this.isHidden = isHidden;
		this.isPasswordProtected = isPasswordProtected;
		this.userCount = userCount;
		this.maxUsers = maxUsers;
		for (RoomVariable var : variables) {
			var.room = this;
			this.variables.put(var.getName(), var);
		}
		this.spectatorCount = spectatorCount;
		this.maxSpectators = maxSpectators;
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

	// TODO: players in room
	// TODO: editing the room and sending said edits to client

	/**
	 * Retrieves the room spectator count
	 * 
	 * @return Room spectator count
	 */
	public short getSpectatorCount() {
		return spectatorCount;
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
		return userCount;
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
		writer.writeShort(userCount);
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
			writer.writeShort(spectatorCount);
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
