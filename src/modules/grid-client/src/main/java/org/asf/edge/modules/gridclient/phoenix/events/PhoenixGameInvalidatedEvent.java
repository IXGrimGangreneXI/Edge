package org.asf.edge.modules.gridclient.phoenix.events;

import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

@EventPath("phoenix.game.invalidated")
public class PhoenixGameInvalidatedEvent extends EventObject {

	@Override
	public String eventPath() {
		return "phoenix.session.refresh.failure";
	}

}
