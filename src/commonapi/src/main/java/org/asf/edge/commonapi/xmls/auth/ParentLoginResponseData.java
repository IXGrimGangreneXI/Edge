package org.asf.edge.commonapi.xmls.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ParentLoginResponseData extends CommonLoginInfo {

	@JsonProperty("LoginStatus")
	public LoginStatusType status;

	public CommonLoginInfo[] childList;

	public boolean sendActivationReminder;

}
