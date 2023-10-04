package org.asf.edge.commonapi.xmls.servers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class MMOServerInfoList {

	@JacksonXmlProperty(localName = "xmlns", isAttribute = true)
	private final String xmlns = "http://api.jumpstart.com/";

	@JsonProperty("MMOServerInfo")
	@JacksonXmlElementWrapper(useWrapping = false)
	public MMOServerInfoBlock[] servers;

}
