package org.asf.edge.gameplayapi.xmls.dragons;

import org.asf.edge.common.xmls.inventories.InventoryUpdateResponseData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class CreatePetResponseData {

	@JsonProperty("rpd")
	public ObjectNode dragonData;

	@JsonProperty("cir")
	public InventoryUpdateResponseData inventoryUpdate;

}
