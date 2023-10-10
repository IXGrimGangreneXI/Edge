package org.asf.edge.gameplayapi.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import java.util.Random;
import org.asf.edge.common.entities.achivements.RankTypeID;
import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.entities.items.PlayerInventoryContainer;
import org.asf.edge.common.entities.items.PlayerInventoryItem;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.achievements.AchievementManager;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.xmls.achievements.AchievementRewardData;
import org.asf.edge.common.xmls.items.inventory.InventoryItemEntryData;
import org.asf.edge.common.xmls.dragons.DragonData;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * 
 * Reward Utility Class
 * 
 * @author Sky Swimmer
 * 
 */
public class RewardUtils {

	private static Random rnd = new Random();

	/**
	 * Gives rewards to the player
	 * 
	 * @param save           Save container
	 * @param rewardDefs     Reward list
	 * @param wasGivenBefore True if the rewards were given before, false otherwise
	 * @param invContainer   Inventory container ID
	 * @return Array of AchievementRewardBlock instances
	 * @throws IOException If giving rewards fails
	 */
	public static AchievementRewardData[] giveRewardsTo(AccountSaveContainer save, AchievementRewardData[] rewardDefs,
			boolean wasGivenBefore, int invContainer) throws IOException {
		// Go through rewards
		ArrayList<AchievementRewardData> rewards = new ArrayList<AchievementRewardData>();
		if (rewardDefs != null) {
			for (AchievementRewardData reward : rewardDefs) {
				if (!reward.allowMultiple && wasGivenBefore)
					continue;

				// Copy
				reward = reward.copy();
				reward.allowMultiple = false;

				// Set ID
				reward.entityID = UUID.fromString(save.getSaveID());

				// Set amount
				if (reward.minAmount != -1 && reward.maxAmount != -1) {
					reward.amount = rnd.nextInt(reward.minAmount, reward.maxAmount + 1);
					reward.minAmount = 0;
					reward.maxAmount = 0;
				}

				// Achievements
				// TODO: achievements

				// Give rewards
				switch (reward.pointTypeID) {

				// Rank points
				case 1:
				case 9:
				case 8:
				case 10:
				case 11:
				case 12: {
					// Use amount and type ID
					// Find type ID
					RankTypeID id = RankTypeID.getByTypeID(reward.pointTypeID);

					// Check
					String userID = rankUserID(save, id);
					reward.entityID = UUID.fromString(userID);
					reward.achievementID = 0;

					// Add XP
					if (userID != null)
						reward.amount = AchievementManager.getInstance().getRank(save, userID, id)
								.addPoints(reward.amount);

					break;
				}

				// Coins
				case 2: {
					// Update inventory
					AccountDataContainer currency = save.getSaveData().getChildContainer("currency");
					int currentC = 300;
					if (currency.entryExists("coins"))
						currentC = currency.getEntry("coins").getAsInt();
					currency.setEntry("coins", new JsonPrimitive(currentC + reward.amount));
					break;
				}

				// Gems
				case 5: {
					// Update inventory
					AccountDataContainer currencyAccWide = save.getAccount().getAccountData()
							.getChildContainer("currency");
					int currentG = 0;
					if (currencyAccWide.entryExists("gems"))
						currentG = currencyAccWide.getEntry("gems").getAsInt();
					currencyAccWide.setEntry("gems", new JsonPrimitive(currentG + reward.amount));
					break;
				}

				// Item
				case 6: {
					// Add item
					PlayerInventory inv = save.getInventory();
					PlayerInventoryContainer cont = inv.getContainer(invContainer);
					PlayerInventoryItem itm = cont.findFirst(reward.itemID);
					if (itm != null) {
						itm.add(reward.amount);
					} else {
						itm = cont.createItem(reward.itemID, reward.amount);
					}

					// Set data
					reward.achievementID = 0;
					reward.uniqueRewardItemID = itm.getUniqueID();

					// Add item
					InventoryItemEntryData block = new InventoryItemEntryData();
					block.itemID = itm.getItemDefID();
					block.quantity = itm.getQuantity();
					block.uses = itm.getUses();
					block.uniqueItemID = itm.getUniqueID();
					// TODO: stats and attributes

					// Add data info from item manager
					ItemInfo def = ItemManager.getInstance().getItemDefinition(block.itemID);
					if (def != null)
						block.data = def.getRawObject();

					// Set block
					reward.rewardItem = block;
					break;
				}

				}

				// Add reward
				rewards.add(reward);
			}
		}

		// Return
		return rewards.toArray(t -> new AchievementRewardData[t]);
	}

