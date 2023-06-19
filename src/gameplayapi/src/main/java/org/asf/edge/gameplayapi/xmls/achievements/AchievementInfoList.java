package org.asf.edge.gameplayapi.xmls.achievements;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class AchievementInfoList {

	@JacksonXmlProperty(localName = "xmlns", isAttribute = true)
	private final String xmlns = "http://api.jumpstart.com/";

	@JsonProperty("UserAchievementInfo")
	@JacksonXmlElementWrapper(useWrapping = false)
	public AchievementBlock[] achievements;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class AchievementBlock {

		@JsonProperty("u")
		public String userID;

		@JsonProperty("n")
		public String saveName;

		@JsonProperty("a")
		public int pointsTotal;

		@JsonProperty("r")
		public int rankID;

		@JsonProperty("p")
		public int pointTypeID;

	}
}
