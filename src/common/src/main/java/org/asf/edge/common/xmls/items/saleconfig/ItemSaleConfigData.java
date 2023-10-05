package org.asf.edge.common.xmls.items.saleconfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ItemSaleConfigData {

	@JsonProperty("IID")
	@JsonInclude(Include.NON_DEFAULT)
	public int itemID;

	@JsonProperty("CID")
	@JsonInclude(Include.NON_DEFAULT)
	public int categoryID;

	@JsonProperty("RID")
	@JsonInclude(Include.NON_DEFAULT)
	public int rarityID;

	@JsonProperty("QTY")
	public int quanity;

	@JsonProperty("RIID")
	public int rewardItemID;

}
