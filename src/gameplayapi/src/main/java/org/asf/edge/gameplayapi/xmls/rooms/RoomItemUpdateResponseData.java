package org.asf.edge.gameplayapi.xmls.rooms;

import org.asf.edge.gameplayapi.xmls.rooms.RoomItemData.ItemStateBlock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class RoomItemUpdateResponseData {

	@JsonProperty("s")
	public boolean success;

	@JsonProperty("r")
	public int status = 1;

	@JsonProperty("ids")
	@JacksonXmlElementWrapper(useWrapping = false)
	public int[] createdRoomItemIDs;

	@JsonProperty("uciis")
	@JacksonXmlElementWrapper(useWrapping = false)
	public ItemStateBlock[] states;

}
