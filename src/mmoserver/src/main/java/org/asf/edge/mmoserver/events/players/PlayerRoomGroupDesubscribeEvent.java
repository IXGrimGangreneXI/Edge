package org.asf.edge.mmoserver.events.players;

import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.mmoserver.entities.smartfox.RoomGroup;
import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Player room group desubscribe event, called when a player leaves a room group
 * 
 * @author Sky Swimmer
 * 
 */
@EventPath("players.rooms.group.desubscribe")
public class PlayerRoomGroupDesubscribeEvent extends EventObject {

	private PlayerInfo player;
	private RoomGroup group;

	public PlayerRoomGroupDesubscribeEvent(PlayerInfo player, RoomGroup group) {
		this.player = player;
		this.group = group;
	}

	@Override
	public String eventPath() {
		return "players.rooms.group.desubscribe";
	}

	/**
	 * Retrieves the room group
	 * 
	 * @return RoomGroup instance
	 */
	public RoomGroup getRoomGroup() {
		return group;
	}

	/**
	 * Retrieves the player object
	 * 
	 * @return PlayerInfo instance
	 */
	public PlayerInfo getPlayer() {
		return player;
	}

}
