package org.asf.edge.gameplayapi.xmls.avatars;

import org.asf.edge.gameplayapi.xmls.names.DisplayNameUniqueResponseData.SuggestionResultBlock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class SetAvatarResultData {

	public String displayName;

	public boolean success;

	public SuggestionResultBlock suggestions;

	// 1 = valid, 10 = invalid name
	public int statusCode;

}
