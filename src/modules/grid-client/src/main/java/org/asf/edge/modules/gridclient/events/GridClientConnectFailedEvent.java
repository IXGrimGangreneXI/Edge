package org.asf.edge.modules.gridclient.events;

import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;
import org.asf.edge.modules.gridclient.phoenix.DisconnectReason;
import org.asf.edge.modules.gridclient.phoenix.PhoenixClient;

/**
 * 
 * Grid Client Connection Failure Event - called when the Grid client fails to
 * connect
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("grid.client.connectfailed")
public class GridClientConnectFailedEvent extends EventObject {

	private PhoenixClient client;
	private DisconnectReason disconnectReason;
	private boolean attemptReconnect = true;

	public GridClientConnectFailedEvent(PhoenixClient client, DisconnectReason disconnectReason) {
		this.client = client;
		this.disconnectReason = disconnectReason;
	}

	/**
	 * Cancels reconnecte attempts
	 */
	public void cancelReconncet() {
		setHandled();
		attemptReconnect = false;
	}

	/**
	 * Checks if the client should attempt reconnects
	 * 
	 * @return True if the client should try to reconnect, false otherwise
	 */
	public boolean shouldAttemptReconnect() {
		return attemptReconnect;
	}

	/**
	 * Retrieves the disconnect reason
	 * 
	 * @return DisconnectReason instance
	 */
	public DisconnectReason getDisconnectReason() {
		return disconnectReason;
	}

	/**
	 * Retrieves the phoenix client
	 * 
	 * @return PhoenixClient instance
	 */
	public PhoenixClient getClient() {
		return client;
	}

	@Override
	public String eventPath() {
		return "grid.client.connectfailed";
	}

}
