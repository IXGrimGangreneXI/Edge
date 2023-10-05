package org.asf.edge.common.entities.items;

/**
 * 
 * Item attribute information
 * 
 * @author Sky Swimmer
 *
 */
public class ItemAttributeInfo {
	private String key;
	private String value;

	public ItemAttributeInfo(String key, String value) {
		this.key = key;
		this.value = value;
	}

	/**
	 * Retrieves the attribute key
	 * 
	 * @return Attribute key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Retrieves the attribute value
	 * 
	 * @return Attribute value
	 */
	public String getValue() {
		return value;
	}
}
