package org.asf.edge.gameplayapi.xmls.rooms;

import org.asf.edge.common.xmls.achievements.AchievementRewardData;
import org.asf.edge.gameplayapi.xmls.rooms.RoomItemData.ItemStateBlock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class SetItemStateResponseData {

	@JsonProperty("S")
	public boolean success;

	@JsonProperty("EC")
	public int errorCode = 1;

	@JsonProperty("UIS")
	public ItemStateBlock state;

	@JsonProperty("AS")
	@JacksonXmlElementWrapper(useWrapping = false)
	public AchievementRewardData[] rewards;
}
