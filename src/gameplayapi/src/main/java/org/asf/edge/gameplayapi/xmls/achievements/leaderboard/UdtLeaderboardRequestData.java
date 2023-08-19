package org.asf.edge.gameplayapi.xmls.achievements.leaderboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class UdtLeaderboardRequestData {

	// 1 = All time, 2 = Daily, 3 = Weekly, 4 = Monthly
	@JsonProperty("M")
	public int mode;

	@JsonProperty("P")
	public int pageNumber;

	@JsonProperty("Q")
	public int pageSize;

	@JsonProperty("PTID")
	public int pointTypeID;

}
