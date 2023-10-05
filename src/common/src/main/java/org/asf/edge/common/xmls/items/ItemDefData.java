package org.asf.edge.common.xmls.items;

import org.asf.edge.common.xmls.items.attributes.ItemAttributeData;
import org.asf.edge.common.xmls.items.availability.ItemAvailabilityData;
import org.asf.edge.common.xmls.items.categories.ItemCategoryData;
import org.asf.edge.common.xmls.items.possiblestats.ItemPossibleStatData;
import org.asf.edge.common.xmls.items.relation.ItemRelationData;
import org.asf.edge.common.xmls.items.saleconfig.ItemSaleConfigData;
import org.asf.edge.common.xmls.items.state.ItemStateData;
import org.asf.edge.common.xmls.items.stats.ItemStatMap;
import org.asf.edge.common.xmls.items.textures.ItemTextureData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ItemDefData {

	@JacksonXmlProperty(localName = "xmlns", isAttribute = true)
	public String xmlns = "";

	@JsonProperty("id")
	public int id;

	@JsonProperty("itn")
	public String name;

	@JsonProperty("itnp")
	public String namePlural;

	@JsonProperty("d")
	public String description;

	@JsonProperty("is")
	@JacksonXmlElementWrapper(useWrapping = false)
	public ItemStateData[] states = new ItemStateData[0];

	@JsonProperty("ipsm")
	public ItemPossibleStatData possibleStats;

	@JsonProperty("ism")
	public ItemStatMap statMap;

	@JsonProperty("at")
	@JacksonXmlElementWrapper(useWrapping = false)
	public ItemAttributeData[] attributes = new ItemAttributeData[0];

	@JsonProperty("r")
	@JacksonXmlElementWrapper(useWrapping = false)
	public ItemRelationData[] relations = new ItemRelationData[0];

	@JsonProperty("c")
	@JacksonXmlElementWrapper(useWrapping = false)
	public ItemCategoryData[] categories = new ItemCategoryData[0];

	@JsonProperty("av")
	@JacksonXmlElementWrapper(useWrapping = false)
	public ItemAvailabilityData[] availability = new ItemAvailabilityData[0];

	@JsonProperty("iscs")
	@JacksonXmlElementWrapper(useWrapping = false)
	public ItemSaleConfigData[] saleConfigs = new ItemSaleConfigData[0];

	@JsonProperty("sf")
	public int saleFactor;

	@JsonProperty("u")
	public int uses;

	@JsonProperty("rtid")
	public int rewardTypeID;

	@JsonProperty("p")
	@JsonInclude(Include.NON_DEFAULT)
	public int points;

	@JsonProperty("ir")
	@JsonInclude(Include.NON_DEFAULT)
	public int itemRarity;

	@JsonProperty("rid")
	@JsonInclude(Include.NON_DEFAULT)
	public int rankID;

	@JsonProperty("im")
	public int inventoryMax;

	@JsonProperty("cp")
	public int creativePoints;

	@JsonProperty("l")
	public boolean locked;

	@JsonProperty("s")
	public boolean stackable;

	@JsonProperty("as")
	public boolean allowStacking;

	@JsonProperty("ct")
	public int costCoins;

	@JsonProperty("ct2")
	public int costGems;

	@JsonProperty("an")
	public String assetName;

	@JsonProperty("g")
	public String geometry;

	@JsonProperty("t")
	@JacksonXmlElementWrapper(useWrapping = false)
	public ItemTextureData[] textures = new ItemTextureData[0];

	@JsonProperty("icn")
	public String iconName;

	@JsonProperty("ro")
	public ItemRolloverData rollover;

	@JsonProperty("bp")
	public ObjectNode blueprint;

}
