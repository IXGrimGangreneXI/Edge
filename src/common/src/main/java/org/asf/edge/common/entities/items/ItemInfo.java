package org.asf.edge.common.entities.items;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 
 * Item information container
 * 
 * @author Sky Swimmer
 *
 */
public class ItemInfo {
	private int id;
	private String name;
	private String description;
	private ObjectNode raw;

	public ItemInfo(int id, String name, String description, ObjectNode raw) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.raw = raw;
	}

	/**
	 * Retrieves the item ID
	 * 
	 * @return Item ID
	 */
	public int getID() {
		return id;
	}

	/**
	 * Retrieves the item name
	 * 
	 * @return Item name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves the item description
	 * 
	 * @return Item description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Retrieves the raw item object
	 * 
	 * @return ObjectNode instance
	 */
	public ObjectNode getRawObject() {
		return raw;
	}

	public void reloadDef() {
		// Reload
		id = raw.get("id").asInt();
		name = raw.get("itn").asText();
		description = raw.get("d").asText();
	}
}
