package org.asf.edge.common.events.updates;

import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Update Cancel Event - called when a (automated) update is cancelled by a
 * admin
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("update.cancel")
public class UpdateCancelEvent extends EventObject {

	@Override
	public String eventPath() {
		return "update.cancel";
	}

}
