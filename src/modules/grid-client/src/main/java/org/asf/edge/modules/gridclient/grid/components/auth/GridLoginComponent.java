package org.asf.edge.modules.gridclient.grid.components.auth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.asf.edge.modules.gridclient.grid.GridClientComponent;
import org.asf.edge.modules.gridclient.phoenix.DisconnectReason;
import org.asf.edge.modules.gridclient.phoenix.PhoenixClient;
import org.asf.edge.modules.gridclient.phoenix.auth.LoginManager;
import org.asf.edge.modules.gridclient.phoenix.events.ClientProgramLateHandshakeEvent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GridLoginComponent extends GridClientComponent {

	private LoginManager manager;

	public GridLoginComponent(LoginManager manager) {
		this.manager = manager;
	}

	@Override
	public String componentID() {
		return "grid-login";
	}

	@Override
	public void addToClient(PhoenixClient client) {
	}

	@Override
	public void programLateHandshake(ClientProgramLateHandshakeEvent event) {
		try {
			// Check session
			if (!manager.isLoggedIn())
				throw new IOException("No session");

			// Early handshake
			byte[] magic = "GRIDLOGINSTART".getBytes("UTF-8");
			event.getWriter().writeRawBytes(magic);
			for (int i = 0; i < magic.length; i++) {
				if (magic[i] != event.getReader().readRawByte()) {
					// Log debug warning
					getLogger().error(
							"WARNING! Failed to authenticate due to the first bit of network traffic not being a Grid login packet.");
					getLogger().error(
							"Please make sure the order of loading for components subscribed to the late handshake event is the same on both client and server.");

					// Disconnect
					event.getClient().closeConnection();
					return;
				}
			}

			// Build URL
			getLogger().info("Attempting to grant server access to the gameplay Grid API for this player...");
			String url = manager.apiServer;
			if (!url.endsWith("/"))
				url += "/";
			url += "grid/gameplay/gridjoinserver";

			// Get server ID
			String serverID = event.getClient().getServerID();

			// Build login payload
			JsonObject loginPayload = new JsonObject();
			loginPayload.addProperty("serverID", serverID);

			// Contact login API
			try {
				getLogger().debug("Contacting API: " + url + ", server ID: " + serverID
						+ ", requesting authentication secret...");
				HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
				conn.setRequestProperty("Authorization", "Bearer " + manager.getSession().getGameSessionToken());
				conn.setRequestMethod("POST");
				conn.setDoOutput(true);
				conn.getOutputStream().write(loginPayload.toString().getBytes("UTF-8"));

				// Read response
				JsonObject response = JsonParser
						.parseString(new String((conn.getResponseCode() >= 400 ? conn.getErrorStream().readAllBytes()
								: conn.getInputStream().readAllBytes()), "UTF-8"))
						.getAsJsonObject();
				if (conn.getResponseCode() == 200 && response.has("secret")) {
					getLogger().debug("Received response: "
							+ response.toString().replace(response.get("secret").getAsString(), "[REDACTED]"));
					getLogger().debug("Sending to server...");
					event.getWriter().writeString(response.get("secret").getAsString());

					// Read response
					if (!event.getReader().readBoolean()) {
						// Read disconnect reason
						DisconnectReason disconnectReason = null;
						if (event.getReader().readBoolean()) {
							String reason = event.getReader().readString();
							String[] parameters = new String[event.getReader().readInt()];
							for (int i = 0; i < parameters.length; i++)
								parameters[i] = event.getReader().readString();
							disconnectReason = new DisconnectReason(reason, parameters);
						}

						// Error
						getLogger().error("Authentication failure! Server rejected join request!");
						if (disconnectReason != null)
							event.getClient().disconnect(disconnectReason.getReason(),
									disconnectReason.getReasonArgs());
						else
							event.getClient().disconnect("disconnect.loginfailure.authfailure");
					} else {
						getLogger().info("Successfully granted API access to the server for this session!");
					}
				} else {
					getLogger().debug("Received response: " + response);
					getLogger().debug("Failed to grant server API access!");
					throw new IOException("Server rejected token");
				}
			} catch (Exception e) {
				getLogger().error("Failed to grant server API access!", e);
				event.getClient().disconnect("disconnect.loginfailure.authfailure");
			}
		} catch (IOException e) {
			event.getClient().disconnect("disconnect.loginfailure.authfailure");
		}
	}

}
