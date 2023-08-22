package org.asf.edge.modules.gridclient.phoenix.events;

import org.asf.edge.modules.eventbus.EventPath;
import org.asf.edge.modules.gridclient.phoenix.auth.LoginManager;
import org.asf.edge.modules.gridclient.phoenix.auth.PhoenixSession;

@EventPath("phoenix.session.authenticated")
public class SessionAuthenticatedEvent extends LoginManagerEvent {

	private PhoenixSession session;

	public SessionAuthenticatedEvent(LoginManager manager, PhoenixSession session) {
		super(manager);
		this.session = session;
	}

	@Override
	public String eventPath() {
		return "phoenix.session.authenticated";
	}

	public PhoenixSession getSession() {
		return session;
	}

}
