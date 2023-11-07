package org.asf.edge.common.events.updates;

import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

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
