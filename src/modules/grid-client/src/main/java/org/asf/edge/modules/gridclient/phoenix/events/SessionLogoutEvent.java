package org.asf.edge.modules.gridclient.phoenix.events;

import org.asf.edge.modules.gridclient.phoenix.auth.LoginManager;
import org.asf.edge.modules.gridclient.phoenix.auth.PhoenixSession;
import org.asf.nexus.events.EventPath;

@EventPath("phoenix.session.logout")
public class SessionLogoutEvent extends LoginManagerEvent {

	private PhoenixSession session;

	public SessionLogoutEvent(LoginManager manager, PhoenixSession session) {
		super(manager);
		this.session = session;
	}

	@Override
	public String eventPath() {
		return "phoenix.session.logout";
	}

	public PhoenixSession getSession() {
		return session;
	}

}
