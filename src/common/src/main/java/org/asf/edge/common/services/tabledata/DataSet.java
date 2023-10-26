package org.asf.edge.common.services.tabledata;

import java.util.HashMap;
import java.util.Iterator;

/**
 * 
 * Table data set
 * 
 * @author Sky Swimmer
 * 
 */
public class DataSet implements Iterable<DataEntry> {

	private HashMap<String, DataEntry> entries = new HashMap<String, DataEntry>();

	@Override
	public Iterator<DataEntry> iterator() {
		return entries.values().iterator();
	}

	/**
	 * Retrieves the amount of entries in this data set
	 * 
	 * @return Entry count
	 */
	public int count() {
		return entries.size();
	}

	/**
	 * Retrieves the column names
	 * 
	 * @return Array of column names
	 */
	public String[] getColumnNames() {
		return entries.values().stream().map(t -> t.getColumnName()).toArray(t -> new String[t]);
	}

	/**
	 * Retrieves all values in the data set
	 * 
	 * @return Array of DataEntry instances
	 */
	public DataEntry[] getValues() {
		return entries.values().toArray(t -> new DataEntry[t]);
	}

	/**
	 * Checks if columns are present in the data set
	 * 
	 * @param columnName Column name to check
	 * @return True if present, false otherwise
	 */
	public boolean has(String columnName) {
		return entries.containsKey(columnName.toUpperCase());
	}

	/**
	 * Clears the data set
	 */
	public void clear() {
		entries.clear();
	}

	/**
	 * Retrieves the value of a column
	 * 
	 * @param <RT>       Return type class
	 * @param columnName Column name
	 * @param cls        Return type
	 * @return Value object
	 * @throws ClassCastException If the value cannot be returned as the requested
	 *                            type
	 */
	public <RT> RT getValue(String columnName, Class<RT> cls) throws ClassCastException {
		if (!entries.containsKey(columnName.toUpperCase()))
			return null;
		return entries.get(columnName.toUpperCase()).getValue(cls);
	}

	/**
	 * Retrieves the value type of a specific column
	 * 
	 * @param columnName Column name
	 * @return DataType value
	 */
	public DataType getValueType(String columnName) {
		if (!entries.containsKey(columnName.toUpperCase()))
			return DataType.NULL;
		return entries.get(columnName.toUpperCase()).getValueType();
	}

	/**
	 * Assigns column values
	 * 
	 * @param <RT>       Value type
	 * @param columnName Column name
	 * @param value      New value
	 */
	public <RT> void setValue(String columnName, RT value) {
		entries.put(columnName.toUpperCase(), new DataEntry(columnName, value));
	}

	/**
	 * Removes values by column name
	 * 
	 * @param columnName Column name of the value to remove
	 */
	public void remove(String columnName) {
		entries.remove(columnName.toUpperCase());
	}

}
