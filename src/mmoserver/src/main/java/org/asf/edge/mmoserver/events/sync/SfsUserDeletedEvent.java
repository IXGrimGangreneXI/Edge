package org.asf.edge.mmoserver.events.sync;

import org.asf.edge.mmoserver.entities.smartfox.RoomInfo;
import org.asf.edge.mmoserver.entities.smartfox.SfsUser;
import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

/**
 * 
 * SFS user deletion event, called when a SFS user object is deleted
 * 
 * @author Sky Swimmer
 * 
 */
@EventPath("players.rooms.sfsuser.deleted")
public class SfsUserDeletedEvent extends EventObject {

	private SfsUser user;
	private RoomInfo room;

	public SfsUserDeletedEvent(SfsUser user, RoomInfo room) {
		this.user = user;
		this.room = room;
	}

	@Override
	public String eventPath() {
		return "players.rooms.sfsuser.deleted";
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
