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
			// TODO: send messages to clients that the server needs updating
			event = event;
			event.cancelReconnect();
			break;
		}

		// Version mismatch
		case "connect.error.connectfailure.versionmismatch": {
			// Cancel reconnect, probably some update required
			// TODO: send messages to clients that the server needs updating
			event = event;
			event.cancelReconnect();
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
			break;
		}

		// Server full
		case "disconnect.loginfailure.fullserver": {
			break;
		}

		// Banned for unspecified reasons
		case "disconnect.loginfailure.banned.undefined": {
			// Player is banned
			// TODO: send message to clients that the server has been banned (for local
			// client it should say their account is banned)
			event = event;
			event.cancelReconnect();
			break;
		}

		// Banned
		case "disconnect.loginfailure.banned": {
			// Player is banned with reason
			// TODO: send message to clients that the server has been banned (for local
			// client it should say their account is banned), include reason (argument 1)
			event = event;
			event.cancelReconnect();
			break;
		}

		// Temporarily banned for unspecified reasons
		case "disconnect.loginfailure.tempbanned.undefined": {
			// Player is banned
			// TODO: send message to clients that the server has been banned (for local
			// client it should say their account is banned), include ban end date
			event = event;
			event.cancelReconnect();
			break;
		}

		// Temporarily banned
		case "disconnect.loginfailure.tempbanned": {
			// Player is banned with reason
			// TODO: send message to clients that the server has been banned (for local
			// client it should say their account is banned), include reason (argument 1)
			// and ban end date
			event = event;
			event.cancelReconnect();
			break;
		}

		}
	}

}
