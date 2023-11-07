package org.asf.edge.mmoserver.events.clients;

import org.asf.edge.mmoserver.events.SmartfoxEvent;
import org.asf.edge.mmoserver.networking.SmartfoxClient;
import org.asf.edge.mmoserver.networking.SmartfoxServer;
import org.asf.nexus.events.EventPath;

@EventPath("mmo.sfsclient.connected")
public class ClientConnectedEvent extends SmartfoxEvent {

	public ClientConnectedEvent(SmartfoxServer server, SmartfoxClient client) {
		super(server, client);
	}

	@Override
	public String eventPath() {
		return "mmo.sfsclient.connected";
	}

}
