package org.asf.edge.gameplayapi.xmls.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class KeyValuePairData {

	@JsonProperty("PairKey")
	public String key;

	@JsonProperty("PairValue")
	public String value;

	public String updateDate;

}
