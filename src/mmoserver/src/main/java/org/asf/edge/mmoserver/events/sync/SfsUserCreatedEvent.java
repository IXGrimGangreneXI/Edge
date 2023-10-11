package org.asf.edge.mmoserver.events.sync;

import org.asf.edge.mmoserver.entities.smartfox.RoomInfo;
import org.asf.edge.mmoserver.entities.smartfox.SfsUser;
import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * SFS user creation event, called when a SFS user object is created
 * 
 * @author Sky Swimmer
 * 
 */
@EventPath("players.rooms.sfsuser.created")
public class SfsUserCreatedEvent extends EventObject {

	private SfsUser user;
	private RoomInfo room;

	public SfsUserCreatedEvent(SfsUser user, RoomInfo room) {
		this.user = user;
		this.room = room;
	}

	@Override
	public String eventPath() {
		return "players.rooms.sfsuser.created";
	}

	/**
	 * Retrieves the room instance
	 * 
	 * @return RoomInfo instance
	 */
	public RoomInfo getRoom() {
		return room;
	}

	/**
	 * Retrieves the SFS user object
	 * 
	 * @return SfsUser instance
	 */
	public SfsUser getSfsUser() {
		return user;
	}

}
