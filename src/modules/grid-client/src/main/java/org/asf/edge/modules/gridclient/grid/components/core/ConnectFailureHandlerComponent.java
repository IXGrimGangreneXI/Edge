package org.asf.edge.modules.gridclient.grid.components.core;

import org.asf.edge.modules.gridclient.grid.GridClientComponent;
import org.asf.edge.modules.gridclient.grid.events.GridClientConnectFailedEvent;
import org.asf.edge.modules.gridclient.phoenix.PhoenixClient;

public class ConnectFailureHandlerComponent extends GridClientComponent {

	@Override
	public String componentID() {
		return "core-connect-failure-handler";
	}

	@Override
	public void addToClient(PhoenixClient client) {
	}

	@Override
	public void connectFailed(GridClientConnectFailedEvent event) {
		// Handle failure
		switch (event.getDisconnectReason().getReason()) {

		// Game ID mismatch
		case "connect.error.connectfailure.gamemismatch": {
			// Cancel reconnect, probably some update required
			event.cancelReconnect();
			getLogger().error(
					"Game ID mismatch while connecting to the Grid servers, please verify if the server software is up to date.");
			break;
		}

		// Version mismatch
		case "connect.error.connectfailure.versionmismatch": {
			// Cancel reconnect, probably some update required
			event.cancelReconnect();
			getLogger().error(
					"Version mismatch while connecting to the Grid servers, please verify if the server software is up to date.");
			break;
		}

		// Authentication failure, phoenix being unreachable may also be a invalid
		// session token from the client, but likely just server being unable to reach
		// Phoenix, either way we should check the session.
		case "disconnect.loginfailure.phoenixunreachable":
		case "disconnect.loginfailure.authfailure": {
			// Lets attempt refreshing the session...
			if (GridClient.verifyGridConnection() && !GridClient.getLoginManager().getSession().refresh()) {
				// Cancel reconnect, our session is invalid, which would explain it
				event.cancelReconnect();
			}
			getLogger().error("Failed to connect to the Grid servers due to a session error.");
			break;
		}

		// Server full
		case "disconnect.loginfailure.fullserver": {
			getLogger().error("Failed to connect to the Grid server, server is full.");
			break;
		}

		// Banned for unspecified reasons
		case "disconnect.loginfailure.banned.undefined": {
			// Player is banned
			event.cancelReconnect();
			getLogger().error(
					"Failed to connect to the Grid server, the account used to connect to the Grid servers was permanently banned.");
			break;
		}

		// Banned
		case "disconnect.loginfailure.banned": {
			// Player is banned with reason
			event.cancelReconnect();
			getLogger().error(
					"Failed to connect to the Grid server, the account used to connect to the Grid servers was permanently banned.");
			getLogger().error("Ban reason: " + event.getDisconnectReason().getReasonArgs()[0]);
			break;
		}

		// Temporarily banned for unspecified reasons
		case "disconnect.loginfailure.tempbanned.undefined": {
			// Player is banned
			event.cancelReconnect();
			getLogger().error(
					"Failed to connect to the Grid server, the account used to connect to the Grid servers was banned.");
			getLogger().error("The account will be unbanned at: " + event.getDisconnectReason().getReasonArgs()[0] + " "
					+ event.getDisconnectReason().getReasonArgs()[1] + "UTC.");
			break;
		}

		// Temporarily banned
		case "disconnect.loginfailure.tempbanned": {
			// Player is banned with reason
			event.cancelReconnect();
			getLogger().error(
					"Failed to connect to the Grid server, the account used to connect to the Grid servers was banned.");
			getLogger().error("The account will be unbanned at: " + event.getDisconnectReason().getReasonArgs()[0] + " "
					+ event.getDisconnectReason().getReasonArgs()[1] + "UTC.");
			getLogger().error("Ban reason: " + event.getDisconnectReason().getReasonArgs()[2]);
			break;
		}

		}
	}

}
