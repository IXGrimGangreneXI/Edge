package org.asf.edge.gameplayapi.xmls.dragons;

import org.asf.edge.gameplayapi.xmls.inventories.InventoryUpdateResponseData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class PetUpdateResponseData {

	public String errorMessage;

	// 1 = ok, 2 = failure, 3 = invalid, 4 = invalid name
	public int raisedPetSetResult;

	@JsonProperty("cir")
	public InventoryUpdateResponseData inventoryUpdate;

}
