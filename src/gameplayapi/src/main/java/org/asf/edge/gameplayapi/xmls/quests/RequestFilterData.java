package org.asf.edge.gameplayapi.xmls.quests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class RequestFilterData {

	@JsonProperty("MGID")
	@JacksonXmlElementWrapper(useWrapping = false)
	public int[] groupIDs;

	@JsonProperty("MP")
	@JacksonXmlElementWrapper(useWrapping = false)
	public MissionPairBlock[] missions;

	@JsonProperty("GCM")
	public boolean getCompletedMissions;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class MissionPairBlock {

		@JsonProperty("MID")
		public int missionID;

		@JsonProperty("VID")
		public int versionID = -1;

	}

}
