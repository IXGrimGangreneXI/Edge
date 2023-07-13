package org.asf.edge.gameplayapi.xmls.items;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ItemPurchaseRequestData {

	@JsonProperty("cid")
	public int containerID;

	@JsonProperty("sid")
	public int storeID;

	@JsonProperty("i")
	@JacksonXmlElementWrapper(useWrapping = false)
	public int[] itemIDs;

	@JsonProperty("ambi")
	public boolean addBoxesAsItem;

}
