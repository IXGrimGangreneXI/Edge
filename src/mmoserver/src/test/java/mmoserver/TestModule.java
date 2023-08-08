package mmoserver;

import org.asf.edge.mmoserver.events.clients.ClientConnectedEvent;
import org.asf.edge.mmoserver.events.clients.ClientDisconnectedEvent;
import org.asf.edge.mmoserver.events.server.MMOServerStartupEvent;
import org.asf.edge.modules.IEdgeModule;
import org.asf.edge.modules.eventbus.EventListener;

public class TestModule implements IEdgeModule {

	@Override
	public String moduleID() {
		return "test";
	}

	@Override
	public String version() {
		return "test";
	}

	@EventListener
	public void start(MMOServerStartupEvent ev) {
		ev.getServer().getServer().getEventBus().addEventHandler(ClientConnectedEvent.class, event -> {
			event = event;
		});
		ev.getServer().getServer().getEventBus().addEventHandler(ClientDisconnectedEvent.class, event -> {
			event = event;
		});
	}

	@Override
	public void init() {
	}
}
