package org.asf.edge.gameplayapi.xmls.names;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class NameValidationRequest {

	public String name;

	// 1 = avatar, 2 = dragon, 3 = group, 4 = default
	@JsonProperty("Category")
	public int category;

}
