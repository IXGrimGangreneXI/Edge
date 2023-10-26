package org.asf.edge.common.services.tabledata;

/**
 * 
 * Table data entry
 * 
 * @author Sky Swimmer
 * 
 */
public class DataEntry {

	private String columnName;
	private Object value;
	private DataType type;

	public DataEntry(String columnName, Object value) {
		this.columnName = columnName;
		this.value = value;

		type = DataType.fromObject(value);
	}

	/**
	 * Retrieves the column name
	 * 
	 * @return Column name string
	 */
	public String getColumnName() {
		return columnName;
	}

	/**
	 * Retrieves the value
	 * 
	 * @param <RT> Return type class
	 * @param cls  Return type
	 * @return Value object
	 * @throws ClassCastException If the value cannot be returned as the requested
	 *                            type
	 */
	@SuppressWarnings("unchecked")
	public <RT> RT getValue(Class<RT> cls) throws ClassCastException {
		return (RT) value;
	}

	/**
	 * Retrieves the value type
	 * 
	 * @return DataType value
	 */
	public DataType getValueType() {
		return type;
	}

}
