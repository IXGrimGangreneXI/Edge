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
public class AchievementRewardDefData {

	public int achievementID;

	@JsonInclude(Include.NON_NULL)
	@JsonProperty("Reward")
	@JacksonXmlElementWrapper(useWrapping = false)
	public AchievementRewardEntryBlock[] rewards;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class AchievementRewardEntryBlock {

		public int itemID = -1;

		public int rewardID = -1;

		public int pointTypeID = -1;

		public boolean allowMultiple;

		public int minAmount;

		public int maxAmount;

	}

}
