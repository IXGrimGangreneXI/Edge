package org.asf.edge.common.services.achievements;

import java.util.ArrayList;
import java.util.List;

import org.asf.edge.common.entities.achivements.EntityRankInfo;
import org.asf.edge.common.entities.achivements.RankInfo;
import org.asf.edge.common.entities.achivements.RankMultiplierInfo;
import org.asf.edge.common.entities.achivements.RankTypeID;
import org.asf.edge.common.services.AbstractService;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.xmls.achievements.UserRankData;

/**
 * 
 * Edge achievement manager - manages achievements, XP and ranks
 * 
 * @author Sky Swimmer
 *
 */
public abstract class AchievementManager extends AbstractService {

	/**
	 * Retrieves the active achievement manager
	 * 
	 * @return AchievementManager instance
	 */
	public static AchievementManager getInstance() {
		return ServiceManager.getService(AchievementManager.class);
	}

	/**
	 * Retrieves rank definitions
	 * 
	 * @return Array of RankInfo instances
	 */
	public abstract RankInfo[] getRankDefinitions();

	/**
	 * Retrieves rank definitions by point type ID
	 * 
	 * @param pointTypeID Point type ID
	 * @return Array of RankInfo instances
	 */
	public abstract RankInfo[] getRankDefinitionsByPointType(int pointTypeID);

	/**
	 * Retrieves rank definitions
	 * 
	 * @param id Rank ID
	 * @return RankInfo instance or null
	 */
	public abstract RankInfo getRankDefinition(int id);

	/**
	 * Registers rank definitions
	 * 
	 * @param id  Rank ID
	 * @param def Rank definition
	 */
	public abstract void registerRankDefinition(int id, UserRankData def);

	/**
	 * Updates rank definitions
	 * 
	 * @param id  Rank ID
	 * @param def Updated definition
	 */
	public abstract void updateRankDefinition(int id, UserRankData def);

	/**
	 * Retrieves rank objects by type ID
	 * 
	 * @param save Account save
	 * @param type Rank type ID
	 * @return {@link EntityRankInfo} instance
	 */
	public abstract EntityRankInfo getRankForUser(AccountSaveContainer save, RankTypeID type);

	/**
	 * Retrieves rank objects by dragon ID
	 * 
	 * @param save           Account save
	 * @param dragonEntityID Dragon entity ID
	 * @return {@link EntityRankInfo} instance
	 */
	public abstract EntityRankInfo getRankForDragon(AccountSaveContainer save, String dragonEntityID);

	/**
	 * Retrieves rank objects by clan ID
	 * 
	 * @param save   Account save
	 * @param clanID Clan ID
	 * @return {@link EntityRankInfo} instance
	 */
	public abstract EntityRankInfo getRankForClan(AccountSaveContainer save, String clanID);

	/**
	 * Retrieves rank objects (does not support clans)
	 * 
	 * @param save     Account save
	 * @param entityID Entity ID
	 * @return Array of EntityRankInfo instance
	 */
	public EntityRankInfo[] getRanks(AccountSaveContainer save, String entityID) {
		ArrayList<EntityRankInfo> ranks = new ArrayList<EntityRankInfo>();
		if (save.getSaveID().equals(entityID)) {
			for (RankTypeID type : RankTypeID.values())
				if (type != RankTypeID.DRAGON && type != RankTypeID.CLAN)
					ranks.add(getRank(save, entityID, type));
		} else
			ranks.add(getRank(save, entityID, RankTypeID.DRAGON));
		return ranks.toArray(t -> new EntityRankInfo[t]);
	}

	/**
	 * Retrieves rank objects
	 * 
	 * @param save     Account save
	 * @param entityID Entity ID
	 * @param type     Rank type ID
	 * @return EntityRankInfo instance
	 */
	public EntityRankInfo getRank(AccountSaveContainer save, String entityID, RankTypeID type) {
		if (type == RankTypeID.DRAGON)
			return getRankForDragon(save, entityID);
		else if (type == RankTypeID.CLAN)
			return getRankForClan(save, entityID);
		else
			return getRankForUser(save, type);
	}

	/**
	 * Retrieves the rank index
	 * 
	 * @param rank Rank object
	 * @return Rank index
	 */
	public int getRankIndex(RankInfo rank) {
		if (rank == null)
			return -1;

		int ind = 0;
		for (RankInfo r : getRankDefinitionsByPointType(rank.getPointTypeID())) {
			if (r.getID() == rank.getID()) {
				return ind;
			}
			ind++;
		}
		return -1;
	}

	/**
	 * Retrieves server-wide rank multipliers
	 * 
	 * @return Array of RankMultiplierInfo instances
	 */
	public abstract RankMultiplierInfo[] getServerwideRankMultipliers();

	/**
	 * Retrieves user-specific rank multipliers
	 * 
	 * @param save User save
	 * @return Array of RankMultiplierInfo instances
	 */
	public abstract RankMultiplierInfo[] getUserRankMultipliers(AccountSaveContainer save);

	/**
	 * Retrieves all rank multipliers (retrieves both user and server-wide
	 * multipliers)
	 * 
	 * @param save User save
	 * @return Array of RankMultiplierInfo instances
	 */
	public RankMultiplierInfo[] getRankMultipliers(AccountSaveContainer save) {
		ArrayList<RankMultiplierInfo> multipliers = new ArrayList<RankMultiplierInfo>();
		multipliers.addAll(List.of(getServerwideRankMultipliers()));
		multipliers.addAll(List.of(getUserRankMultipliers(save)));
		return multipliers.toArray(t -> new RankMultiplierInfo[t]);
	}

	/**
	 * Called to apply all rank modifiers
	 * 
	 * @param save  User save
	 * @param value Point value
	 * @param type  Type ID
	 * @return Modified point value
	 */
	public abstract int applyModifiers(AccountSaveContainer save, int value, RankTypeID type);

	/**
	 * Called to reload achievement and rank data from disk
	 */
	public abstract void reload();

}
