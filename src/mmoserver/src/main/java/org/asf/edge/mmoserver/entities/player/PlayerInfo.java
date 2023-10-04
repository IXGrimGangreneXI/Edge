package org.asf.edge.mmoserver.entities.player;

import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.mmoserver.EdgeMMOServer;
import org.asf.edge.mmoserver.entities.smartfox.GameZone;
import org.asf.edge.mmoserver.events.players.*;
import org.asf.edge.mmoserver.networking.SmartfoxClient;
import org.asf.edge.modules.eventbus.EventBus;

/**
 * 
 * Player information class
 * 
 * @author Sky Swimmer
 * 
 */
public class PlayerInfo {

	private SmartfoxClient client;
	private AccountObject account;
	private AccountSaveContainer save;
	private GameZone zone;

	private Logger logger;

	public static class PlayerDataController {
		public PlayerInfo player;
		public Runnable onJoinComplete;
	}

	public static PlayerDataController createPlayerInfo(SmartfoxClient client, AccountSaveContainer save,
			AccountObject account, GameZone zone) {
		PlayerDataController cont = new PlayerDataController();
		PlayerInfo info = new PlayerInfo(client, save, account, zone);
		cont.player = info;
		cont.onJoinComplete = () -> {
			info.onJoinZone(zone);
		};
		return cont;
	}

	private PlayerInfo(SmartfoxClient client, AccountSaveContainer save, AccountObject account, GameZone zone) {
		// Load details
		this.save = save;
		this.client = client;
		this.account = account;
		this.zone = zone;

		// Logging
		logger = client.getObject(EdgeMMOServer.class).getLogger();
	}

	/**
	 * Retrieves the player client
	 * 
	 * @return SmartfoxClient instance
	 */
	public SmartfoxClient getClient() {
		return client;
	}

	/**
	 * Retrieves the account used by this plaeyr
	 * 
	 * @return AccountObject instance
	 */
	public AccountObject getAccount() {
		return account;
	}

	/**
	 * Retrieves the account save
	 * 
	 * @return AccountSaveContainer instance
	 */
	public AccountSaveContainer getSave() {
		return save;
	}

	/**
	 * Changes the zone the player is in
	 * 
	 * @param zone New zone instance
	 */
	public void setZone(GameZone zone) {
		// Leave old zone
		onLeaveZone(this.zone);

		// Update zone
		this.zone = zone;

		// Join new zone
		onJoinZone(zone);
	}

	/**
	 * Retrieves the zone the player is connected to
	 * 
	 * @return GameZone instance
	 */
	public GameZone getZone() {
		return zone;
	}

	/**
	 * Disconnects the player
	 */
	public void disconnect() {
		// Handle disconnect preparations
		onLeaveZone(this.zone);
		// TODO

		// Close connection if needed
		if (client.isConnected())
			client.disconnect();

		// Fire disconnect
		logger.info("Player disconnected: " + account.getAccountID() + " (was " + save.getUsername() + ", ID: "
				+ save.getSaveID() + ")");
		EventBus.getInstance().dispatchEvent(new PlayerDisconnectEvent(this));
	}

	private void onJoinZone(GameZone zone) {
		// Join zone
		zone = zone;
		// TODO

		// Dispatch join zone event
		EventBus.getInstance().dispatchEvent(new PlayerJoinZoneEvent(this, this.zone));
	}

	private void onLeaveZone(GameZone zone) {
		// Leave zone
		zone = zone;
		// TODO

		// Dispatch leave zone event
		EventBus.getInstance().dispatchEvent(new PlayerLeaveZoneEvent(this, this.zone));
	}

}
