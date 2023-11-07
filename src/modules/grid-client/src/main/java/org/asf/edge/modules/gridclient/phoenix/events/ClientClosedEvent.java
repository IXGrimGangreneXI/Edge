package org.asf.edge.modules.gridclient.phoenix.events;

import org.asf.edge.modules.gridclient.phoenix.PhoenixClient;
import org.asf.nexus.events.EventPath;

@EventPath("phoenix.closed")
public class ClientClosedEvent extends PhoenixEvent {

	public ClientClosedEvent(PhoenixClient client) {
		super(client);
	}

	@Override
	public String eventPath() {
		return "phoenix.closed";
	}

}
