package org.asf.edge.common.xmls.items.state.criteria;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ItemStateSpeedUpCriteriaData extends ItemStateBaseCriteriaData {

	@JsonProperty("ItemID")
	public int itemID;

	@JsonProperty("ConsumeUses")
	public boolean appliesUses;

	@JsonProperty("Amount")
	public int amount;

	@JsonProperty("ChangeState")
	public boolean changesState;

	@JsonProperty("EndStateID")
	public int targetState;

	@JsonProperty("SpeedUpUses")
	public boolean speedUpUses;

	@JsonProperty("SpeedUpCapacity")
	public int speedUpUsesCount;

}
