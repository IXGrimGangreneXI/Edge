package org.asf.edge.modules.gridclient.phoenix.events;

import org.asf.edge.modules.eventbus.EventPath;
import org.asf.edge.modules.gridclient.phoenix.PhoenixClient;

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