	private static String rankUserID(AccountSaveContainer save, RankTypeID id)
			throws JsonMappingException, JsonProcessingException, IOException {
		String userID = save.getSaveID();
		if (id == RankTypeID.CLAN) {
			// TODO: clan XP
			return null;
		} else if (id == RankTypeID.DRAGON) {
			// Find active dragon
			AccountDataContainer data = save.getSaveData();

			// Pull dragons
			data = data.getChildContainer("dragons");
			JsonArray dragonIds = new JsonArray();
			if (data.entryExists("dragonlist"))
				dragonIds = data.getEntry("dragonlist").getAsJsonArray();
			else
				data.setEntry("dragonlist", dragonIds);

			// Find dragon
			for (JsonElement ele : dragonIds) {
				String did = ele.getAsString();
				DragonData dragon = new XmlMapper().readValue(data.getEntry("dragon-" + did).getAsString(),
						DragonData.class);

				// Check if active
				if (dragon.isSelected) {
					// Found dragon
					userID = dragon.entityID;
					break;
				}
			}
		}
		return userID;
	}

	/**
	 * Adds rewards to the player
	 * 
	 * @param save           Save container
	 * @param rewardDefs     Reward list
	 * @param wasGivenBefore True if the rewards were given before, false otherwise
	 * @param invContainer   Inventory container ID
	 * @throws IOException If giving rewards fails
	 */
	public static void addRewards(AccountSaveContainer save, AchievementRewardData[] rewardDefs, boolean wasGivenBefore,
			int invContainer) throws IOException {
		if (rewardDefs != null) {
			for (AchievementRewardData reward : rewardDefs) {
				if (!reward.allowMultiple && wasGivenBefore)
					continue;

				// Ranks
				// TODO: achievements

				// Give rewards
				switch (reward.pointTypeID) {

				// Rank points
				case 1:
				case 9:
				case 8:
				case 10:
				case 11:
				case 12: {
					// Use amount and type ID
					// Find type ID
					RankTypeID id = RankTypeID.getByTypeID(reward.pointTypeID);

					// Check
					String userID = rankUserID(save, id);
					reward.entityID = UUID.fromString(userID);

					// Add XP
					if (userID != null)
						reward.amount = AchievementManager.getInstance().getRank(save, userID, id)
								.addPoints(reward.amount);

					break;
				}

				// Coins
				case 2: {
					// Add coins
					// Update inventory
					AccountDataContainer currency = save.getSaveData().getChildContainer("currency");
					int currentC = 300;
					if (currency.entryExists("coins"))
						currentC = currency.getEntry("coins").getAsInt();
					currency.setEntry("coins", new JsonPrimitive(currentC + reward.amount));
					break;
				}

				// Gems
				case 5: {
					// Add gems
					// Update inventory
					AccountDataContainer currencyAccWide = save.getAccount().getAccountData()
							.getChildContainer("currency");
					int currentG = 0;
					if (currencyAccWide.entryExists("gems"))
						currentG = currencyAccWide.getEntry("gems").getAsInt();
					currencyAccWide.setEntry("gems", new JsonPrimitive(currentG + reward.amount));
					break;
				}

				// Item
				case 6: {
					// Add item
					PlayerInventory inv = save.getInventory();
					PlayerInventoryContainer cont = inv.getContainer(invContainer);
					PlayerInventoryItem itm = cont.findFirst(reward.itemID);
					if (itm != null) {
						itm.add(reward.amount);
					} else {
						itm = cont.createItem(reward.itemID, reward.amount);
					}

					// Add item
					InventoryItemEntryData block = new InventoryItemEntryData();
					block.itemID = itm.getItemDefID();
					block.quantity = itm.getQuantity();
					block.uses = itm.getUses();
					block.uniqueItemID = itm.getUniqueID();
					// TODO: stats and attributes

					// Add data info from item manager
					ItemInfo def = ItemManager.getInstance().getItemDefinition(block.itemID);
					if (def != null)
						block.data = def.getRawObject();

					break;
				}

				}
			}
		}
	}

}
