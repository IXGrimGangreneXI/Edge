package org.asf.edge.gameplayapi.xmls.minigamedata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class GameDataSummaryData {

	@JsonInclude(Include.NON_NULL)
	@JsonProperty("GameDataList")
	@JacksonXmlElementWrapper(useWrapping = false)
	public GameDataBlock[] entries;

	@JsonProperty("UserPosition")
	public int userPosition;

	@JsonProperty("GameID")
	public int gameID;

	@JsonProperty("IsMultiplayer")
	public boolean isMultiplayer;

	@JsonProperty("Difficulty")
	public int difficulty;

	@JsonProperty("Key")
	public String key;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class GameDataBlock {

		@JsonProperty("RankID")
		public int rankID = 1;

		@JsonProperty("IsMember")
		public boolean isMember;

		@JsonProperty("UserName")
		public String userName;

		@JsonProperty("UserID")
		public String userID;

		@JsonProperty("Value")
		public int value;

		@JsonProperty("DatePlayed")
		public String datePlayed;

		@JsonProperty("Loss")
		public int timesLost;

		@JsonProperty("Win")
		public int timesWon;

	}

}
