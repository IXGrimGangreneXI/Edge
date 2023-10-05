package org.asf.edge.gameplayapi.entities.rooms;

import org.asf.edge.common.entities.coordinates.Vector3D;
import org.asf.edge.gameplayapi.xmls.data.KeyValuePairSetData;
import org.asf.edge.gameplayapi.xmls.rooms.RoomItemData.ItemStatBlock;

/**
 * 
 * Room item information container
 * 
 * @author Sky swimmer
 * 
 */
public class RoomItemInfo {

	public int roomItemID = -1;
	public int parentID = -1;

	public int itemID = -1;
	public int itemUniqueID = -1;

	public int uses = -1;

	public String inventoryModificationDate;

	public int currentStateID = -1;
	public long lastStateChange;

	public KeyValuePairSetData itemAttributes;
	public ItemStatBlock itemStats;

	public Vector3D position;
	public Vector3D rotation;

}
