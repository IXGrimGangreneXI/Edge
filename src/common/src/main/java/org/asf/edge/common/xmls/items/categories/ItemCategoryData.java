package org.asf.edge.common.xmls.items.categories;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ItemCategoryData {

	@JsonProperty("cid")
	public int categoryID;

	@JsonProperty("cn")
	public String categoryName;

	@JsonProperty("i")
	public String iconName;

}
