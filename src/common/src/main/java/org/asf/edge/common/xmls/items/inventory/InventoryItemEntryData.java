package org.asf.edge.common.xmls.items.inventory;

import org.asf.edge.common.xmls.data.KeyValuePairSetData;
import org.asf.edge.common.xmls.items.ItemDefData;
import org.asf.edge.common.xmls.items.stats.ItemStatData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class InventoryItemEntryData {

	@JsonProperty("uiid")
	public int uniqueItemID;

	@JsonProperty("iid")
	public int itemID;

	@JsonProperty("q")
	public int quantity;

	@JsonProperty("u")
	public int uses;

	@JsonProperty("i")
	public ItemDefData data;

	@JsonProperty("IT")
	public String tier;

	@JsonProperty("uia")
	public KeyValuePairSetData itemAttributes;

	@JsonProperty("iss")
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlElementWrapper(useWrapping = false)
	public ItemStatData[] stats = new ItemStatData[0];

}
