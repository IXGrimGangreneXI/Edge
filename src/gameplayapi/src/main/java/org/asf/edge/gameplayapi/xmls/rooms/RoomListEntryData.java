package org.asf.edge.gameplayapi.xmls.rooms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class RoomListEntryData {

	@JsonProperty("N")
	public String name;

	@JsonProperty("R")
	public String roomID;

	@JsonProperty("CP")
	public double creativePoints = 0;

	@JsonProperty("C")
	public int categoryID = -1;

	@JsonProperty("IID")
	public int itemID = -1;

}
