package org.asf.edge.common.xmls.items.textures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ItemTextureData {

	@JsonProperty("n")
	public String textureName;

	@JsonProperty("t")
	public String textureTypeName;

	@JsonProperty("x")
	@JsonInclude(Include.NON_DEFAULT)
	public float offsetX;

	@JsonProperty("y")
	@JsonInclude(Include.NON_DEFAULT)
	public float offsetY;

}
