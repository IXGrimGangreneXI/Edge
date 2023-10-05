package org.asf.edge.common.xmls.items.state;

import java.io.IOException;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
@JsonSerialize(using = ItemStateCriteriaData.Serializer.class)
@JsonDeserialize(using = ItemStateCriteriaData.Deserializer.class)
public class ItemStateCriteriaData {

	@JacksonXmlProperty(isAttribute = true)
	public String objectType = "";

	public int criteriaType;

	public ObjectNode criteriaData;

	public static class Serializer extends StdSerializer<ItemStateCriteriaData> {

		private static final long serialVersionUID = 1L;

		public Serializer() {
			this(null);
		}

		public Serializer(Class<ItemStateCriteriaData> t) {
			super(t);
		}

		@Override
		public void serialize(ItemStateCriteriaData data, JsonGenerator gen, SerializerProvider prov)
				throws IOException {
			if (gen instanceof ToXmlGenerator) {
				final ToXmlGenerator genXml = (ToXmlGenerator) gen;
				genXml.writeStartObject();
				genXml.setNextIsAttribute(true);
				genXml.writeFieldName("xmlns:xsi");
				genXml.writeString("http://www.w3.org/2001/XMLSchema-instance");
				genXml.writeFieldName("xsi:type");
				genXml.writeString(data.objectType);
				genXml.setNextIsAttribute(false);
				data.criteriaData.put("Type", data.criteriaType);
				for (Entry<String, JsonNode> prop : data.criteriaData.properties()) {
					genXml.writeFieldName(prop.getKey());
					genXml.writeObject(prop.getValue());
				}
				genXml.writeEndObject();
			} else {
				data.criteriaData.put("Type", data.criteriaType);
				gen.writeObject(data.criteriaData);
			}
		}
	}

	public static class Deserializer extends StdDeserializer<ItemStateCriteriaData> {

		private static final long serialVersionUID = 1L;

		public Deserializer() {
			this(null);
		}

		public Deserializer(Class<ItemStateCriteriaData> t) {
			super(t);
		}

		@Override
		public ItemStateCriteriaData deserialize(JsonParser parser, DeserializationContext ctxt)
				throws IOException, JacksonException {
			// Parse
			ItemStateCriteriaData wrapper = new ItemStateCriteriaData();
			wrapper.criteriaData = ctxt.getParser().readValueAs(ObjectNode.class);
			wrapper.criteriaType = wrapper.criteriaData.has("Type") ? wrapper.criteriaData.get("Type").asInt() : -1;

			// Assign attribute value
			switch (wrapper.criteriaType) {

			case 1:
				wrapper.objectType = "ItemStateCriteriaLength";
				break;

			case 2:
				wrapper.objectType = "ItemStateCriteriaConsumable";
				break;

			case 3:
				wrapper.objectType = "ItemStateCriteriaReplenishable";
				break;

			case 4:
				wrapper.objectType = "ItemStateCriteriaSpeedUpItem";
				break;

			case 5:
				wrapper.objectType = "ItemStateCriteriaExpiry";
				break;

			}
			return wrapper;
		}

	}

}
