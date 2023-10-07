package org.asf.edge.common.xmls.items.state.criteria;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ItemStateReplenishableCriteriaData extends ItemStateBaseCriteriaData {

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class ReplenishRateBlock {

		@JsonProperty("Uses")
		public int usesPerPeriod;

		@JsonProperty("Rate")
		public double increasePeriod;

		@JsonProperty("MaxUses")
		public int usesLimit;

		@JsonProperty("Rank")
		public int rankLevel;

	}

	@JsonProperty("ApplyRank")
	public boolean useRank;

	@JsonProperty("PointTypeID")
	public int rankPointTypeID;

	@JsonProperty("ReplenishableRates")
	@JacksonXmlElementWrapper(useWrapping = false)
	public ReplenishRateBlock[] replenishRates = new ReplenishRateBlock[0];

}
