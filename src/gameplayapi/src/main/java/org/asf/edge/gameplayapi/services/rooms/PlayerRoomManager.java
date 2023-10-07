package org.asf.edge.gameplayapi.services.rooms;

import org.asf.edge.common.services.AbstractService;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.gameplayapi.entities.rooms.PlayerRoomInfo;
import org.asf.edge.gameplayapi.entities.rooms.RoomItemInfo;

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
	public abstract PlayerRoomInfo[] getRooms(AccountSaveContainer save);

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

	/**
	 * Checks if room items exists
	 * 
	 * @param itemID Room item ID
	 * @param save   Player save
	 * @return True if the room exists, false otherwise
	 */
	public abstract boolean roomItemExists(int itemID, AccountSaveContainer save);

	/**
	 * Deletes room items
	 * 
	 * @param itemID Room item ID
	 * @param save   Player save
	 */
	public abstract void deleteRoomItem(int itemID, AccountSaveContainer save);

	/**
	 * Retrieves room items by ID
	 * 
	 * @param itemID Room item ID
	 * @param save   Player save
	 * @return RoomItemInfo instance or null
	 */
	public abstract RoomItemInfo getRoomItem(int itemID, AccountSaveContainer save);

	/**
	 * Saves room items
	 * 
	 * @param item Item to save
	 * @param save Player save
	 */
	public abstract void setRoomItem(RoomItemInfo item, AccountSaveContainer save);

	/**
	 * Creates room items
	 * 
	 * @param item Item data to use
	 * @param save Player save
	 * @return RoomItemInfo instance with new item ID
	 */
	public abstract RoomItemInfo createRoomItem(RoomItemInfo item, AccountSaveContainer save);

}
