package org.asf.edge.common.xmls.achievements.edgespecific;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class AchievementRewardTransformerData {

	public int achievementID;

	@JsonProperty("Mode")
	public String mode = "Merge";

	@JsonInclude(Include.NON_NULL)
	@JsonProperty("Reward")
	@JacksonXmlElementWrapper(useWrapping = false)
	public TransformerRewardEntryBlock[] rewards;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class TransformerRewardEntryBlock extends AchievementRewardDefData.AchievementRewardEntryBlock {

		public int amount = -1;

		public int transformPointTypeID = -1;
		public int transformItemID = -1;
		public int transformMinAmount = -1;
		public int transformMaxAmount = -1;
		public String transformAllowMultiple;

		public TransformerRewardEntryBlock() {
			minAmount = -1;
			maxAmount = -1;
		}

	}

}
