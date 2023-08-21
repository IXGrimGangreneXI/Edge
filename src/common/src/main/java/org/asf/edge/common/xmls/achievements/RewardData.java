package org.asf.edge.common.xmls.achievements;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class RewardData {

	@JsonProperty("EntityID")
	public String entityID;

	@JsonInclude(Include.NON_DEFAULT)
	@JsonProperty("AchGrpID")
	public int taskGroupID;

}
