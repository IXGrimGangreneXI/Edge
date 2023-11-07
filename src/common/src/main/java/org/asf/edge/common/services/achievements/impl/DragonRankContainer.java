package org.asf.edge.common.services.achievements.impl;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.entities.achivements.EntityRankInfo;
import org.asf.edge.common.entities.achivements.RankInfo;
import org.asf.edge.common.entities.achivements.RankTypeID;
import org.asf.edge.common.events.achievements.RankChangedEvent;
import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.achievements.AchievementManager;
import org.asf.nexus.events.EventBus;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class DragonRankContainer extends EntityRankInfo {

	private AccountSaveContainer save;
	private AccountKvDataContainer data;
	private String dragonEntityID;
	private String dragonNumericID;
	private String lastName;

	private Logger logger = LogManager.getLogger("AchievementManager");

	public DragonRankContainer(AccountSaveContainer save, String dragonEntityID) throws IOException {
		this.save = save;
		this.dragonEntityID = dragonEntityID;

		try {
			data = save.getSaveData().getChildContainer("dragons").getChildContainer("dragonrank-" + dragonEntityID);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// Find dragon
		AccountKvDataContainer d = save.getSaveData().getChildContainer("dragons");
		JsonArray dragonIds;
		if (d.entryExists("dragonlist"))
			dragonIds = d.getEntry("dragonlist").getAsJsonArray();
		else
			throw new IOException("Dragon not found");
		boolean found = false;
		for (JsonElement ele : dragonIds) {
			String did = ele.getAsString();
			if (d.entryExists("dragon-" + did)) {
				ObjectNode dragon = new XmlMapper().readValue(d.getEntry("dragon-" + did).getAsString(),
						ObjectNode.class);
				String id = dragon.get("eid").asText();
				if (id.equals(dragonEntityID)) {
					found = true;
					dragonNumericID = did;
					lastName = dragon.get("n").asText();
					break;
				}
			}
		}
		if (!found)
			throw new IOException("Dragon not found");
	}

	@Override
	public String getEntityID() {
		return dragonEntityID;
	}

	@Override
	public RankTypeID getTypeID() {
		return RankTypeID.DRAGON;
	}

	@Override
	public int getTotalScore() {
		try {
			if (data.entryExists("total"))
				return data.getEntry("total").getAsInt();
			return 0;
		} catch (IOException e) {
			return 0;
		}
	}

	@Override
	public void setTotalScore(int value) {
		try {
			// Get current score
			int current = getTotalScore();
			RankInfo currentRank = getRank();

			// Set score
			data.setEntry("total", new JsonPrimitive(value));

			// Get new rank
			RankInfo newRank = getRank();

			// Dispatch event
			EventBus.getInstance().dispatchEvent(new RankChangedEvent(AchievementManager.getInstance(), save, this,
					currentRank, newRank, current, value));

			// Log
			try {
				AccountKvDataContainer d = save.getSaveData().getChildContainer("dragons");
				if (d.entryExists("dragon-" + dragonNumericID)) {
					ObjectNode dragon = new XmlMapper().readValue(d.getEntry("dragon-" + dragonNumericID).getAsString(),
							ObjectNode.class);
					String id = dragon.get("eid").asText();
					if (id.equals(dragonEntityID)) {
						lastName = dragon.get("n").asText();
					}
				}
			} catch (IOException e) {
			}
			logger.info("Updated dragon rank " + getTypeID() + " of '" + lastName + "' (owner " + save.getUsername()
					+ ", ID " + save.getSaveID() + ") to " + value + " points, rank name: " + newRank.getName()
					+ " (level " + (AchievementManager.getInstance().getRankIndex(newRank) + 1) + ")");
		} catch (IOException e) {
		}
	}

	@Override
	public int addPoints(int value) {
		value = AchievementManager.getInstance().applyModifiers(save, value, RankTypeID.DRAGON);
		setTotalScore(getTotalScore() + value);
		return value;
	}

}
