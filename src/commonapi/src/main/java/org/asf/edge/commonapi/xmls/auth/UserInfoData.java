package org.asf.edge.commonapi.xmls.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class UserInfoData {

	@JacksonXmlProperty(localName = "xmlns", isAttribute = true)
	private final String xmlns = "http://api.jumpstart.com/";

	public String userID;
	public String parentUserID;

	public String username;

	public boolean multiplayerEnabled;

	@JsonProperty("oce")
	public boolean chatEnabled;

	public String registrationDate;

	public String creationDate;

}
