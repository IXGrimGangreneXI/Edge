package org.asf.edge.common.services.minigamedata;

import org.asf.edge.common.entities.minigamedata.MinigameData;
import org.asf.edge.common.entities.minigamedata.MinigameDataRequest;
import org.asf.edge.common.entities.minigamedata.MinigameSaveRequest;
import org.asf.edge.common.services.AbstractService;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.common.services.accounts.AccountSaveContainer;

/**
 * 
 * Minigame Data Management Service
 * 
 * @author Sky Swimmer
 *
 */
public abstract class MinigameDataManager extends AbstractService {

	/**
	 * Retrieves the minigame data service
	 * 
	 * @return MinigameDataManager instance
	 */
	public static MinigameDataManager getInstance() {
		return ServiceManager.getService(MinigameDataManager.class);
	}

	/**
	 * Deletes game data for a save
	 * 
	 * @param saveID Save ID
	 */
	public abstract void deleteDataFor(String saveID);

	/**
	 * Saves minigame data
	 * 
	 * @param save        Save container
	 * @param gameId      Minigame ID
	 * @param saveRequest Save request
	 */
	public abstract void saveGameData(AccountSaveContainer save, int gameId, MinigameSaveRequest saveRequest);

	/**
	 * Retrieves minigame data of the given save
	 * 
	 * @param save        Save container
	 * @param gameId      Minigame ID
	 * @param dataRequest Data request
	 * @return MinigameData instance (value will be 0 if not found)
	 */
	public abstract MinigameData getGameDataOf(AccountSaveContainer save, int gameId, MinigameDataRequest dataRequest);

	/**
	 * Retrieves minigame data
	 * 
	 * @param requestingID Requesting user
	 * @param gameId       Minigame ID
	 * @param dataRequest  Data request
	 * @return Array of MinigameData instances
	 */
	public abstract MinigameData[] getAllGameData(String requestingID, int gameId, MinigameDataRequest dataRequest);

}
