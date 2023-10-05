package org.asf.edge.common.xmls.items.possiblestats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class PossibleStat {

	@JsonProperty("IID")
	public int itemID;

	@JsonProperty("ISID")
	public int itemStatID;

	@JsonProperty("SID")
	public int setID;

	@JsonProperty("PROB")
	public int probability;

	@JsonProperty("ISRM")
	@JacksonXmlElementWrapper(useWrapping = false)
	public StatRange[] itemStatsRanges = new StatRange[0];

}
