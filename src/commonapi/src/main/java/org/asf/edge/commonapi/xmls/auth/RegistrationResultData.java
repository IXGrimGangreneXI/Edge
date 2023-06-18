package org.asf.edge.commonapi.xmls.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class RegistrationResultData {

	public LoginStatusType status;

	public String userID;

	public String apiToken;

	public String parentLoginInfo;

}
