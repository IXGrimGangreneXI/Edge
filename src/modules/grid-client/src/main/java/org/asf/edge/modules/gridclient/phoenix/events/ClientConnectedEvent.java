package org.asf.edge.modules.gridclient.phoenix.events;

import org.asf.edge.modules.gridclient.phoenix.PhoenixClient;
import org.asf.nexus.events.EventPath;

@EventPath("phoenix.connected")
public class ClientConnectedEvent extends PhoenixEvent {

	public ClientConnectedEvent(PhoenixClient client) {
		super(client);
	}

	@Override
	public String eventPath() {
		return "phoenix.connected";
	}

}
