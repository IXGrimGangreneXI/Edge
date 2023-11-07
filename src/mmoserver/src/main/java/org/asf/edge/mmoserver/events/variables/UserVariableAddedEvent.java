package org.asf.edge.mmoserver.events.variables;

import org.asf.edge.mmoserver.entities.smartfox.SfsUser;
import org.asf.edge.mmoserver.entities.smartfox.UserVariable;
import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

/**
 * 
 * User variable added event - called when variables are added to users
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("users.variables.added")
public class UserVariableAddedEvent extends EventObject {

	private SfsUser user;
	private UserVariable variable;

	@Override
	public String eventPath() {
		return "users.variables.added";
	}

	public UserVariableAddedEvent(SfsUser user, UserVariable variable) {
		this.user = user;
		this.variable = variable;
	}

	/**
	 * Retrieves the variable instance
	 * 
	 * @return UserVariable instance
	 */
	public UserVariable getVariable() {
		return variable;
	}

	/**
	 * Retrieves the smartfox user object
	 * 
	 * @return SfsUser instance
	 */
	public SfsUser getUser() {
		return user;
	}

}
