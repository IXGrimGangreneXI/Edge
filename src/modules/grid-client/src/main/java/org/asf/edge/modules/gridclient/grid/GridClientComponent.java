package org.asf.edge.modules.gridclient.grid;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.modules.eventbus.IEventReceiver;
import org.asf.edge.modules.gridclient.grid.events.GridClientConnectFailedEvent;
import org.asf.edge.modules.gridclient.grid.events.GridClientDisconnectedEvent;
import org.asf.edge.modules.gridclient.phoenix.PhoenixClient;
import org.asf.edge.modules.gridclient.phoenix.PhoenixEnvironment;
import org.asf.edge.modules.gridclient.phoenix.auth.LoginManager;
import org.asf.edge.modules.gridclient.phoenix.certificate.PhoenixCertificate;
import org.asf.edge.modules.gridclient.phoenix.events.ClientProgramHandshakeEvent;
import org.asf.edge.modules.gridclient.phoenix.events.ClientProgramLateHandshakeEvent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 
 * Grid Client Component Abstract
 * 
 * @author Sky Swimmer
 *
 */
public abstract class GridClientComponent implements IEventReceiver {

	private Logger logger = LogManager.getLogger(componentID());

	public static class GridClient {

		/**
		 * Retrieves the client login manager
		 * 
		 * @return LoginManager instance
		 */
		public static LoginManager getLoginManager() {
			return org.asf.edge.modules.gridclient.grid.GridClient.getLoginManager();
		}

		/**
		 * Retrieves all client component instances
		 * 
		 * @return Array of GridClientComponent instances
		 */
		public static GridClientComponent[] getAllComponents() {
			return org.asf.edge.modules.gridclient.grid.GridClient.components.values()
					.toArray(t -> new GridClientComponent[t]);
		}

		/**
		 * Retrieves components by ID
		 * 
		 * @param componentID Component ID string
		 * @return GridClientComponent instance or null
		 */
		public static GridClientComponent getComponent(String componentID) {
			for (GridClientComponent comp : org.asf.edge.modules.gridclient.grid.GridClient.components.values())
				if (comp.componentID().equals(componentID))
					return comp;
			return null;
		}

		/**
		 * Checks if components are present
		 * 
		 * @param componentID Component ID string
		 * @return True if present, false otherwise
		 */
		public static boolean hasComponent(String componentID) {
			return org.asf.edge.modules.gridclient.grid.GridClient.components.containsKey(componentID);
		}

		/**
		 * Retrieves components
		 * 
		 * @param <T>          Component type
		 * @param componentCls Component class
		 * @return GridClientComponent instance or null
		 */
		@SuppressWarnings("unchecked")
		public static <T extends GridClientComponent> T getComponent(Class<T> componentCls) {
			for (GridClientComponent comp : org.asf.edge.modules.gridclient.grid.GridClient.components.values())
				if (componentCls.isAssignableFrom(comp.getClass()))
					return (T) comp;
			return null;
		}

		/**
		 * Retrieves the Phoenix client instance
		 * 
		 * @return PhoenixClient instance
		 */
		public static PhoenixClient getClient() {
			return org.asf.edge.modules.gridclient.grid.GridClient.client;
		}

		/**
		 * Checks if the client is connected
		 * 
		 * @return True if connected, false otherwise
		 */
		public static boolean isConnected() {
			return org.asf.edge.modules.gridclient.grid.GridClient.client.isConnected();
		}

		/**
		 * Switches Phoenix servers
		 * 
		 * @param host Server host
		 * @param port Server port
		 * @param cert Server certificate
		 */
		public static void switchServer(String host, int port, PhoenixCertificate cert) {
			org.asf.edge.modules.gridclient.grid.GridClient.switchServer(host, port, cert);
		}

		/**
		 * Verifies the grid server connection
		 * 
		 * @return True if connected, false otherwise
		 */
		public static boolean verifyGridConnection() {
			try {
				// Verify our own connection to the Grid
				String url = PhoenixEnvironment.defaultAPIServer;
				if (!url.endsWith("/"))
					url += "/";
				url += "grid/utilities/testconnection";
				new URL(url).openStream().close();

				// Connected!
				return true;
			} catch (IOException e) {
			}
			return false;
		}

