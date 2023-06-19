package org.asf.edge.gameplayapi.xmls.items;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ItemStoreDefinitionData {

	@JsonProperty("i")
	public int storeID;

	@JsonProperty("s")
	public String storeName;

	@JsonProperty("d")
	public String storeDescription;

	@JsonProperty("is")
	@JacksonXmlElementWrapper(useWrapping = false)
	@JsonInclude(Include.NON_DEFAULT)
	public ObjectNode[] items;

	/**
	 * Retrieves items by ID
	 * 
	 * @param id Item ID
	 * @return ObjectNode instance or null
	 */
	@JsonIgnore
	public ObjectNode getItem(int id) {
		for (ObjectNode nd : items)
			if (nd.get("id").asInt() == id)
				return nd;
		return null;
	}

	/**
	 * Retrieves the item IDs known to the shop
	 * 
	 * @return List of shop item IDs
	 */
	@JsonIgnore
	public int[] getItems() {
		int[] arr = new int[items.length];
		for (int i = 0; i < arr.length; i++)
			arr[i] = items[i].get("id").asInt();
		return arr;
	}

}
