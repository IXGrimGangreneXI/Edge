package org.asf.edge.gameplayapi.xmls.achievements;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class UserRankData {

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class UserRankDataWrapper {
		@JsonIgnore
		private UserRankData unwrapped;

		public UserRankData getUnwrapped() {
			if (unwrapped == null) {
				unwrapped = new UserRankData();
				unwrapped.rankID = rankID.value;
				unwrapped.rankName = rankName.value;
				unwrapped.rankDescription = rankDescription.value;
				unwrapped.audio = audio.value;
				unwrapped.image = image.value;
				unwrapped.value = value.value;
				unwrapped.pointTypeID = pointTypeID.value;
				unwrapped.globalRankID = globalRankID.value;
			}
			return unwrapped;
		}

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

		public IntWrapper rankID;

		@JsonProperty("Name")
		public StringWrapper rankName;

		@JsonProperty("Description")
		public StringWrapper rankDescription;

		public StringWrapper audio;

		public StringWrapper image;

		public IntWrapper value;

		public IntWrapper pointTypeID;

		public IntWrapper globalRankID;

	}

	public int rankID;

	public String rankName;

	public String rankDescription;

	public String audio;

	public String image;

	public int value;

	public int pointTypeID;

	public int globalRankID;

	@JsonIgnore
	public UserRankDataWrapper getWrapped() {
		UserRankDataWrapper wrapper = new UserRankDataWrapper();
		wrapper.unwrapped = this;
		wrapper.rankID = new UserRankDataWrapper.IntWrapper(rankID);
		wrapper.rankName = new UserRankDataWrapper.StringWrapper(rankName);
		wrapper.rankDescription = new UserRankDataWrapper.StringWrapper(rankDescription);
		wrapper.audio = new UserRankDataWrapper.StringWrapper(audio);
		wrapper.image = new UserRankDataWrapper.StringWrapper(image);
		wrapper.value = new UserRankDataWrapper.IntWrapper(value);
		wrapper.pointTypeID = new UserRankDataWrapper.IntWrapper(pointTypeID);
		wrapper.globalRankID = new UserRankDataWrapper.IntWrapper(globalRankID);
		return wrapper;
	}

}
