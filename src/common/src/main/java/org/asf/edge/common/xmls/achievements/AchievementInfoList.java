package org.asf.edge.common.xmls.achievements;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class AchievementInfoList {

	@JacksonXmlProperty(localName = "xmlns", isAttribute = true)
	private final String xmlns = "http://api.jumpstart.com/";

	@JsonProperty("UserAchievementInfo")
	@JacksonXmlElementWrapper(useWrapping = false)
	public AchievementBlock[] achievements;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class AchievementBlock {

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

		@JsonProperty("u")
		public StringWrapper userID;

		@JsonProperty("n")
		public StringWrapper saveName;

		@JsonProperty("a")
		public IntWrapper pointsTotal;

		@JsonProperty("r")
		public IntWrapper rank;

		@JsonProperty("p")
		public IntWrapper pointTypeID;

	}
}
