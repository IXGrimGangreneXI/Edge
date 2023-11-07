package org.asf.edge.mmoserver.events.players;

import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.mmoserver.entities.smartfox.RoomInfo;
import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Player room leave event, called when a player leaves a room
 * 
 * @author Sky Swimmer
 * 
 */
@EventPath("players.rooms.leave spectator")
public class PlayerRoomLeaveEvent extends EventObject {

	private PlayerInfo player;
	private RoomInfo room;

	public PlayerRoomLeaveEvent(PlayerInfo player, RoomInfo room) {
		this.player = player;
		this.room = room;
	}

	@Override
	public String eventPath() {
		return "players.rooms.leave spectator";
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
