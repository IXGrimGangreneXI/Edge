package org.asf.edge.gameplayapi.xmls.achievements;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class AchievementTaskSetResponseData {

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

	@JsonProperty("s")
	public BooleanWrapper success;

	@JsonProperty("u")
	public BooleanWrapper userMessage;

	@JsonProperty("a")
	public StringWrapper achievementName;

	@JsonProperty("l")
	public IntWrapper level;

	@JsonProperty("aid")
	public IntWrapper achievementTaskGroupID = null;

	@JsonProperty("LL")
	public StringWrapper lastLevelCompletedBoolean = null;

	@JsonProperty("aiid")
	public IntWrapper achievementInfoID = null;

}
