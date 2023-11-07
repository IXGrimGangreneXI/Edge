package org.asf.edge.mmoserver.events.players;

import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.mmoserver.entities.smartfox.GameZone;
import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Player zone leave event, called when a player leaves a zone
 * 
 * @author Sky Swimmer
 * 
 */
@EventPath("players.zone.leave")
public class PlayerLeaveZoneEvent extends EventObject {

	private PlayerInfo player;
	private GameZone zone;

	public PlayerLeaveZoneEvent(PlayerInfo player, GameZone zone) {
		this.player = player;
		this.zone = zone;
	}

	@Override
	public String eventPath() {
		return "players.zone.leave";
	}

	/**
	 * Retrieves the zone
	 * 
	 * @return GameZone instance
	 */
	public GameZone getZone() {
		return zone;
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
