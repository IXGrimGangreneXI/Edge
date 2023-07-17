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

import com.google.gson.JsonPrimitive;

public class DragonRankContainer extends EntityRankInfo {

	private AccountSaveContainer save;
	private AccountDataContainer data;
	private String dragonEntityID;

	public DragonRankContainer(AccountSaveContainer save, String dragonEntityID) {
		this.save = save;
		this.dragonEntityID = dragonEntityID;

		try {
			data = save.getSaveData().getChildContainer("dragons").getChildContainer("dragonrank-" + dragonEntityID);
		} catch (IOException e) {
			throw new RuntimeException(e);
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
