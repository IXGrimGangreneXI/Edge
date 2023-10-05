package org.asf.edge.gameplayapi.xmls.rooms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class RoomUpdateResponseData {

	@JsonProperty("S")
	public boolean success = true;

	@JsonProperty("SC")
	public int statusCode = 1;

}
