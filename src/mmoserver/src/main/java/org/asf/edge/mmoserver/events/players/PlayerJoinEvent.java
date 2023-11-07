package org.asf.edge.mmoserver.events.players;

import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Player join event, called when a player successfully joins the server
 * 
 * @author Sky Swimmer
 * 
 */
@EventPath("players.join")
public class PlayerJoinEvent extends EventObject {

	private PlayerInfo player;

	public PlayerJoinEvent(PlayerInfo player) {
		this.player = player;
	}

	@Override
	public String eventPath() {
		return "players.join";
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
