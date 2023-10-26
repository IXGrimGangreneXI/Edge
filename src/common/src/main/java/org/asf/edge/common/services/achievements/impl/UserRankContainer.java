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
import org.asf.edge.modules.eventbus.EventBus;

import com.google.gson.JsonPrimitive;

public class UserRankContainer extends EntityRankInfo {

	private AccountKvDataContainer data;
	private AccountSaveContainer save;
	private RankTypeID type;

	private Logger logger = LogManager.getLogger("AchievementManager");

	public UserRankContainer(RankTypeID type, AccountSaveContainer save) {
		this.type = type;
		this.save = save;

		try {
			data = save.getSaveData().getChildContainer("ranks").getChildContainer("rank-" + type.getPointTypeID());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getEntityID() {
		return save.getSaveID();
	}

	@Override
	public RankTypeID getTypeID() {
		return type;
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
			logger.info("Updated player rank " + getTypeID() + " of '" + save.getUsername() + "' (ID "
					+ save.getSaveID() + ") to " + value + " points, rank name: " + newRank.getName() + " (level "
					+ (AchievementManager.getInstance().getRankIndex(newRank) + 1) + ")");
		} catch (IOException e) {
		}
	}

	@Override
	public int addPoints(int value) {
		value = AchievementManager.getInstance().applyModifiers(save, value, type);
		setTotalScore(getTotalScore() + value);
		return value;
	}

}
