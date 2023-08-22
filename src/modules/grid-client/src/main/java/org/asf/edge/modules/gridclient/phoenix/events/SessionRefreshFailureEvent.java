package org.asf.edge.modules.gridclient.phoenix.events;

import org.asf.edge.modules.eventbus.EventPath;
import org.asf.edge.modules.gridclient.phoenix.auth.LoginManager;
import org.asf.edge.modules.gridclient.phoenix.auth.PhoenixSession;

@EventPath("phoenix.session.refresh.failure")
public class SessionRefreshFailureEvent extends LoginManagerEvent {

	private PhoenixSession session;

	public SessionRefreshFailureEvent(LoginManager manager, PhoenixSession session) {
		super(manager);
		this.session = session;
	}

	@Override
	public String eventPath() {
		return "phoenix.session.refresh.failure";
	}

	public PhoenixSession getSession() {
		return session;
	}

}
