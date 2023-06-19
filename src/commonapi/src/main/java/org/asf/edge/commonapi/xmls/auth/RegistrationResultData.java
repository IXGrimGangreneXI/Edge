package org.asf.edge.commonapi.xmls.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class RegistrationResultData {

	public LoginStatusType status;

	public String userID;

	public String apiToken;

	public String parentLoginInfo;

	@JsonProperty("Suggestions")
	public SuggestionResultBlock suggestions;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class SuggestionResultBlock {

		@JsonProperty("Suggestion")
		@JacksonXmlElementWrapper(useWrapping = false)
		public String[] suggestions;

	}
}
