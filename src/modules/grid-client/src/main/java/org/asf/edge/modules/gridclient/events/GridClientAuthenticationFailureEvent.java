package org.asf.edge.modules.gridclient.events;

import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;
import org.asf.edge.modules.gridclient.phoenix.auth.LoginFailureMessage;
import org.asf.edge.modules.gridclient.phoenix.auth.LoginManager;

/**
 * 
 * Grid Client Authentication Failure Event - called when authentication fails
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("grid.client.auth.failed")
public class GridClientAuthenticationFailureEvent extends EventObject {

	private LoginManager manager;
	private LoginFailureMessage message;

	public GridClientAuthenticationFailureEvent(LoginManager manager, LoginFailureMessage message) {
		this.manager = manager;
		this.message = message;
	}

	/**
	 * Retrieves the login manager
	 * 
	 * @return LoginManager instance
	 */
	public LoginManager getLoginManager() {
		return manager;
	}

	/**
	 * Retrieves the authentication message
	 * 
	 * @return LoginFailureMessage instance
	 */
	public LoginFailureMessage getMessage() {
		return message;
	}

	@Override
	public String eventPath() {
		return "grid.client.auth.failed";
	}

}
