package org.asf.edge.gameplayapi.xmls.achievements;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class StableQuestData {

	@JsonInclude(Include.NON_NULL)
	@JsonProperty("Missions")
	@JacksonXmlElementWrapper(useWrapping = false)
	public MissionBlock[] missions;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class MissionBlock {

		@JsonInclude(Include.NON_NULL)
		@JacksonXmlElementWrapper(useWrapping = false)
		public StableQuestDragonAchievementBlock[] winAchievements = new StableQuestDragonAchievementBlock[0];

		@JsonInclude(Include.NON_NULL)
		@JacksonXmlElementWrapper(useWrapping = false)
		public StableQuestDragonAchievementBlock[] loseAchievements = new StableQuestDragonAchievementBlock[0];

		@JsonProperty("WinAchID")
		public int winAchievementID;

		@JsonProperty("LoseAchID")
		public int loseAchievementID;

		@JsonInclude(Include.NON_NULL)
		@JacksonXmlElementWrapper(useWrapping = false)
		public StableQuestRewardBlock[] winRewards;

		@JsonInclude(Include.NON_NULL)
		@JacksonXmlElementWrapper(useWrapping = false)
		public StableQuestRewardBlock[] loseRewards;
	}
}
