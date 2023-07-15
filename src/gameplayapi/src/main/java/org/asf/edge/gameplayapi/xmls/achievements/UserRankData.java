package org.asf.edge.gameplayapi.xmls.achievements;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class UserRankData {

	@JacksonXmlProperty(localName = "xmlns", isAttribute = true)
	private final String xmlns = "";

	public int rankID;

	@JsonProperty("Name")
	public String rankName;

	@JsonProperty("Description")
	public String rankDescription;

	public boolean isMember;

	public String audio;

	public String image;

	public int value;

	public int pointTypeID;

	public int globalRankID;

}
