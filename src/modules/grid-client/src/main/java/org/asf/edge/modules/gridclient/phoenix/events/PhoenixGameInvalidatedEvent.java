package org.asf.edge.modules.gridclient.phoenix.events;

import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

@EventPath("phoenix.game.invalidated")
public class PhoenixGameInvalidatedEvent extends EventObject {

	@Override
	public String eventPath() {
		return "phoenix.session.refresh.failure";
	}

}
