package org.asf.edge.gameplayapi.xmls.quests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class QuestListResponseData {

	@JsonProperty("UID")
	public String userID;

	@JsonProperty("Mission")
	@JacksonXmlElementWrapper(useWrapping = false)
	public MissionData[] quests;

}
