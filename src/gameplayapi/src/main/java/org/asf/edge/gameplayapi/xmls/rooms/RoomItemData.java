package org.asf.edge.gameplayapi.xmls.rooms;

import org.asf.edge.common.xmls.data.KeyValuePairSetData;
import org.asf.edge.common.xmls.items.ItemDefData;
import org.asf.edge.common.xmls.items.stats.ItemStatData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class RoomItemData {

	public static class StringWrapper {
		public StringWrapper() {
		}

		public StringWrapper(String value) {
			this.value = value;
		}

		@JacksonXmlProperty(isAttribute = true)
		public String xmlns = "";

		@JacksonXmlText
		public String value;
	}

	public static class IntWrapper {
		public IntWrapper() {
		}

		public IntWrapper(int value) {
			this.value = value;
		}

		@JacksonXmlProperty(isAttribute = true)
		public String xmlns = "";

		@JacksonXmlText
		public int value;
	}

	public static class DoubleWrapper {
		public DoubleWrapper() {
		}

		public DoubleWrapper(double value) {
			this.value = value;
		}

		@JacksonXmlProperty(isAttribute = true)
		public String xmlns = "";

		@JacksonXmlText
		public double value;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class ItemStateBlock {

		@JacksonXmlProperty(localName = "xmlns", isAttribute = true)
		public String xmlns = "";

		@JsonProperty("CIID")
		public int itemUniqueID;

		@JsonProperty("UIPID")
		public int itemPositionID;

		@JsonProperty("IID")
		public int itemDefID;

		@JsonProperty("ISID")
		public int itemStateID;

		@JsonProperty("SCD")
		public String stateChangeDate;

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class ItemStatBlock {

		@JacksonXmlProperty(localName = "xmlns", isAttribute = true)
		public String xmlns = "";

		@JsonProperty("it")
		public int itemTier;

		@JsonProperty("iss")
		@JacksonXmlElementWrapper(useWrapping = false)
		public ItemStatData[] stats;

	}

	@JsonProperty("id")
	public IntWrapper itemPositionID;

	@JsonProperty("pid")
	public IntWrapper parentID;

	@JsonProperty("iid")
	public IntWrapper itemID;

	@JsonProperty("uicid")
	public IntWrapper itemUniqueID;

	@JsonProperty("i")
	public ItemDefData itemDef;

	@JsonProperty("uses")
	public IntWrapper uses;

	@JsonProperty("invmdate")
	public StringWrapper inventoryModificationDate;

	@JsonProperty("uis")
	public ItemStateBlock itemState;

	@JsonProperty("uia")
	public KeyValuePairSetData itemAttributes;

	@JsonProperty("uiss")
	public ItemStatBlock itemStats;

	@JsonProperty("px")
	public DoubleWrapper posX;

	@JsonProperty("py")
	public DoubleWrapper posY;

	@JsonProperty("pz")
	public DoubleWrapper posZ;

	@JsonProperty("rx")
	public DoubleWrapper rotX;

	@JsonProperty("ry")
	public DoubleWrapper rotY;

	@JsonProperty("rz")
	public DoubleWrapper rotZ;

}
