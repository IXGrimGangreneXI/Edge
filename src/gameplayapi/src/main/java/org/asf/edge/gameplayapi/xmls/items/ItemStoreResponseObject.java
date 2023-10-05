package org.asf.edge.gameplayapi.xmls.items;

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
public class ItemStoreResponseObject {

	@JsonProperty("i")
	public int storeID;

	@JsonProperty("s")
	public String storeName;

	@JsonProperty("d")
	public String storeDescription;

	@JsonProperty("is")
	@JacksonXmlElementWrapper(useWrapping = false)
	@JsonInclude(Include.NON_DEFAULT)
	public ItemDefData[] items;

	@JsonProperty("pitem")
	@JacksonXmlElementWrapper(useWrapping = false)
	@JsonInclude(Include.NON_DEFAULT)
	public PopularItemBlock[] popularItems;

	@JsonProperty("ss")
	@JacksonXmlElementWrapper(useWrapping = false)
	@JsonInclude(Include.NON_DEFAULT)
	public SaleBlock[] itemSales;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class PopularItemBlock {

		@JsonProperty("id")
		public int itemID;

		@JsonProperty("c")
		public int rank;

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class SaleBlock {

		@JsonProperty("pcid")
		public int saleID;

		@JsonProperty("m")
		public float modifier;

		@JsonProperty("iids")
		@JacksonXmlElementWrapper(useWrapping = false)
		public int[] itemIDs;

		@JsonProperty("cids")
		@JacksonXmlElementWrapper(useWrapping = false)
		public int[] categoryIDs;

		@JsonProperty("ism")
		public boolean memberOnly;

		@JsonProperty("sd")
		public String startDate;

		@JsonProperty("ed")
		public String endDate;

	}

}
