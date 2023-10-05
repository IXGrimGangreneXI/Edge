package org.asf.edge.gameplayapi.services.rooms;

import org.asf.edge.common.services.AbstractService;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.gameplayapi.entities.rooms.PlayerRoomInfo;

/**
 * 
 * EDGE Player Room Manager - manages player rooms such as hideouts and farms
 * 
 * @author Sky Swimmer
 * 
 */
public abstract class PlayerRoomManager extends AbstractService {

	/**
	 * Retrieves the active room manager
	 * 
	 * @return PlayerRoomManager instance
	 */
	public static PlayerRoomManager getInstance() {
		return ServiceManager.getService(PlayerRoomManager.class);
	}

	/**
	 * Retrieves all player rooms
	 * 
	 * @param save Player save
	 * @return Array of PlayerRoomInfo instances
	 */
	public abstract PlayerRoomInfo[] getPlayerRooms(AccountSaveContainer save);

	/**
	 * Creates or retrieves player rooms
	 * 
	 * @param roomID     Room ID
	 * @param categoryID Room category ID
	 * @param save       Player save
	 * @return PlayerRoomInfo instance
	 */
	public PlayerRoomInfo createOrGetRoom(String roomID, int categoryID, AccountSaveContainer save) {
		if (!roomExists(roomID, save))
			return createRoom(roomID, categoryID, save);
		return getRoom(roomID, save);
	}

	/**
	 * Creates player rooms
	 * 
	 * @param roomID     Room ID
	 * @param categoryID Room category ID
	 * @param save       Player save
	 * @return PlayerRoomInfo instance
	 */
	protected abstract PlayerRoomInfo createRoom(String roomID, int categoryID, AccountSaveContainer save);

	/**
	 * Retrieves player rooms
	 * 
	 * @param roomID Room ID
	 * @param save   Player save
	 * @return PlayerRoomInfo instance or null
	 */
	public abstract PlayerRoomInfo getRoom(String roomID, AccountSaveContainer save);

	/**
	 * Checks if player rooms exist
	 * 
	 * @param roomID Room ID
	 * @param save   Player save
	 * @return True if the room exists, false otherwise
	 */
	public abstract boolean roomExists(String roomID, AccountSaveContainer save);

}
