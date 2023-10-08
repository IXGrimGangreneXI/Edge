package org.asf.edge.gameplayapi.xmls.inventories;

import org.asf.edge.common.xmls.items.ItemDefData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class InventoryUpdateResponseData {

	@JsonProperty("pir")
	@JacksonXmlElementWrapper(useWrapping = false)
	@JsonInclude(Include.NON_NULL)
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
		public ItemDefData[] mysteryPrizeItems;

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class ItemUpdateBlock {

		@JsonProperty("iid")
		public int itemID;

		@JsonProperty("cid")
		public int itemUniqueID;

		@JsonProperty("qty")
		public int quantity;

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
