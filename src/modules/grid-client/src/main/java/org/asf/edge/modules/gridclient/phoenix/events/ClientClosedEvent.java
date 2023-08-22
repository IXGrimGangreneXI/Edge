package org.asf.edge.modules.gridclient.phoenix.events;

import org.asf.edge.modules.eventbus.EventPath;
import org.asf.edge.modules.gridclient.phoenix.PhoenixClient;

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
