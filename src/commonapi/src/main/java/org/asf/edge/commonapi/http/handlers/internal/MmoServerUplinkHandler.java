package org.asf.edge.commonapi.http.handlers.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.common.events.messages.SendSessionWebserviceMessageEvent;
import org.asf.edge.common.events.messages.SendWebserviceMessageEvent;
import org.asf.edge.common.io.DataReader;
import org.asf.edge.common.io.DataWriter;
import org.asf.edge.common.util.SimpleBinaryMessageClient;
import org.asf.edge.commonapi.EdgeCommonApiServer;
import org.asf.edge.commonapi.util.MmoServerEntry;
import org.asf.edge.modules.eventbus.EventBus;

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

					// Create event handlers
					Consumer<SendSessionWebserviceMessageEvent> handlerSesMessages = null;
					Consumer<SendWebserviceMessageEvent> handlerPersMessages = null;

					// Handle messages
					try {
						// Create client
						SimpleBinaryMessageClient client = new SimpleBinaryMessageClient((packet, cl) -> {
							return true;
						}, hCl.getInputStream(), hCl.getOutputStream());

						// Bind events
						handlerSesMessages = ev -> {
							// Send packet
							try {
								ByteArrayOutputStream data = new ByteArrayOutputStream();
								DataWriter wr = new DataWriter(data);
								wr.writeRawByte((byte) 0);
								wr.writeString(ev.getAccount().getAccountID());
								client.send(data.toByteArray());
							} catch (IOException e) {
							}
						};
						handlerPersMessages = ev -> {
							// Send packet
							try {
								ByteArrayOutputStream data = new ByteArrayOutputStream();
								DataWriter wr = new DataWriter(data);
								wr.writeRawByte((byte) 0);
								wr.writeString(ev.getAccount().getAccountID());
								client.send(data.toByteArray());
							} catch (IOException e) {
							}
						};
						EventBus.getInstance().addEventHandler(SendSessionWebserviceMessageEvent.class,
								handlerSesMessages);
						EventBus.getInstance().addEventHandler(SendWebserviceMessageEvent.class, handlerPersMessages);

						// Start client
						client.start();
					} catch (Exception e) {
					}

					// Detach events
					EventBus.getInstance().removeEventHandler(SendSessionWebserviceMessageEvent.class,
							handlerSesMessages);
					EventBus.getInstance().removeEventHandler(SendWebserviceMessageEvent.class, handlerPersMessages);

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
