package org.asf.edge.common.services.tabledata;

import java.util.Map;

/**
 * 
 * Data filter container, used to filter rows in data table requests
 * 
 * @author Sky Swimmer
 * 
 */
public class DataFilter extends DataSet {

	/**
	 * Creates a empty data filter
	 */
	public DataFilter() {
	}

	/**
	 * Creates a data filter populated with values from a Map instance
	 * 
	 * @param values Values to use in the filter
	 */
	public DataFilter(Map<String, Object> values) {
		for (String key : values.keySet())
			setValue(key, values.get(key));
	}

	/**
	 * Creates a data filter populated with the values from a data set
	 * 
	 * @param set Values to use in the filter
	 */
	public DataFilter(DataSet set) {
		for (String key : set.getColumnNames())
			if (set.getValueType(key) != DataType.NULL)
				setValue(key, set.getValue(key, Object.class));
	}

}
