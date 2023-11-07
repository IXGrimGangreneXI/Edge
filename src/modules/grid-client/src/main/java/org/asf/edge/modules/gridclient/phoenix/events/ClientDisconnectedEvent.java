package org.asf.edge.modules.gridclient.phoenix.events;

import org.asf.edge.modules.gridclient.phoenix.DisconnectReason;
import org.asf.edge.modules.gridclient.phoenix.PhoenixClient;
import org.asf.nexus.events.EventPath;

@EventPath("phoenix.disconnected")
public class ClientDisconnectedEvent extends PhoenixEvent {

	private DisconnectReason disconnectReason;

	public ClientDisconnectedEvent(PhoenixClient client, DisconnectReason disconnectReason) {
		super(client);
		this.disconnectReason = disconnectReason;
	}

	public DisconnectReason getDisconnectReason() {
		return disconnectReason;
	}

	@Override
	public String eventPath() {
		return "phoenix.disconnected";
	}

}
