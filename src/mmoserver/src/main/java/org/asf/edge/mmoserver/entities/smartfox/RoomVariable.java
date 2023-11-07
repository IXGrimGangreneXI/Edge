package org.asf.edge.mmoserver.entities.smartfox;

import org.asf.edge.mmoserver.events.variables.DynamicRoomVariableSetupEvent;
import org.asf.edge.mmoserver.events.variables.RoomVariableValueUpdateEvent;
import org.asf.edge.mmoserver.io.SequenceReader;
import org.asf.edge.mmoserver.io.SequenceWriter;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;
import org.asf.edge.mmoserver.services.ZoneManager;
import org.asf.nexus.events.EventBus;

public class RoomVariable {

	RoomInfo room;

	private boolean dynamic;
	private String dynamicVarKey;

	private String name;
	private VariableType type;
	private Object value;

	private boolean isPrivate;
	private boolean isPersistent;

	public RoomVariable(RoomInfo room, String name, VariableType type, Object value, boolean isPrivate,
			boolean isPersistent) {
		this.room = room;
		this.name = name;
		this.value = value;
		this.type = type;
		this.isPrivate = isPrivate;
		this.isPersistent = isPersistent;
	}

	public RoomVariable(RoomInfo room, String name, String dynamicVarKey, boolean isPrivate, boolean isPersistent) {
		this.room = room;
		this.name = name;
		this.dynamic = true;
		this.dynamicVarKey = dynamicVarKey;
		this.isPrivate = isPrivate;
		this.isPersistent = isPersistent;
	}

	/**
	 * Populates dynamic variables
	 */
	public void populate() {
		if (dynamic) {
			// Dispatch event
			EventBus.getInstance().dispatchEvent(new DynamicRoomVariableSetupEvent(ZoneManager.getInstance(),
					room.getGroup().getZone(), room.getGroup(), room, dynamicVarKey, this));
		}
	}

	/**
	 * Assigns the variable value
	 * 
	 * @param type  Variable type
	 * @param value Variable value
	 */
	public void setValue(VariableType type, Object value) {
		this.type = type;
		this.value = value;

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new RoomVariableValueUpdateEvent(ZoneManager.getInstance(),
				room.getGroup().getZone(), room.getGroup(), room, this));
	}

	/**
	 * Assigns the variable value
	 * 
	 * @param value Variable value
	 */
	public void setValue(Object value) {
		this.type = null;
		this.value = value;

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new RoomVariableValueUpdateEvent(ZoneManager.getInstance(),
				room.getGroup().getZone(), room.getGroup(), room, this));
	}

	/**
	 * Checks if the variable is dynamic
	 * 
	 * @return True if dynamic, false otherwise
	 */
	public boolean isDynamicVariable() {
		return dynamic;
	}

	/**
	 * Retrieves the dynamic assignment key
	 * 
	 * @return Dynamic assignment key string
	 */
	public String getDynamicAssignmentKey() {
		return dynamicVarKey;
	}

	/**
	 * Checks if the variable is private
	 * 
	 * @return True if private, false otherwise
	 */
	public boolean isPrivate() {
		return isPrivate;
	}

	/**
	 * Checks if the variable is persistent
	 * 
	 * @return True if persistent, false otherwise
	 */
	public boolean isPersistent() {
		return isPersistent;
	}

	/**
	 * Retrieves the room instance
	 * 
	 * @return RoomInfo object
	 */
	public RoomInfo getRoom() {
		return room;
	}

	/**
	 * Retrieves the variable name
	 * 
	 * @return Variable name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves the variable type
	 * 
	 * @return VariableType value
	 */
	public VariableType getType() {
		if (type == null)
			return VariableType.UNDEFINED;
		return type;
	}

	/**
	 * Retrieves the variable value
	 * 
	 * @return Variable value
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Writes the variable as an object array
	 * 
	 * @return Object array of the variable
	 */
	public Object[] toObjectArray() {
		populate();
		return toObjectArray(getName(), getType(), getValue(), isPrivate(), isPersistent());
	}

	/**
	 * Writes variable object arrays
	 * 
	 * @param name         Variable name
	 * @param varType      Variable type
	 * @param value        Variable value
	 * @param isPrivate    Value for isPrivate
	 * @param isPersistent Value for isPersistent
	 * @return Variable object array
	 */
	public static Object[] toObjectArray(String name, VariableType varType, Object value, boolean isPrivate,
			boolean isPersistent) {
		// Create writer
		SequenceWriter wr = new SequenceWriter();

		// Write name
		wr.writeString(name);

		// Find type
		int type = varType.getValue();
		if (type == -1) {
			if (value == null)
				type = 0;
			else if (value instanceof Boolean)
				type = 1;
			else if (value instanceof Integer)
				type = 2;
			else if (value instanceof Double)
				type = 3;
			else if (value instanceof String)
				type = 4;
			else if (value instanceof SmartfoxPayload)
				type = 5;
			else if (value instanceof Object[])
				type = 6;
			else
				throw new IllegalArgumentException(
						"Invalid variable value for '" + name + "': " + value + ": invalid type");
		}

		// Write type
		wr.writeByte((byte) type);

		// Write value
		switch (type) {

		case 0:
			wr.writeObject(null);
			break;

		case 1:
			wr.writeBoolean((boolean) value);
			break;

		case 2:
			wr.writeInt((int) value);
			break;

		case 3:
			wr.writeDouble((double) value);
			break;

		case 4:
			wr.writeString((String) value);
			break;

		case 5:
			wr.writeObject((SmartfoxPayload) value);
			break;

		case 6:
			wr.writeObjectArray((Object[]) value);
			break;

		}

		// Write isPrivate
		wr.writeBoolean(isPrivate);

		// Write isPersistent
		wr.writeBoolean(isPersistent);

		// Return
		return wr.toArray();
	}

	/**
	 * Parses object arrays to room variable instances
	 * 
	 * @param varData Variable data
	 * @return RoomVariable instance
	 */
	public static RoomVariable parseObjectArray(Object[] varData) {
		SequenceReader reader = new SequenceReader(varData);

		// Read name
		String name = reader.readString();

		// Read type
		int type = reader.readInt();

		// Read object
		Object value = null;
		switch (type) {

		case 0:
			value = null;
			break;

		case 1:
			value = reader.readBoolean();
			break;

		case 2:
			value = reader.readInt();
			break;

		case 3:
			value = reader.readDouble();
			break;

		case 4:
			value = reader.readString();
			break;

		case 5:
			value = reader.readObject();
			break;

		case 6:
			value = reader.readObjectArray();
			break;

		default:
			throw new IllegalArgumentException("Invalid variable value for '" + name + "': invalid type: " + type);

		}

		// Read settings
		boolean isPrivate = reader.readBoolean();
		boolean isPersistent = reader.readBoolean();

		// Return
		return new RoomVariable(null, name, VariableType.fromIntValue(type), value, isPrivate, isPersistent);
	}

}
