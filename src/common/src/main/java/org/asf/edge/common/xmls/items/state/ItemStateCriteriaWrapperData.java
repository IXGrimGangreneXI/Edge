package org.asf.edge.common.xmls.items.state;

import java.io.IOException;
import java.util.Map.Entry;

import org.asf.edge.common.xmls.items.state.criteria.ItemStateConsumableCriteriaData;
import org.asf.edge.common.xmls.items.state.criteria.ItemStateExpiryCriteriaData;
import org.asf.edge.common.xmls.items.state.criteria.ItemStateBaseCriteriaData;
import org.asf.edge.common.xmls.items.state.criteria.ItemStateLengthCriteriaData;
import org.asf.edge.common.xmls.items.state.criteria.ItemStateReplenishableCriteriaData;
import org.asf.edge.common.xmls.items.state.criteria.ItemStateSpeedUpCriteriaData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
@JsonSerialize(using = ItemStateCriteriaWrapperData.Serializer.class)
@JsonDeserialize(using = ItemStateCriteriaWrapperData.Deserializer.class)
public class ItemStateCriteriaWrapperData {

	@JacksonXmlProperty(isAttribute = true)
	public String objectType = "";

	public int criteriaType;

	public ItemStateBaseCriteriaData criteriaData;

	public static class Serializer extends StdSerializer<ItemStateCriteriaWrapperData> {

		private static final long serialVersionUID = 1L;

		public Serializer() {
			this(null);
		}

		public Serializer(Class<ItemStateCriteriaWrapperData> t) {
			super(t);
		}

		@Override
		public void serialize(ItemStateCriteriaWrapperData critData, JsonGenerator gen, SerializerProvider prov)
				throws IOException {
			if (gen instanceof ToXmlGenerator) {
				ObjectNode data = new ObjectMapper().convertValue(critData.criteriaData, ObjectNode.class);
				final ToXmlGenerator genXml = (ToXmlGenerator) gen;
				genXml.writeStartObject();
				genXml.setNextIsAttribute(true);
				genXml.writeFieldName("xmlns:xsi");
				genXml.writeString("http://www.w3.org/2001/XMLSchema-instance");
				genXml.writeFieldName("xsi:type");
				genXml.writeString(critData.objectType);
				genXml.setNextIsAttribute(false);
				data.put("Type", critData.criteriaType);
				for (Entry<String, JsonNode> prop : data.properties()) {
					genXml.writeFieldName(prop.getKey());
					genXml.writeObject(prop.getValue());
				}
				genXml.writeEndObject();
			} else {
				critData.criteriaData.criteriaType = critData.criteriaType;
				gen.writeObject(critData.criteriaData);
			}
		}
	}

	public static class Deserializer extends StdDeserializer<ItemStateCriteriaWrapperData> {

		private static final long serialVersionUID = 1L;

		public Deserializer() {
			this(null);
		}

		public Deserializer(Class<ItemStateCriteriaWrapperData> t) {
			super(t);
		}

		@Override
		public ItemStateCriteriaWrapperData deserialize(JsonParser parser, DeserializationContext ctxt)
				throws IOException, JacksonException {
			// Parse
			ItemStateCriteriaWrapperData wrapper = new ItemStateCriteriaWrapperData();
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
			ObjectNode data = ctxt.getParser().readValueAs(ObjectNode.class);
			wrapper.criteriaType = data.has("Type") ? data.get("Type").asInt() : -1;

			// Assign attribute value
			switch (wrapper.criteriaType) {

			case 1:
				wrapper.objectType = "ItemStateCriteriaLength";
				wrapper.criteriaData = mapper.convertValue(data, ItemStateLengthCriteriaData.class);
				break;

			case 2:
				wrapper.objectType = "ItemStateCriteriaConsumable";
				wrapper.criteriaData = mapper.convertValue(data, ItemStateConsumableCriteriaData.class);
				break;

			case 3:
				wrapper.objectType = "ItemStateCriteriaReplenishable";
				wrapper.criteriaData = mapper.convertValue(data, ItemStateReplenishableCriteriaData.class);
				break;

			case 4:
				wrapper.objectType = "ItemStateCriteriaSpeedUpItem";
				wrapper.criteriaData = mapper.convertValue(data, ItemStateSpeedUpCriteriaData.class);
				break;

			case 5:
				wrapper.objectType = "ItemStateCriteriaExpiry";
				wrapper.criteriaData = mapper.convertValue(data, ItemStateExpiryCriteriaData.class);
				break;

			}
			return wrapper;
		}

	}

}
