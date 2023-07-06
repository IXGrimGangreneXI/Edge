package org.asf.edge.gameplayapi.xmls.inventories;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class InventoryUpdateResponseData {

	@JsonProperty("pir")
	@JacksonXmlElementWrapper(useWrapping = false)
	public PrizeItemInfo[] prizeItems;

	@JsonProperty("s")
	public boolean success;

	@JsonProperty("cids")
	@JacksonXmlElementWrapper(useWrapping = false)
	public ItemUpdateBlock[] updateItems;

	@JsonProperty("ugc")
	public CurrencyUpdateBlock currencyUpdate;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class PrizeItemInfo {

		@JsonProperty("i")
		public int boxItemID;

		@JsonProperty("pi")
		public int prizeItemID;

		@JsonProperty("pis")
		@JacksonXmlElementWrapper(useWrapping = false)
		public ObjectNode[] mysteryPrizeItems;

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class ItemUpdateBlock {

		@JsonProperty("iid")
		public int itemID;

		@JsonProperty("cid")
		public int itemUniqueID;

		@JsonProperty("qty")
		public int addedQuantity;

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class CurrencyUpdateBlock {

		@JsonProperty("uid")
		public String userID;

		@JsonProperty("gc")
		public int coinCount;

		@JsonProperty("cc")
		public int gemCount;

	}

}
