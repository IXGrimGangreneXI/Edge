package org.asf.edge.commonapi.http.handlers.internal;

import java.io.IOException;
import java.util.UUID;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.common.io.DataReader;
import org.asf.edge.common.util.SimpleBinaryMessageClient;
import org.asf.edge.commonapi.EdgeCommonApiServer;
import org.asf.edge.commonapi.util.MmoServerEntry;

public class MmoServerUplinkHandler extends HttpPushProcessor {

	private EdgeCommonApiServer server;

	public MmoServerUplinkHandler(EdgeCommonApiServer server) {
		this.server = server;
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new MmoServerUplinkHandler(server);
	}

	@Override
	public void process(String path, String method, RemoteClient hCl, String contentType) throws IOException {
		// Check headers
		if (!getRequest().hasHeader("Upgrade")
				|| !getRequest().getHeaderValue("Upgrade").equals("EDGEBINPROT/MMOUPLINK")) {
			setResponseStatus(400, "Bad request");
			return;
		}

		// Set headers
		setResponseHeader("X-Response-ID", UUID.randomUUID().toString());
		setResponseHeader("Upgrade", "EDGEBINPROT/MMOUPLINK");

		// Setup
		AsyncTaskManager.runAsync(() -> {
			// Wait for upgrade
			while (hCl.isConnected()) {
				// Check
				if (getResponse().hasHeader("Upgraded")
						&& getResponse().getHeaderValue("Upgraded").equalsIgnoreCase("true"))
					break;

				// Wait
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			}

			// Check
			if (hCl.isConnected()) {
				try {
					// Upgraded

					// Read handshake
					MmoServerEntry entry = new MmoServerEntry();
					DataReader reader = new DataReader(hCl.getInputStream());
					entry.address = reader.readString();
					entry.port = reader.readInt();
					entry.isBackupServer = reader.readBoolean();
					entry.rootZone = reader.readString();
					entry.zones = new String[reader.readInt()];
					for (int i = 0; i < entry.zones.length; i++)
						entry.zones[i] = reader.readString();

					// Add
					server.addMmoServer(entry);

					// Handle messages
					try {
						SimpleBinaryMessageClient client = new SimpleBinaryMessageClient((packet, cl) -> {
							return true;
						}, hCl.getInputStream(), hCl.getOutputStream());
						client.start();
					} catch (Exception e) {
					}

					// Remove
					server.removeMmoServer(entry);
				} catch (IOException e) {
				}

				// Stop client connection
				hCl.closeConnection();
			}
		});

		// Send response
		setResponseStatus(101, "Switching Protocols");
	}

	@Override
	public boolean supportsNonPush() {
		return true;
	}

	@Override
	public String path() {
		return "/mmoserver/edgemmopublish";
	}

}
