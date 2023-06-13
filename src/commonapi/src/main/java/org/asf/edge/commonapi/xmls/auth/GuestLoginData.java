package org.asf.edge.commonapi.xmls.auth;

import org.asf.edge.commonapi.xmls.auth.ParentLoginData.UserPolicyBlock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class GuestLoginData {

	@JsonProperty("UserName")
	public String username;

	public UserPolicyBlock userPolicy;

}
