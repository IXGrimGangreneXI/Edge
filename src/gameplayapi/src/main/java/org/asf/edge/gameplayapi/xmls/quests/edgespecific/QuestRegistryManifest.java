package org.asf.edge.gameplayapi.xmls.quests.edgespecific;

import org.asf.edge.gameplayapi.xmls.quests.MissionData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class QuestRegistryManifest {

	public DefaultStartedQuestsBlock defaultStartedQuests;
	public DefaultUnlockedQuestsBlock defaultUnlockedQuests;
	public QuestDefsBlock defaultQuestDefs;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class DefaultStartedQuestsBlock {

		@JsonProperty("DefaultStartedQuest")
		@JacksonXmlElementWrapper(useWrapping = false)
		public int[] defaultStartedQuests;

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class DefaultUnlockedQuestsBlock {

		@JsonProperty("DefaultUnlockedQuest")
		@JacksonXmlElementWrapper(useWrapping = false)
		public int[] defaultUnlockedQuests;

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class QuestDefsBlock {

		@JsonProperty("QuestDef")
		@JacksonXmlElementWrapper(useWrapping = false)
		public MissionData[] questDefs;

	}
}
