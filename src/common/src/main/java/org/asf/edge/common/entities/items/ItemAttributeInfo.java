package org.asf.edge.common.entities.items;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 
 * Item attribute information
 * 
 * @author Sky Swimmer
 *
 */
public class ItemAttributeInfo {
	private String key;
	private JsonNode value;

	public ItemAttributeInfo(String key, JsonNode value) {
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
	public JsonNode getValue() {
		return value;
	}
}
