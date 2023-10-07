package org.asf.edge.gameplayapi.entities.rooms;

import java.util.Optional;
import java.util.stream.Stream;

import org.asf.edge.common.entities.achivements.RankTypeID;
import org.asf.edge.common.entities.coordinates.Vector3D;
import org.asf.edge.common.entities.items.ItemInfo;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.achievements.AchievementManager;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.xmls.data.KeyValuePairSetData;
import org.asf.edge.common.xmls.items.ItemDefData;
import org.asf.edge.common.xmls.items.state.ItemStateCriteriaWrapperData;
import org.asf.edge.common.xmls.items.state.ItemStateData;
import org.asf.edge.common.xmls.items.state.criteria.ItemStateReplenishableCriteriaData;
import org.asf.edge.common.xmls.items.state.criteria.ItemStateReplenishableCriteriaData.ReplenishRateBlock;
import org.asf.edge.gameplayapi.xmls.rooms.RoomItemData.ItemStatBlock;

/**
 * 
 * Room item information container
 * 
 * @author Sky swimmer
 * 
 */
public class RoomItemInfo {

	public String roomID = null;

	public int roomItemID = -1;
	public int parentID = -1;

	public int itemID = -1;
	public int itemUniqueID = -1;

	public int uses = 0;

	public String inventoryModificationDate;

	public int currentStateID = -1;
	public long lastStateChange;

	public KeyValuePairSetData itemAttributes;
	public ItemStatBlock itemStats;

	public Vector3D position;
	public Vector3D rotation;

	public int getCurrentUses(AccountSaveContainer save) {
		// Find state
		ItemInfo i = ItemManager.getInstance().getItemDefinition(itemID);
		if (i == null)
			return uses;
		ItemDefData def = i.getRawObject();
		if (def.states != null) {
			Optional<ItemStateData> stateO = Stream.of(def.states).filter(t -> t.stateID == currentStateID).findFirst();
			if (stateO.isPresent()) {
				ItemStateData state = stateO.get();

				// Find replenishing info
				if (state.rules != null) {
					for (ItemStateCriteriaWrapperData criteria : state.rules.criteria) {
						if (criteria.criteriaData instanceof ItemStateReplenishableCriteriaData) {
							ItemStateReplenishableCriteriaData crit = (ItemStateReplenishableCriteriaData) criteria.criteriaData;

							// Find replenish rate
							for (ReplenishRateBlock block : crit.replenishRates) {
								// Check rank
								if (crit.useRank) {
									AchievementManager manager = AchievementManager.getInstance();
									int value = manager.getRankIndex(manager.getRank(save, save.getSaveID(),
											RankTypeID.getByTypeID(crit.rankPointTypeID)).getRank()) + 1;

									// Check
									if (value < block.rankLevel)
										continue; // No match
								}

								// Found match
								double period = block.increasePeriod * 1000d;
								long timePassed = System.currentTimeMillis() - lastStateChange;
								int addedUses = (int) (block.usesPerPeriod * (timePassed / period));
								if (addedUses < 0)
									addedUses = 0;
								int newUses = uses + addedUses;
								if (newUses > block.usesLimit)
									newUses = block.usesLimit;
								return newUses;
							}
						}
					}
				}
			}
		}
		return uses;
	}

}
