package org.asf.edge.modules.gridapi.serverlist;

import java.io.IOException;

import org.asf.connective.RemoteClient;
import org.asf.edge.modules.gridapi.identities.IdentityDef;

import com.google.gson.JsonObject;

public class ServerPostHandler {

	private long lastPacket = 0;
	private RemoteClient client;
	public boolean state;

	public ServerPostHandler(IdentityDef identity, RemoteClient client) {
		this.client = client;
	}

	public void start(Runnable action) {
		// Send start
		JsonObject packet = new JsonObject();
		packet.addProperty("success", true);
		try {
			writePacket("listensuccess", packet);
			action.run();

			// Start ping loop
			while (true) {
				if (lastPacket + 3000 < System.currentTimeMillis()) {
					writePacket("ping", new JsonObject());
				}
				Thread.sleep(1000);
			}
		} catch (IOException | InterruptedException e) {
		}
	}

	private boolean writing = false;

	public void writePacket(String event, JsonObject packet) throws IOException {
		while (writing)
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				break;
			}
		writing = true;
		try {
			packet.addProperty("event", event);
			client.getOutputStream().write((packet.toString() + "\0").getBytes("UTF-8"));
			writing = false;
			lastPacket = System.currentTimeMillis();
		} catch (Exception e) {
			writing = false;
			throw e;
		}
	}

}