		/**
		 * Verifies the session lock status
		 * 
		 * @return SessionLockStatus value
		 */
		public static SessionLockStatus sessionLockStatus() {
			try {
				// Test connection
				String url = PhoenixEnvironment.defaultAPIServer;
				if (!url.endsWith("/"))
					url += "/";
				url += "grid/utilities/testsessionlock";
				HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
				conn.addRequestProperty("Authorization",
						"Bearer " + getLoginManager().getSession().getGameSessionToken());
				if (conn.getResponseCode() >= 400) {
					return SessionLockStatus.valueOf(new String(conn.getErrorStream().readAllBytes(), "UTF-8").trim());
				} else {
					return SessionLockStatus.valueOf(new String(conn.getInputStream().readAllBytes(), "UTF-8").trim());
				}
			} catch (Exception e) {
				return SessionLockStatus.CONNECTION_FAILURE;
			}
		}

		/**
		 * Creates Grid API requests
		 * 
		 * @param function            API function
		 * @param token               API token
		 * @param payload             Payload json
		 * @param returnErrorMessages True to return error messages, false to throw
		 *                            exceptions instead
		 * @return Response object
		 * @throws IOException If contacting the server fails
		 */
		public static JsonObject sendGridApiRequest(String function, String token, JsonObject payload,
				boolean returnErrorMessages) throws IOException {
			// Build URL
			String url = PhoenixEnvironment.defaultAPIServer;
			if (!url.endsWith("/"))
				url += "/";
			if (function.startsWith("/"))
				function = function.substring(1);
			url += function;

			// Open connection
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			conn.addRequestProperty("Authorization", "Bearer " + token);
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);

			// Write request
			conn.getOutputStream().write(payload.toString().getBytes("UTF-8"));

			// Check response
			if (conn.getResponseCode() >= 400) {
				if (!returnErrorMessages)
					throw new IOException(
							"Server returned HTTP " + conn.getResponseCode() + " " + conn.getResponseMessage());

				// Read error
				try {
					JsonObject errorMessage = JsonParser
							.parseString(new String(conn.getErrorStream().readAllBytes(), "UTF-8")).getAsJsonObject();
					if (!errorMessage.has("error")) {
						errorMessage.addProperty("error", "http_status_" + conn.getResponseCode());
						errorMessage.addProperty("errorMessage", conn.getResponseMessage());
					}
					return errorMessage;
				} catch (IOException e) {
					throw new IOException(
							"Server returned HTTP " + conn.getResponseCode() + " " + conn.getResponseMessage());
				}
			}

			// Read response
			try {
				return JsonParser.parseString(new String(conn.getInputStream().readAllBytes(), "UTF-8"))
						.getAsJsonObject();
			} catch (Exception e) {
				throw new IOException("Server returned a non-json response");
			}
		}

	}

	/**
	 * Retrieves the component logger
	 * 
	 * @return Logger instance
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Defines the component ID
	 * 
	 * @return Component ID string
	 */
	public abstract String componentID();

	/**
	 * Initializes the component
	 */
	public void init() {
	}

	/**
	 * De-initializes the component
	 */
	public void deInit() {
	}

	/**
	 * Adds component event handlers, packet channels and packet handlers to the
	 * client
	 * 
	 * @param client Phoenix client instance
	 */
	public abstract void addToClient(PhoenixClient client);

	/**
	 * Called on program handshake
	 * 
	 * @param event Event object for program handshake logic
	 */
	public void programHandshake(ClientProgramHandshakeEvent event) {
	}

	/**
	 * Called on late program handshake
	 * 
	 * @param event Event object for program late handshake logic
	 */
	public void programLateHandshake(ClientProgramLateHandshakeEvent event) {
	}

	/**
	 * Called on connect
	 */
	public void connected(PhoenixClient client) {
	}

	/**
	 * Called on connect failures
	 * 
	 * @param event Failure event object
	 */
	public void connectFailed(GridClientConnectFailedEvent event) {
	}

	/**
	 * Called on disconnect
	 * 
	 * @param event Disconnect event object
	 */
	public void disconnect(GridClientDisconnectedEvent reason) {
	}

}
