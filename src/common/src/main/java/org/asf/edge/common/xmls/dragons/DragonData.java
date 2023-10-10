package org.asf.edge.common.xmls.dragons;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class DragonData {

	@JsonProperty("id")
	public int id;

	@JsonProperty("eid")
	@JsonInclude(Include.NON_NULL)
	public String entityID;

	@JsonProperty("uid")
	@JsonInclude(Include.NON_NULL)
	public String userID;

	@JsonProperty("n")
	@JsonInclude(Include.NON_NULL)
	public String name;

	@JsonProperty("ptid")
	@JsonInclude(Include.NON_NULL)
	public String typeID;

	@JsonProperty("gs")
	@JsonInclude(Include.NON_NULL)
	public ObjectNode growthState;

	@JsonProperty("ip")
	@JsonInclude(Include.NON_NULL)
	public String imagePosition;

	@JsonProperty("g")
	@JsonInclude(Include.NON_NULL)
	public String geometry;

	@JsonProperty("t")
	@JsonInclude(Include.NON_NULL)
	public String texture;

	@JsonProperty("gd")
	@JsonInclude(Include.NON_NULL)
	public String gender;

	@JsonProperty("ac")
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlElementWrapper(useWrapping = false)
	public ObjectNode[] accessories;

	@JsonProperty("at")
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlElementWrapper(useWrapping = false)
	public ObjectNode[] attributes;

	@JsonProperty("c")
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlElementWrapper(useWrapping = false)
	public ObjectNode[] colors;

	@JsonProperty("sk")
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlElementWrapper(useWrapping = false)
	public ObjectNode[] skills;

	@JsonProperty("st")
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlElementWrapper(useWrapping = false)
	public ObjectNode[] states;

	@JsonProperty("is")
	public boolean isSelected;

	@JsonProperty("ir")
	public boolean isReleased;

	@JsonProperty("cdt")
	@JsonInclude(Include.NON_NULL)
	public String createDate;

	@JsonProperty("updt")
	@JsonInclude(Include.NON_NULL)
	public String updateDate;

}
