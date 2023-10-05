package org.asf.edge.common.xmls.items.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ItemStateCompletionAction {

	@JsonProperty("Transition")
	public int transitionMode;

	@JsonProperty("AchievementCompletion")
	@JacksonXmlElementWrapper(useWrapping = false)
	public ItemStateAchievementCompletionData[] achievements = new ItemStateAchievementCompletionData[0];

}
