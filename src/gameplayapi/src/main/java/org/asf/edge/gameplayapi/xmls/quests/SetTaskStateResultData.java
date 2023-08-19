package org.asf.edge.gameplayapi.xmls.quests;

import org.asf.edge.gameplayapi.xmls.inventories.InventoryUpdateResponseData;
import org.asf.edge.gameplayapi.xmls.achievements.AchievementRewardBlock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class SetTaskStateResultData {

	public static class SetTaskStateResultStatuses {
		public static final int REQUIRES_MEMBERSHIP = 1;
		public static final int REQUIRES_ACCEPTANCE = 2;
		public static final int NOT_WITHIN_DATE_RANGE = 3;
		public static final int PREREQUISITE_MISSION_INCOMPLETE = 4;
		public static final int USER_RANK_LESS_THAN_MIN_RANK = 5;
		public static final int USER_RANK_GREATER_THAN_MAX_RANK = 6;
		public static final int USER_HAS_NO_RANK_DATA = 7;
		public static final int MISSION_STATE_NOT_FOUND = 8;
		public static final int REQUIRED_PRIOR_TASK_INCOMPLETE = 9;
		public static final int PARENT_TASK_INCOMPLETE = 10;
		public static final int PARENT_SUBMISSION_INCOMPLETE = 11;
		public static final int TASK_CAN_BE_DONE = 12;
		public static final int OK_MISSING_REWARD_DATA = 13;
		public static final int PAYLOAD_UPDATED = 14;
		public static final int NON_REPEATABLE_MISSION = 15;
	}

	@JsonProperty("S")
	public boolean success;

	@JsonProperty("T")
	public int status = 12;

	@JsonProperty("R")
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlElementWrapper(useWrapping = false)
	public CompletedMissionInfoBlock[] completedMissions;

	@JsonProperty("C")
	@JsonInclude(Include.NON_NULL)
	public InventoryUpdateResponseData inventoryUpdate;

	public static class CompletedMissionInfoBlock {

		@JsonProperty("M")
		public int missionID;

		@JsonProperty("A")
		@JacksonXmlElementWrapper(useWrapping = false)
		public AchievementRewardBlock[] rewards;

	}

}
