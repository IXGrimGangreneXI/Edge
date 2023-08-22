package org.asf.edge.modules.gridclient.phoenix.events;

import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.gridclient.phoenix.auth.LoginManager;

public abstract class LoginManagerEvent extends EventObject {

	private LoginManager manager;

	public LoginManagerEvent(LoginManager manager) {
		this.manager = manager;
	}

	public LoginManager getLoginManager() {
		return manager;
	}

}
