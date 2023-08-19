package org.asf.edge.common.services.achievements.impl;

import java.io.IOException;

import org.asf.edge.common.entities.achivements.EntityRankInfo;
import org.asf.edge.common.entities.achivements.RankInfo;
import org.asf.edge.common.entities.achivements.RankTypeID;
import org.asf.edge.common.events.achievements.RankChangedEvent;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.achievements.AchievementManager;
import org.asf.edge.modules.eventbus.EventBus;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class DragonRankContainer extends EntityRankInfo {

	private AccountSaveContainer save;
	private AccountDataContainer data;
	private String dragonEntityID;

	public DragonRankContainer(AccountSaveContainer save, String dragonEntityID) throws IOException {
		this.save = save;
		this.dragonEntityID = dragonEntityID;

		try {
			data = save.getSaveData().getChildContainer("dragons").getChildContainer("dragonrank-" + dragonEntityID);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// Check dragon existence
		if (!data.entryExists("total")) {
			// Find dragon
			AccountDataContainer d = save.getSaveData().getChildContainer("dragons");
			JsonArray dragonIds;
			if (d.entryExists("dragonlist"))
				dragonIds = d.getEntry("dragonlist").getAsJsonArray();
			else
				throw new IOException("Dragon not found");
			boolean found = false;
			for (JsonElement ele : dragonIds) {
				String did = ele.getAsString();
				ObjectNode dragon = new XmlMapper().readValue(data.getEntry("dragon-" + did).getAsString(),
						ObjectNode.class);
				String id = dragon.get("eid").asText();
				if (id.equals(dragonEntityID)) {
					found = true;
					break;
				}
			}
			if (!found)
				throw new IOException("Dragon not found");
		}
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
