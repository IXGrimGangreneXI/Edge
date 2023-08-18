package org.asf.edge.gameplayapi.xmls.achievements;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class AchievementTaskSetResponseData {

	@JacksonXmlProperty(localName = "xmlns", isAttribute = true)
	private final String xmlns = "";

	@JsonProperty("s")
	public boolean success;

	@JsonProperty("u")
	public boolean userMessage;

	@JsonProperty("a")
	public String achievementName;

	@JsonProperty("l")
	public int level;

	@JsonProperty("aid")
	@JsonInclude(Include.NON_DEFAULT)
	public int achievementTaskGroupID = -1;

	@JsonProperty("LL")
	@JsonInclude(Include.NON_NULL)
	public String lastLevelCompletedBoolean = null;

	@JsonProperty("aiid")
	@JsonInclude(Include.NON_DEFAULT)
	public int achievementInfoID = -1;

}
