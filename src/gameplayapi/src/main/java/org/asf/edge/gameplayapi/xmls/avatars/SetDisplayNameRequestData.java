package org.asf.edge.gameplayapi.xmls.avatars;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class SetDisplayNameRequestData {

	@JsonProperty("dn")
	public String displayName;

	@JsonProperty("iid")
	public int itemID;

	@JsonProperty("sid")
	public int storeID;

}
