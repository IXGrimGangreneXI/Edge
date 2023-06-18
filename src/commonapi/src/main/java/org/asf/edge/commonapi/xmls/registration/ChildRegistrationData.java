package org.asf.edge.commonapi.xmls.registration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ChildRegistrationData {

	public String childName;
	public String password;

	public int age;

	public boolean isGuest;
	public String guestUserName;

}
