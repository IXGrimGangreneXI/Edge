package org.asf.edge.gameplayapi.xmls.achievements;

import java.util.UUID;

import org.asf.edge.gameplayapi.xmls.inventories.CommonInventoryData.ItemBlock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class AchievementRewardBlock {

	// 1, 9, 10, 12: achievement points, 2: coins, 5: gems, 6: item, 8: dragon XP
	@JsonProperty("ai")
	public int achievementID;

	@JsonProperty("ii")
	public int itemID;

	@JsonProperty("a")
	public int amount;

	@JsonProperty("p")
	@JsonInclude(Include.NON_DEFAULT)
	public int pointTypeID = -1;

	@JsonProperty("i")
	public UUID entityID;

	@JsonProperty("t")
	public int entityTypeID;

	@JsonProperty("r")
	public int rewardID;

	@JsonProperty("amulti")
	public boolean allowMultiple;

	@JsonProperty("mina")
	@JsonInclude(Include.NON_DEFAULT)
	public int minAmount = -1;

	@JsonProperty("maxa")
	@JsonInclude(Include.NON_DEFAULT)
	public int maxAmount = -1;

	@JsonProperty("cid")
	public int uniqueRewardItemID = 0;

	@JsonProperty("ui")
	public ItemBlock rewardItem;

	@JsonProperty("d")
	public String date = null;

	public AchievementRewardBlock copy() {
		AchievementRewardBlock cp = new AchievementRewardBlock();
		cp.achievementID = achievementID;
		cp.itemID = itemID;
		cp.amount = amount;
		cp.pointTypeID = pointTypeID;
		cp.entityID = entityID;
		cp.entityTypeID = entityTypeID;
		cp.rewardID = rewardID;
		cp.allowMultiple = allowMultiple;
		cp.minAmount = minAmount;
		cp.maxAmount = maxAmount;
		return cp;
	}

}
