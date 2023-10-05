package org.asf.edge.common.xmls.items.possiblestats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class StatRange {

	@JsonProperty("ISID")
	public int itemStatID;

	@JsonProperty("ISN")
	public String itemStatName;

	@JsonProperty("ITID")
	public int itemTierID;

	@JsonProperty("SR")
	public int startRange;

	@JsonProperty("ER")
	public int endRange;

}
