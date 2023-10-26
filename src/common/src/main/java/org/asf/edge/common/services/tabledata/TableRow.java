package org.asf.edge.common.services.tabledata;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 
 * Table row abstract
 * 
 * @author Sky Swimmer
 * 
 */
public abstract class TableRow {

	@JsonIgnore
	private DataSet valueCache = new DataSet();

	/**
	 * Retrieves the value cache of this table row (holds all previous values
	 * assigned when the row was deserialized)
	 * 
	 * @return DataSet instance
	 */
	@JsonIgnore
	public DataSet getValueCache() {
		return valueCache;
	}

}
