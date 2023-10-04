package org.asf.edge.gameplayapi.xmls.quests;

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
public class MissionData {

	@JsonProperty("I")
	public int id;

	@JsonProperty("N")
	public String name;

	@JsonProperty("G")
	public int groupID;

	@JsonProperty("P")
	@JsonInclude(Include.NON_DEFAULT)
	public int parentQuestID;

	@JsonProperty("S")
	public String staticData;

	@JsonProperty("A")
	public boolean accepted;

	@JsonProperty("C")
	public int completed;

	@JsonProperty("MR")
	public MissionRulesBlock missionRules;

	@JsonProperty("V")
	public int version;

	@JsonProperty("AID")
	public int achievementID;

	@JsonProperty("AAID")
	public int acceptanceAchievementID;

	@JsonProperty("M")
	@JacksonXmlElementWrapper(useWrapping = false)
	@JsonInclude(Include.NON_NULL)
	public MissionData[] childMissions;

	@JsonProperty("Task")
	@JacksonXmlElementWrapper(useWrapping = false)
	public TaskBlock[] tasks;

	@JsonProperty("AR")
	@JacksonXmlElementWrapper(useWrapping = false)
	@JsonInclude(Include.NON_NULL)
	public AchievementRewardBlock[] rewards;

	@JsonProperty("AAR")
	@JacksonXmlElementWrapper(useWrapping = false)
	@JsonInclude(Include.NON_NULL)
	public AchievementRewardBlock[] acceptanceRewards;

	@JsonProperty("RPT")
	public String repeatable;

	public MissionData copy() {
		MissionData cp = new MissionData();
		cp.staticData = staticData;
		cp.acceptanceAchievementID = acceptanceAchievementID;
		if (acceptanceRewards != null) {
			cp.acceptanceRewards = new AchievementRewardBlock[acceptanceRewards.length];
			for (int i = 0; i < cp.acceptanceRewards.length; i++)
				cp.acceptanceRewards[i] = acceptanceRewards[i].copy();
		}
		cp.accepted = accepted;
		cp.achievementID = achievementID;
		if (childMissions != null) {
			cp.childMissions = new MissionData[childMissions.length];
			for (int i = 0; i < cp.childMissions.length; i++)
				cp.childMissions[i] = childMissions[i].copy();
		}
		cp.completed = completed;
		cp.groupID = groupID;
		cp.id = id;
		if (missionRules != null)
			cp.missionRules = missionRules.copy();
		cp.name = name;
		cp.parentQuestID = parentQuestID;
		cp.repeatable = repeatable;
		if (tasks != null) {
			cp.tasks = new TaskBlock[tasks.length];
			for (int i = 0; i < cp.tasks.length; i++)
				cp.tasks[i] = tasks[i].copy();
		}
		if (rewards != null) {
			cp.rewards = new AchievementRewardBlock[rewards.length];
			for (int i = 0; i < cp.rewards.length; i++)
				cp.rewards[i] = rewards[i].copy();
		}
		cp.version = version;
		return cp;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class TaskBlock {

		@JsonProperty("I")
		public int id;

		@JsonProperty("N")
		public String name;

		@JsonProperty("S")
		public String staticData;

		@JsonProperty("C")
		public int completed;

		@JsonProperty("F")
		public boolean failed;

		@JsonProperty("P")
		public String payload;

		public TaskBlock copy() {
			TaskBlock cp = new TaskBlock();
			cp.completed = completed;
			cp.failed = failed;
			cp.id = id;
			cp.name = name;
			cp.payload = payload;
			cp.staticData = staticData;
			return cp;
		}

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class MissionRulesBlock {

		@JsonIgnoreProperties(ignoreUnknown = true)
		@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
		public static class PrerequisiteInfoBlock {

			public static class PrerequisiteRuleTypes {
				public static final int MEMBER = 1;
				public static final int ACCEPT = 2;
				public static final int MISSION = 3;
				public static final int RANK = 4;
				public static final int DATERANGE = 5;
				public static final int ITEM = 7;
			}

			public int type;

			public String value;

			@JsonInclude(Include.NON_DEFAULT)
			public int quantity;

			public boolean clientRule;

			public PrerequisiteInfoBlock copy() {
				PrerequisiteInfoBlock cp = new PrerequisiteInfoBlock();
				cp.type = type;
				cp.value = value;
				cp.quantity = quantity;
				cp.clientRule = clientRule;
				return cp;
			}

		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
		public static class CriteriaBlock {

			@JsonIgnoreProperties(ignoreUnknown = true)
			@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
			public static class RuleInfoBlock {

				public static class RuleInfoTypes {
					public static final int TASK = 1;
					public static final int MISSION = 2;
				}

				public int type;

				public int missionID;

				@JsonProperty("ID")
				public int id;

				public int complete;

				public RuleInfoBlock copy() {
					RuleInfoBlock cp = new RuleInfoBlock();
					cp.type = type;
					cp.missionID = missionID;
					cp.id = id;
					cp.complete = complete;
					return cp;
				}

			}

			public String type;

			public boolean ordered;

			public int min;

			public int repeat;

			@JsonProperty("RuleItems")
			@JacksonXmlElementWrapper(useWrapping = false)
			public RuleInfoBlock[] rules;

			public CriteriaBlock copy() {
				CriteriaBlock cp = new CriteriaBlock();
				cp.type = type;
				cp.ordered = ordered;
				cp.min = min;
				cp.repeat = repeat;
				if (rules != null) {
					cp.rules = new RuleInfoBlock[rules.length];
					for (int i = 0; i < cp.rules.length; i++)
						cp.rules[i] = rules[i].copy();
				}
				return cp;
			}

		}

		@JacksonXmlElementWrapper(useWrapping = false)
		public PrerequisiteInfoBlock[] prerequisites;

		public CriteriaBlock criteria;

		public MissionRulesBlock copy() {
			MissionRulesBlock cp = new MissionRulesBlock();
			if (criteria != null)
				cp.criteria = criteria.copy();
			if (prerequisites != null) {
				cp.prerequisites = new PrerequisiteInfoBlock[prerequisites.length];
				for (int i = 0; i < cp.prerequisites.length; i++)
					cp.prerequisites[i] = prerequisites[i].copy();
			}
			return cp;
		}

	}

}
