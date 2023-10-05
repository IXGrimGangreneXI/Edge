package org.asf.edge.common.xmls.items.stats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ItemStatData {

	@JsonProperty("ID")
	public int itemStatID;

	@JsonProperty("N")
	public String statName;

	@JsonProperty("V")
	public String statValue;

	@JsonProperty("DTI")
	public String statType;

}
