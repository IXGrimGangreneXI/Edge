package org.asf.edge.gameplayapi.xmls.names;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class DisplayNameUniqueResponseData {

	@JsonProperty("suggestions")
	public SuggestionResultBlock suggestions;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class SuggestionResultBlock {

		@JsonProperty("Suggestion")
		@JacksonXmlElementWrapper(useWrapping = false)
		public String[] suggestions;

	}

}
