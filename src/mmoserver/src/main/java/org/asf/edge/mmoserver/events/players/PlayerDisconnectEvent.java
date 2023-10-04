package org.asf.edge.mmoserver.events.players;

import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Player disconnect event, called when a player disconnects from the MMO server
 * 
 * @author Sky Swimmer
 * 
 */
@EventPath("players.disconnect")
public class PlayerDisconnectEvent extends EventObject {

	private PlayerInfo player;

	public PlayerDisconnectEvent(PlayerInfo player) {
		this.player = player;
	}

	@Override
	public String eventPath() {
		return "players.disconnect";
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
