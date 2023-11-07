package org.asf.edge.common.services.tabledata;

import java.util.Date;

public enum DataType {

	NULL,

	OBJECT,

	STRING,

	CHAR,

	BYTE,

	SHORT,

	INT,

	LONG,

	FLOAT,

	DOUBLE,

	BOOLEAN,

	BYTE_ARRAY,

	DATE;

	/**
	 * Finds the DataType of a given object
	 * 
	 * @param obj Object to retrieve the type of
	 * @return DataType value
	 */
	public static DataType fromObject(Object obj) {
		if (obj == null)
			return DataType.NULL;
		return fromClass(obj.getClass());
	}

	/**
	 * Finds the DataType of a given class instance
	 * 
	 * @param cls Class to retrieve the type of
	 * @return DataType value
	 */
	public static DataType fromClass(Class<?> cls) {
		// Check primitive
		if (cls.isPrimitive()) {
			// Find primitive
			switch (cls.getTypeName()) {

			case "boolean":
				return DataType.BOOLEAN;

			case "byte":
				return DataType.BYTE;

			case "char":
				return DataType.CHAR;

			case "short":
				return DataType.SHORT;

			case "int":
				return DataType.INT;

			case "long":
				return DataType.LONG;

			case "float":
				return DataType.FLOAT;

			case "double":
				return DataType.DOUBLE;

			}
		}

		// Handle byte arrays and strings
		if (String.class.isAssignableFrom(cls))
			return DataType.STRING;
		else if (byte[].class.isAssignableFrom(cls))
			return DataType.BYTE_ARRAY;

		// Handle object types
		if (Date.class.isAssignableFrom(cls))
			return DataType.DATE;

		// Handle wrappers
		if (Boolean.class.isAssignableFrom(cls))
			return DataType.BOOLEAN;
		else if (Byte.class.isAssignableFrom(cls))
			return DataType.BYTE;
		else if (Short.class.isAssignableFrom(cls))
			return DataType.SHORT;
		else if (Integer.class.isAssignableFrom(cls))
			return DataType.INT;
		else if (Long.class.isAssignableFrom(cls))
			return DataType.LONG;
		else if (Float.class.isAssignableFrom(cls))
			return DataType.FLOAT;
		else if (Double.class.isAssignableFrom(cls))
			return DataType.DOUBLE;
		else if (Character.class.isAssignableFrom(cls))
			return DataType.CHAR;

		// Object
		return DataType.OBJECT;
	}

}
