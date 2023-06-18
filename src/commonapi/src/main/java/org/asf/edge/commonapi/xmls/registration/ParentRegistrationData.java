package org.asf.edge.commonapi.xmls.registration;

import org.asf.edge.commonapi.xmls.auth.ParentLoginData.UserPolicyBlock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ParentRegistrationData {

	public String email;
	public String password;

	public String locale;

	@JacksonXmlElementWrapper(useWrapping = false)
	public ChildRegistrationData[] childList;

	public UserPolicyBlock userPolicy;

	// 1 = opt in, 2 = opt out
	public int emailNotification;

}
