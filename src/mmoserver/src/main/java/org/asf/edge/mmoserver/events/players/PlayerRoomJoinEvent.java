package org.asf.edge.mmoserver.events.players;

import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.mmoserver.entities.smartfox.RoomInfo;
import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Player room join event, called when a player joins a room
 * 
 * @author Sky Swimmer
 * 
 */
@EventPath("players.rooms.join")
public class PlayerRoomJoinEvent extends EventObject {

	private PlayerInfo player;
	private RoomInfo room;

	public PlayerRoomJoinEvent(PlayerInfo player, RoomInfo room) {
		this.player = player;
		this.room = room;
	}

	@Override
	public String eventPath() {
		return "players.rooms.join";
	}

	/**
	 * Retrieves the room instance
	 * 
	 * @return RoomInfo instance
	 */
	public RoomInfo getRoom() {
		return room;
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
