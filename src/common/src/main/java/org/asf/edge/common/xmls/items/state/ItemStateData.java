package org.asf.edge.common.xmls.items.state;

import org.asf.edge.common.xmls.achievements.AchievementRewardData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ItemStateData {

	@JsonProperty("ItemStateID")
	public int stateID;

	@JsonProperty("Name")
	public String stateName;

	@JsonProperty("Rule")
	public ItemStateRuleData rules;

	@JsonProperty("Order")
	public int order;

	@JsonProperty("AchievementID")
	@JsonInclude(Include.NON_DEFAULT)
	public int achievementID;

	@JsonProperty("Rewards")
	@JacksonXmlElementWrapper(useWrapping = false)
	public AchievementRewardData[] rewards = new AchievementRewardData[0];

}
