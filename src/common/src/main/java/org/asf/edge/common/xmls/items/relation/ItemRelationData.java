package org.asf.edge.common.xmls.items.relation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ItemRelationData {

	@JsonProperty("t")
	public String type;

	@JsonProperty("id")
	public int itemID;

	@JsonProperty("wt")
	public int weight;

	@JsonProperty("q")
	public int quantity;

}
