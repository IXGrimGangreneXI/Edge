package org.asf.edge.gameplayapi.xmls.inventories;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class CommonInventoryRequestData {

	@JsonProperty("CID")
	public int containerID;

	@JsonProperty("LTS")
	public boolean loadItemStats;

}
