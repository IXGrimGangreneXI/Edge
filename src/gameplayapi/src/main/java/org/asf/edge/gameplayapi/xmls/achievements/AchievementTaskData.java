package org.asf.edge.gameplayapi.xmls.achievements;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class AchievementTaskData {

	@JsonProperty("tid")
	public int id;

	@JsonProperty("oid")
	public String entityID;

	@JsonProperty("aiid")
	public int achievementInfoID;

	@JsonProperty("rid")
	public String relatedID;

	@JsonProperty("pts")
	public int points;

	@JsonProperty("etid")
	public int entityTypeID;

}
