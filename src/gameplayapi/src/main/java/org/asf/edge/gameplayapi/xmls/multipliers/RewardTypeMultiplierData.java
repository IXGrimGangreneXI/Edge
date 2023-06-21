package org.asf.edge.gameplayapi.xmls.multipliers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class RewardTypeMultiplierData {

	@JsonProperty("RT")
	public int typeID;

	@JsonProperty("MF")
	public int factor;

	@JsonProperty("TD")
	public String expiryTime;

}
