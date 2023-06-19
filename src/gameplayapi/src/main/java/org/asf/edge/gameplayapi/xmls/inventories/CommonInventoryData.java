package org.asf.edge.gameplayapi.xmls.inventories;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class CommonInventoryData {

	@JsonProperty("uid")
	public String userID;

	@JsonProperty("i")
	@JacksonXmlElementWrapper(useWrapping = false)
	public ItemBlock[] items;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class ItemBlock {

		@JsonProperty("uiid")
		public int userInventoryID;

		@JsonProperty("iid")
		public int itemID;

		@JsonProperty("q")
		public int quantity;

		@JsonProperty("u")
		public int uses;

		@JsonProperty("i")
		public ObjectNode data;

	}

}
