package org.asf.edge.modules.gridapi.http.handlers;

import java.io.IOException;
import java.util.UUID;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.modules.gridapi.EdgeGridApiServer;
import org.asf.nexus.common.io.DataReader;
import org.asf.nexus.common.io.DataWriter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ApiConnectorHandler extends HttpPushProcessor {

	private EdgeGridApiServer server;

	public ApiConnectorHandler(EdgeGridApiServer server) {
		this.server = server;
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new ApiConnectorHandler(server);
	}

	@Override
	public String path() {
		return "/grid/apiconnector";
	}

	@Override
	public boolean supportsNonPush() {
		return true;
	}

	@Override
	public void process(String path, String method, RemoteClient client, String contentType) throws IOException {
		// Handle

		// Check headers
		if (!getRequest().hasHeader("Upgrade") || !getRequest().getHeaderValue("Upgrade").equals("GRIDAPICONNECTOR")) {
			setResponseStatus(400, "Bad request");
			return;
		}

		// Write HTTP success
		getResponse().addHeader("X-Response-ID", UUID.randomUUID().toString());
		setResponseHeader("Upgrade", "GRIDAPICONNECTOR");

		// Setup
		AsyncTaskManager.runAsync(() -> {
			// Wait for upgrade
			while (client.isConnected()) {
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
			if ((client.isConnected())) {
				// Handle client
				DataReader rd = new DataReader(client.getInputStream());
				Object writeLock = new Object();
				while (client.isConnected()) {
					// Read packet
					int targetID = 0;
					String requestType = "";
					String requestPayload = "";
					try {
						// Read
						targetID = rd.readInt();
						if (targetID != -1) {
							requestType = rd.readString();
							requestPayload = rd.readString();
						}
					} catch (Exception e) {
						client.closeConnection();
						break;
					}

					// Handle
					if (targetID != -1) {
						// Handle request
						String pl = requestPayload;
						String tp = requestType;
						int targetIDF = targetID;
						AsyncTaskManager.runAsync(() -> {
							JsonObject req = JsonParser.parseString(pl).getAsJsonObject();
							server.handlePhoenixApiConnectorRequest(tp, req, client, t -> {
								String str = t.toString();
								try {
									// Write response
									synchronized (writeLock) {
										DataWriter wr = new DataWriter(client.getOutputStream());
										wr.writeInt(targetIDF);
										wr.writeString(str);
									}
								} catch (IOException e) {
									client.closeConnection();
								}
							});
						});
					}
				}
			}
		});

		// Set status
		setResponseStatus(101, "Switching Protocols");
	}

}
