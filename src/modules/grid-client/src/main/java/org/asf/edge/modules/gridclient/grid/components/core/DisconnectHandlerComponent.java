package org.asf.edge.modules.gridclient.grid.components.core;

import org.asf.edge.modules.gridclient.grid.GridClientComponent;
import org.asf.edge.modules.gridclient.grid.events.GridClientDisconnectedEvent;
import org.asf.edge.modules.gridclient.phoenix.PhoenixClient;

public class DisconnectHandlerComponent extends GridClientComponent {

	@Override
	public String componentID() {
		return "core-disconnect-handler";
	}

	@Override
	public void addToClient(PhoenixClient client) {
	}

	@Override
	public void disconnect(GridClientDisconnectedEvent event) {
		// Handle failure
		switch (event.getDisconnectReason().getReason()) {

		// Duplicate login
		case "disconnect.duplicatelogin": {
			// Verify session
			if (GridClient.verifyGridConnection() && !GridClient.getLoginManager().getSession().refresh()) {
				// Cancel reconnect, our session is invalid
				event.cancelReconnect();
			}
			break;
		}

		}
	}

}
