package org.asf.edge.mmoserver.events.players;

import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.mmoserver.entities.smartfox.RoomGroup;
import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Player room group subscribe event, called when a player joins a room group
 * 
 * @author Sky Swimmer
 * 
 */
@EventPath("players.rooms.group.subscribe")
public class PlayerRoomGroupSubscribeEvent extends EventObject {

	private PlayerInfo player;
	private RoomGroup group;

	public PlayerRoomGroupSubscribeEvent(PlayerInfo player, RoomGroup group) {
		this.player = player;
		this.group = group;
	}

	@Override
	public String eventPath() {
		return "players.rooms.group.subscribe";
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
