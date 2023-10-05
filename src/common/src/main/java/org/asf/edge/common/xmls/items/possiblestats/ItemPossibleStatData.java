package org.asf.edge.common.xmls.items.possiblestats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ItemPossibleStatData {

	@JsonProperty("IID")
	public int itemID;

	@JsonProperty("SC")
	public int itemStatsCount;

	@JsonProperty("SID")
	public int setID;

	@JsonProperty("SS")
	@JacksonXmlElementWrapper(useWrapping = false)
	public PossibleStat[] stats = new PossibleStat[0];

}
