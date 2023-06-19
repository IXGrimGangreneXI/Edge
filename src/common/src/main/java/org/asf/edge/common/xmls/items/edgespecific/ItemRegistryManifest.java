package org.asf.edge.common.xmls.items.edgespecific;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ItemRegistryManifest {

	public DefaultItemsBlock defaultItems;

	@JacksonXmlElementWrapper(useWrapping = false)
	public ObjectNode[] itemDefs;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class DefaultItemsBlock {

		@JsonProperty("DefaultItemDef")
		@JacksonXmlElementWrapper(useWrapping = false)
		public DefaultItemBlock[] defaultItems;

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class DefaultItemBlock {

		public int inventoryID;

		public int itemID;

		public int quantity;

		public int uses;

	}

}
