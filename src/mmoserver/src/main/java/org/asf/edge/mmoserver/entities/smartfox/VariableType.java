package org.asf.edge.mmoserver.entities.smartfox;

public enum VariableType {

	UNDEFINED(-1),

	NULL(0),

	BOOLEAN(1),

	INTEGER(2),

	DOUBLE(3),

	STRING(4),

	OBJECT(5),

	ARRAY(6);

	private int value;

	private VariableType(int value) {
		this.value = value;
	}

	public static VariableType fromIntValue(int value) {
		for (VariableType type : values())
			if (type.getValue() == value)
				return type;
		return VariableType.UNDEFINED;
	}

	public int getValue() {
		return value;
	}

}
