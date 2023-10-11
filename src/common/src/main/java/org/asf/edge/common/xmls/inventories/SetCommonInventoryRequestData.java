package org.asf.edge.common.xmls.inventories;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class SetCommonInventoryRequestData {

	@JsonProperty("u")
	public String uses;

	@JsonProperty("iid")
	public int itemID;

	@JsonProperty("q")
	public int quantity;

	@JsonProperty("cid")
	public int itemUniqueID = -1;

	@JsonProperty("im")
	public String inventoryMax;

}
