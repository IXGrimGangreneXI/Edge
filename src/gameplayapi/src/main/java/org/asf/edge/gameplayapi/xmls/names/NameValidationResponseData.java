package org.asf.edge.gameplayapi.xmls.names;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class NameValidationResponseData {

	public String errorMessage;

	// 1 = ok, 2 = filtered, 3 = not unique, 4 = invalid
	@JsonProperty("Category")
	public int result;

}
