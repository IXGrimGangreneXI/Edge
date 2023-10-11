package org.asf.edge.common.entities.items;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.asf.edge.common.xmls.data.KeyValuePairData;
import org.asf.edge.common.xmls.data.KeyValuePairSetData;

/**
 * 
 * Player inventory item attribute data
 * 
 * @author Sky Swimmer
 * 
 */
public abstract class PlayerInventoryItemAttributes {

	/**
	 * Retrieves all attribute keys
	 * 
	 * @return Array of attribute keys
	 */
	public abstract String[] getAttributeKeys();

	/**
	 * Retrieves the value of a key
	 * 
	 * @param key Key to retrieve the value of
	 * @return Value string or null
	 */
	public abstract String getValue(String key);

	/**
	 * Removes value keys
	 * 
	 * @param key Value to remove
	 */
	public abstract void removeValue(String key);

	/**
	 * Assigns the value of a key
	 * 
	 * @param key   Key to change
	 * @param value Value to assign
	 */
	public abstract void setValue(String key, String value);

	/**
	 * Retrieves the time on which the value of a key last changed
	 * 
	 * @param key Key to retrieve the value of
	 * @return Value update timestamp or -1
	 */
	public abstract long getValueUpdateTime(String key);

	/**
	 * Generates a KeyValuePairSetData instance containing the attributes assigned
	 * in this container
	 * 
	 * @return KeyValuePairSetData instance
	 */
	public KeyValuePairSetData toAttributeData() {
		int i = 0;
		String[] keys = getAttributeKeys();
		KeyValuePairSetData setData = new KeyValuePairSetData();
		setData.items = new KeyValuePairData[keys.length];
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ssXXX");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		for (String key : keys) {
			KeyValuePairData p = new KeyValuePairData();
			p.key = key;
			p.value = getValue(key);
			p.updateDate = fmt.format(new Date(getValueUpdateTime(key)));
			setData.items[i++] = p;
		}
		return setData;
	}

}
