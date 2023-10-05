package org.asf.edge.common.xmls.items.stats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ItemStatMap {

	@JsonProperty("IID")
	public int itemID;

	@JsonProperty("IT")
	public int itemTier;

	@JsonProperty("ISS")
	@JacksonXmlElementWrapper(useWrapping = false)
	public ItemStatData[] stats = new ItemStatData[0];

}
