package org.asf.edge.mmoserver.events.variables;

import org.asf.edge.mmoserver.entities.smartfox.SfsUser;
import org.asf.edge.mmoserver.entities.smartfox.UserVariable;
import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * User variable value update event - called when variable values are changed
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("users.variables.value.update")
public class UserVariableValueUpdateEvent extends EventObject {

	private SfsUser user;
	private UserVariable variable;

	@Override
	public String eventPath() {
		return "users.variables.value.update";
	}

	public UserVariableValueUpdateEvent(SfsUser user, UserVariable variable) {
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
