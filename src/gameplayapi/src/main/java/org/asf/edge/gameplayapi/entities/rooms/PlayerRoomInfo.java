package org.asf.edge.gameplayapi.entities.rooms;

/**
 * 
 * Player room information container
 * 
 * @author Sky Swimmer
 * 
 */
public abstract class PlayerRoomInfo {

	/**
	 * Retrieves the room ID
	 * 
	 * @return Room ID
	 */
	public abstract String getID();

	/**
	 * Retrieves the room category ID
	 * 
	 * @return Room category ID
	 */
	public abstract int getCategoryID();

	/**
	 * Retrieves the room creative points
	 * 
	 * @return Room creative points
	 */
	public abstract double getCreativePoints();

	/**
	 * Retrieves the room item ID
	 * 
	 * @return Room item ID
	 */
	public abstract int getItemID();

	/**
	 * Retrieves the room name
	 * 
	 * @return Room name
	 */
	public abstract String getName();

	/**
	 * Retrieves the room items
	 * 
	 * @return Array of RoomItemInfo instances
	 */
	public abstract RoomItemInfo[] getItems();

	/**
	 * Updates room items
	 * 
	 * @param items New room item list
	 */
	public abstract void setItems(RoomItemInfo[] items);

	/**
	 * Updates the room name
	 * 
	 * @param newName New room name
	 */
	public abstract void setName(String newName);

	/**
	 * Updates the room item ID
	 * 
	 * @param newID New room item ID
	 */
	public abstract void setItemID(int newID);

	/**
	 * Updates the room creative point count
	 * 
	 * @param newPoints New room creative points
	 */
	public abstract void setCreativePoints(double newPoints);

}
