package org.asf.edge.gameplayapi.xmls.rooms;

import java.io.IOException;
import java.util.Map.Entry;

import org.asf.edge.gameplayapi.xmls.data.KeyValuePairSetData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

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
	@JsonSerialize(using = ItemDataBlockWrapper.Serializer.class)
	@JsonDeserialize(using = ItemDataBlockWrapper.Deserializer.class)
	public static class ItemDataBlockWrapper {

		public ObjectNode itemDef;

		public static class Serializer extends StdSerializer<ItemDataBlockWrapper> {

			private static final long serialVersionUID = 1L;

			public Serializer() {
				this(null);
			}

			public Serializer(Class<ItemDataBlockWrapper> t) {
				super(t);
			}

			@Override
			public void serialize(ItemDataBlockWrapper data, JsonGenerator gen, SerializerProvider prov)
					throws IOException {
				if (gen instanceof ToXmlGenerator) {
					final ToXmlGenerator genXml = (ToXmlGenerator) gen;
					genXml.writeStartObject();
					genXml.setNextIsAttribute(true);
					genXml.writeFieldName("xmlns");
					genXml.writeString("");
					genXml.setNextIsAttribute(false);
					for (Entry<String, JsonNode> prop : data.itemDef.properties()) {
						genXml.writeFieldName(prop.getKey());
						genXml.writeObject(prop.getValue());
					}
					genXml.writeEndObject();
				} else {
					gen.writeObject(data.itemDef);
				}
			}
		}

		public static class Deserializer extends StdDeserializer<ItemDataBlockWrapper> {

			private static final long serialVersionUID = 1L;

			public Deserializer() {
				this(null);
			}

			public Deserializer(Class<ItemDataBlockWrapper> t) {
				super(t);
			}

			@Override
			public ItemDataBlockWrapper deserialize(JsonParser parser, DeserializationContext ctxt)
					throws IOException, JacksonException {
				// Parse
				ItemDataBlockWrapper wrapper = new ItemDataBlockWrapper();
				wrapper.itemDef = ctxt.getParser().readValueAs(ObjectNode.class);
				return wrapper;
			}

		}

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class ItemStateBlock {

		@JacksonXmlProperty(isAttribute = true)
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

		@JacksonXmlProperty(isAttribute = true)
		public String xmlns = "";

		@JsonProperty("it")
		public int itemTier;

		@JsonProperty("iss")
		@JacksonXmlElementWrapper(useWrapping = false)
		public ItemStatBlock[] stats;

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
	public ItemDataBlockWrapper itemDef;

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
