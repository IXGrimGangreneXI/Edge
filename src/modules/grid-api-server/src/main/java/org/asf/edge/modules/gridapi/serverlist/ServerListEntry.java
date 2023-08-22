package org.asf.edge.modules.gridapi.serverlist;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

public class ServerListEntry {
	public ServerPostHandler listConnection;
	public long lastEntryUpdate;

	public int port;
	public String[] addresses;
	public String serverId;
	public String ownerId;
	public Map<String, String> entries = new HashMap<String, String>();
	
	public String version;
	public int protocolVersion;
	public int phoenixProtocolVersion;

	public void requestUpdate() throws IOException {
		// Request update
		listConnection.writePacket("requestupdate", new JsonObject());
		for (int i = 0; i < 30; i++) {
			if (lastEntryUpdate + 5000 >= System.currentTimeMillis())
				break;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}
}
