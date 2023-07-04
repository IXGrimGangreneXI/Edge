package org.asf.edge.gameplayapi.xmls.quests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class RequestFilterDataLegacy {

	@JsonProperty("MGID")
	@JacksonXmlElementWrapper(useWrapping = false)
	public int groupID = -1;

	@JsonProperty("MID")
	@JacksonXmlElementWrapper(useWrapping = false)
	public int missionID = -1;

	@JsonProperty("GCM")
	public boolean getCompletedMissions;

}
