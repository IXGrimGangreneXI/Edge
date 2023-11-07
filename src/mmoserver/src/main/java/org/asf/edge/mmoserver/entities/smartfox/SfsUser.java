package org.asf.edge.mmoserver.entities.smartfox;

import java.util.HashMap;

import org.asf.edge.mmoserver.events.variables.UserVariableAddedEvent;
import org.asf.edge.mmoserver.events.variables.UserVariableRemovedEvent;
import org.asf.edge.mmoserver.io.SequenceReader;
import org.asf.edge.mmoserver.io.SequenceWriter;
import org.asf.nexus.events.EventBus;

public class SfsUser {

	private RoomInfo room;

	private int userNumericID;
	private String userID;

	private int privilegeID;
	private int playerIndex;

	private HashMap<String, UserVariable> variables = new HashMap<String, UserVariable>();

	private HashMap<String, Object> memory = new HashMap<String, Object>();

	public SfsUser(RoomInfo room, int userNumericID, String userID, int privilegeID, int playerIndex,
			UserVariable... variables) {
		this.room = room;
		this.userNumericID = userNumericID;
		this.userID = userID;
		this.privilegeID = privilegeID;
		this.playerIndex = playerIndex;
		for (UserVariable var : variables) {
			var.user = this;
			this.variables.put(var.getName(), var);
		}
	}

	public SfsUser(int userNumericID, String userID, int privilegeID, int playerIndex, UserVariable... variables) {
		this.userNumericID = userNumericID;
		this.userID = userID;
		this.privilegeID = privilegeID;
		this.playerIndex = playerIndex;
		for (UserVariable var : variables) {
			var.user = this;
			this.variables.put(var.getName(), var);
		}
	}

	public SfsUser(int userNumericID, String userID, int privilegeID, UserVariable... variables) {
		this.userNumericID = userNumericID;
		this.userID = userID;
		this.privilegeID = privilegeID;
		this.playerIndex = userNumericID;
		for (UserVariable var : variables) {
			var.user = this;
			this.variables.put(var.getName(), var);
		}
	}

	public SfsUser(RoomInfo room, int userNumericID, String userID, int privilegeID, UserVariable... variables) {
		this.room = room;
		this.userNumericID = userNumericID;
		this.userID = userID;
		this.privilegeID = privilegeID;
		this.playerIndex = userNumericID;
		for (UserVariable var : variables) {
			var.user = this;
			this.variables.put(var.getName(), var);
		}
	}

	void update(RoomInfo room, int playerIndex) {
		this.playerIndex = playerIndex;
		this.room = room;
	}

	void update(int userNumericID, String userID, RoomInfo room, int privilegeID, int playerIndex) {
		this.userNumericID = userNumericID;
		this.privilegeID = privilegeID;
		this.playerIndex = playerIndex;
		this.userID = userID;
		this.room = room;
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

	/**
	 * Retrieves the room instance
	 * 
	 * @return RoomInfo instance
	 */
	public RoomInfo getRoom() {
		return room;
	}

	/**
	 * Retrieves the user numeric ID
	 * 
	 * @return User numeric ID
	 */
	public int getUserNumericID() {
		return userNumericID;
	}

	/**
	 * Retrieves the user ID
	 * 
	 * @return User ID string
	 */
	public String getUserID() {
		return userID;
	}

	/**
	 * Retrieves the user privilege ID
	 * 
	 * @return User privilege ID
	 */
	public int getPrivilegeID() {
		return privilegeID;
	}

	/**
	 * Retrieves the player index
	 * 
	 * @return Player index integer
	 */
	public int getPlayerIndex() {
		return playerIndex;
	}

	/**
	 * Retrieves all user variables
	 * 
	 * @return Array of UserVariable instances
	 */
	public UserVariable[] getVariables() {
		synchronized (variables) {
			return variables.values().toArray(t -> new UserVariable[t]);
		}
	}

	/**
	 * Retrieves variables by name
	 * 
	 * @param name Variable name
	 * @return UserVariable instance or null
	 */
	public UserVariable getVariable(String name) {
		synchronized (variables) {
			return variables.get(name);
		}
	}

	/**
	 * Removes variables
	 * 
	 * @param var Variable to remove
	 */
	public void removeVariable(UserVariable var) {
		synchronized (variables) {
			if (variables.containsKey(var.getName())) {
				// Remove
				variables.remove(var.getName());

				// Dispatch event
				EventBus.getInstance().dispatchEvent(new UserVariableRemovedEvent(this, var));
			}
		}
	}

	/**
	 * Adds variables
	 * 
	 * @param name  Variable name
	 * @param value Variable value
	 * @return UserVariable instance
	 */
	public UserVariable addVariable(String name, Object value) {
		return addVariable(name, null, value);
	}

	/**
	 * Adds variables
	 * 
	 * @param name  Variable name
	 * @param type  Variable type
	 * @param value Variable value
	 * @return UserVariable instance
	 */
	public UserVariable addVariable(String name, VariableType type, Object value) {
		synchronized (variables) {
			if (variables.containsKey(name))
				throw new IllegalArgumentException("Variable '" + name + "' is already present");

			// Create
			UserVariable var = new UserVariable(this, name, type, value);

			// Add
			variables.put(name, var);

			// Dispatch event
			EventBus.getInstance().dispatchEvent(new UserVariableAddedEvent(this, var));

			// Return
			return var;
		}
	}

	/**
	 * Writes the user object to a SequenceWriter instance
	 * 
	 * @param writer SequenceWriter instance
	 */
	public void writeTo(SequenceWriter writer) {
		// Write headers
		writer.writeInt(userNumericID);
		writer.writeString(userID);

		// Write properties
		writer.writeShort((short) privilegeID);
		writer.writeShort((short) playerIndex);

		// Write variables
		int i = 0;
		Object[] varsO = new Object[variables.size()];
		for (UserVariable var : variables.values()) {
			// Write variable
			varsO[i++] = var.toObjectArray();
		}

		// Write variables
		writer.writeObjectArray(varsO);
	}

	/**
	 * Reads a SfsUser object from a sequence reader
	 * 
	 * @param reader Reader to read from
	 * @return SfsUser instance
	 */
	public static SfsUser readFrom(SequenceReader reader) {
		// Read headers
		int id = reader.readInt();
		String userID = reader.readString();

		// Read properties
		int privilegeID = reader.readShort();
		int playerIndex = reader.readShort();

		// Read variables
		int i = 0;
		Object[] vars = reader.readObjectArray();
		UserVariable[] v = new UserVariable[vars.length];
		for (Object o : vars) {
			v[i++] = UserVariable.parseObjectArray((Object[]) o);
		}

		// Return
		return new SfsUser(null, id, userID, privilegeID, playerIndex, v);
	}

}
