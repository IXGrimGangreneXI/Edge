package org.asf.edge.mmoserver.events.players;

import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.mmoserver.entities.smartfox.GameZone;
import org.asf.edge.mmoserver.networking.channels.system.packets.clientbound.ClientboundLoginResponsePacket;
import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Player authenticate event, called when a player authenticates with the MMO
 * server
 * 
 * @author Sky Swimmer
 * 
 */
@EventPath("players.authenticate")
public class PlayerAuthenticateEvent extends EventObject {

	private PlayerInfo player;
	private GameZone zone;

	private ClientboundLoginResponsePacket respIn;
	private boolean fail;

	public PlayerAuthenticateEvent(PlayerInfo player, GameZone zone, ClientboundLoginResponsePacket respIn) {
		this.player = player;
		this.zone = zone;
		this.respIn = respIn;
	}

	@Override
	public String eventPath() {
		return "players.authenticate";
	}

	/**
	 * Fails the authentication attempt
	 */
	public void failAuthentication() {
		fail = true;
		setHandled();
	}

	/**
	 * Retrieves the login response packet
	 * 
	 * @return ClientboundLoginResponsePacket instance
	 */
	public ClientboundLoginResponsePacket getLoginResponse() {
		return respIn;
	}

	/**
	 * Checks if the authentication attempt has been marked as failed by a module
	 * 
	 * @return True if failed, false otherwise
	 */
	public boolean hasFailed() {
		return fail;
	}

	/**
	 * Retrieves the zone the player is attempting to join
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
