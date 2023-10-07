package org.asf.edge.gameplayapi.xmls.rooms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class SetItemStateRequestData {

	@JsonProperty("SID")
	public int storeID;

	@JsonProperty("CIID")
	public int itemUniqueID;

	@JsonProperty("UIPID")
	public int roomItemID;

	@JsonProperty("OSC")
	public boolean overrideStateCriteria;

}
