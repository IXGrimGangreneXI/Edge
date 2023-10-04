package org.asf.edge.commonapi.xmls.servers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class MMOServerInfoBlock {

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

	public static class BooleanWrapper {
		public BooleanWrapper() {
		}

		public BooleanWrapper(boolean value) {
			this.value = value;
		}

		@JacksonXmlProperty(isAttribute = true)
		public String xmlns = "";

		@JacksonXmlText
		public boolean value;
	}

	@JsonProperty("IP")
	public StringWrapper serverAddress;

	@JsonProperty("PN")
	public IntWrapper port;

	@JsonProperty("VR")
	public StringWrapper protocolVersion = new StringWrapper("S2X");

	@JsonProperty("DF")
	public BooleanWrapper isDefault;

	@JsonProperty("ZN")
	public StringWrapper zoneName;

	@JsonProperty("RZN")
	public StringWrapper rootZoneName;

}
