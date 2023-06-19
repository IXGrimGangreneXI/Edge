package org.asf.edge.gameplayapi.xmls.dragons;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class EmptyDragonListData {

	@JacksonXmlProperty(localName = "xmlns:xsi", isAttribute = true)
	public String ns = "http://www.w3.org/2001/XMLSchema-instance";

	@JacksonXmlProperty(localName = "xsi:nil", isAttribute = true)
	public String nil = "true";

}
