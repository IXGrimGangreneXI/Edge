package org.asf.edge.gameplayapi.xmls.achievements.leaderboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class UdtLeaderboardResponseData {

	@JsonInclude(Include.NON_NULL)
	@JsonProperty("UAI")
	@JacksonXmlElementWrapper(useWrapping = false)
	public UdtLeaderboardEntryBlock[] entries;

	@JsonProperty("DR")
	public UdtDateRangeBlock dateRange;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class UdtDateRangeBlock {

		@JsonProperty("SD")
		public String dateStart;

		@JsonProperty("ED")
		public String dateEnd;

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class UdtLeaderboardEntryBlock {

		@JsonProperty("u")
		public String userID;

		@JsonProperty("n")
		public String userName;

		@JsonProperty("a")
		public int pointsTotal;

		@JsonProperty("p")
		public int pointTypeID;

	}

}
