package org.asf.edge.gameplayapi.xmls.rooms;

import org.asf.edge.gameplayapi.entities.rooms.RoomItemInfo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class RoomItemUpdateRequestData extends RoomItemData {

	@JsonIgnore
	public RoomItemInfo result;

	@JsonProperty("pix")
	public IntWrapper parentIndex;

}
