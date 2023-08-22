package org.asf.edge.modules.gridclient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.modules.gridclient.components.auth.AuthenticatorComponent;
import org.asf.edge.modules.gridclient.events.GridClientConnectFailedEvent;
import org.asf.edge.modules.gridclient.events.GridClientConnectedEvent;
import org.asf.edge.modules.gridclient.events.GridClientDisconnectedEvent;
import org.asf.edge.modules.gridclient.events.GridClientSetupEvent;
import org.asf.edge.modules.gridclient.phoenix.DisconnectReason;
import org.asf.edge.modules.gridclient.phoenix.PhoenixClient;
import org.asf.edge.modules.gridclient.phoenix.PhoenixEnvironment;
import org.asf.edge.modules.gridclient.phoenix.auth.LoginManager;
import org.asf.edge.modules.gridclient.phoenix.certificate.PhoenixCertificate;
import org.asf.edge.modules.gridclient.phoenix.events.ClientConnectedEvent;
import org.asf.edge.modules.gridclient.phoenix.events.ClientDisconnectedEvent;
import org.asf.edge.modules.gridclient.phoenix.events.ClientProgramHandshakeEvent;
import org.asf.edge.modules.gridclient.phoenix.events.ClientProgramLateHandshakeEvent;
import org.asf.edge.modules.gridclient.phoenix.exceptions.PhoenixConnectException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 
 * Grid client class (<b>DO NOT USE DIRECTLY WHEN WORKING FROM WITHIN
 * COMPONENTS, THIS CLASS LOCKS UNTIL THE SERVER CONNECTION IS ESTABLISHED</b>)
 * 
 * @author Sky Swimmer
 *
 */
public class GridClient {

	public static final int GRID_PROTOCOL_VERSION = 1;
	public static final String GRID_SOFTWARE_VERSION = "1.0.0.A1";

	static PhoenixClient client;
	static HashMap<String, GridClientComponent> components = new HashMap<String, GridClientComponent>();

	private static boolean isDisconnect;
	private static boolean disconnected;

	private static Object serverSwitchLock = new Object();
	private static Object serverConnectLock = new Object();

	private static PhoenixCertificate cert;
	private static String host;
	private static int port;

	private static LoginManager loginManager;

	private static void addComponentsToPhoenixClient(PhoenixClient client) {
		client.getLogger().info("Loading components...");

		// Add components
		registerComponent(new AuthenticatorComponent(loginManager));

		// Dispatch event
		client.getEventBus().dispatchEvent(new GridClientSetupEvent(client));
	}

	/**
	 * Retrieves all client component instances
	 * 
	 * @return Array of GridClientComponent instances
	 */
	public static GridClientComponent[] getAllComponents() {
		synchronized (serverSwitchLock) {
			synchronized (serverConnectLock) {
				return components.values().toArray(t -> new GridClientComponent[t]);
			}
		}
	}

	/**
	 * Retrieves components by ID
	 * 
	 * @param componentID Component ID string
	 * @return GridClientComponent instance or null
	 */
	public static GridClientComponent getComponent(String componentID) {
		synchronized (serverSwitchLock) {
			synchronized (serverConnectLock) {
				for (GridClientComponent comp : getAllComponents())
					if (comp.componentID().equals(componentID))
						return comp;
				return null;
			}
		}
	}

	/**
	 * Checks if components are present
	 * 
	 * @param componentID Component ID string
	 * @return True if present, false otherwise
	 */
	public static boolean hasComponent(String componentID) {
		synchronized (serverSwitchLock) {
			synchronized (serverConnectLock) {
				return components.containsKey(componentID);
			}
		}
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
		synchronized (serverSwitchLock) {
			synchronized (serverConnectLock) {
				for (GridClientComponent comp : getAllComponents())
					if (componentCls.isAssignableFrom(comp.getClass()))
						return (T) comp;
				return null;
			}
		}
	}

	/**
	 * Registers client components
	 * 
	 * @param component GridClientComponent instance to register
	 */
	public static void registerComponent(GridClientComponent component) {
		if (components.containsKey(component.componentID()))
			throw new IllegalArgumentException("Component " + component.componentID() + " is already registered!");

		// Add
		client.getLogger().info("Loading component: " + component.componentID());
		components.put(component.componentID(), component);
		component.addToClient(client);

		// Register events
		client.getEventBus().addAllEventsFromReceiver(component);
	}

	/**
	 * Retrieves the Phoenix client instance
	 * 
	 * @return PhoenixClient instance
	 */
	public static PhoenixClient getClient() {
		synchronized (serverSwitchLock) {
			synchronized (serverConnectLock) {
				return client;
			}
		}
	}

	/**
	 * Checks if the client is connected
	 * 
	 * @return True if connected, false otherwise
	 */
	public static boolean isConnected() {
		return getClient().isConnected();
	}

	/**
	 * Switches Phoenix servers
	 * 
	 * @param host Server host
	 * @param port Server port
	 * @param cert Server certificate
	 */
	public static void switchServer(String host, int port, PhoenixCertificate cert) {
		synchronized (serverSwitchLock) {
			// Disconnect
			disconnect();

			// Assign
			GridClient.host = host;
			GridClient.port = port;
			GridClient.cert = cert;

			// Reconnect
			connectToServer();
		}
	}

	/**
	 * Disconnects from the servers
	 */
	public static void disconnect() {
		if (client == null)
			return;

		// Disconnect
		client.getLogger().info("Disconnecting from servers...");
		client.getEventBus().addEventHandler(ClientDisconnectedEvent.class, event -> {
			disconnected = true;
		});
		isDisconnect = true;
		if (client.isConnected())
			client.disconnect();

		// Wait for disconnect
		while (!disconnected)
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}

		// Unload components
		client.getLogger().info("Unloading components...");
		for (GridClientComponent comp : components.values()) {
			client.getLogger().info("Unoading component: " + comp.componentID());
			comp.deInit();
		}

		// Disconnected
		disconnected = false;
		isDisconnect = false;
		client.getLogger().info("Disconnected from Phoenix servers.");
		client = null;
	}

	private static boolean inited;

	/**
	 * Initializes the Grid client
	 * 
	 * @param host         Server host
	 * @param port         Server port
	 * @param cert         Server certificate
	 * @param loginManager Login manager
	 */
	public static void initGridPhoenixClient(String host, int port, PhoenixCertificate cert,
			LoginManager loginManager) {
		if (inited)
			return;
		inited = true;

		// Create client
		GridClient.host = host;
		GridClient.port = port;
		GridClient.cert = cert;
		GridClient.loginManager = loginManager;
		connectToServer();
	}

	/**
	 * Connects to the Phoenix servers
	 */
	public static void connectToServer() {
		synchronized (serverConnectLock) {
			if (client != null)
				throw new IllegalStateException("Already connected");

			// Phoenix handshake
			client = new PhoenixClient();
			client.getLogger().info("Attempting to connect to Grid Phoenix server " + host + ":" + port + "...");
			String gameID = "nexusgrid";
			String gameVersion = GRID_SOFTWARE_VERSION;
			int gameProtocol = GRID_PROTOCOL_VERSION;
			client.getEventBus().addEventHandler(ClientProgramHandshakeEvent.class, event -> {
				try {
					PhoenixClient cl = event.getClient();

					// Write game ID
					cl.getLogger().debug("Performing Phoenix Game Handshake on connection " + event.getClient());
					cl.getLogger().trace("Sending game ID: " + gameID + ", protocol version " + gameProtocol
							+ ", game version " + gameVersion + " to " + cl);
					event.getWriter().writeString(gameID);

					// Send protocol
					event.getWriter().writeInt(gameProtocol);
					event.getWriter().writeString(gameVersion);

					// Send IP and port
					event.getWriter().writeString(host);
					event.getWriter().writeInt(port);

					// Read ID and protocol
					String rGID = event.getReader().readString();
					int rProtocol = event.getReader().readInt();
					String rVer = event.getReader().readString();
					cl.getLogger().debug(
							"Received game ID: " + rGID + ", protocol version " + rProtocol + ", game version " + rVer);
					cl.getLogger().debug("Verifying handshake...");
					if (!rGID.equals(gameID)) {
						// Fail
						cl.getLogger().error("Handshake failure! Game ID mismatch!");
						cl.disconnect("connect.error.connectfailure.gamemismatch", new String[] { rGID, gameID });
						event.failHandshake();
						return;
					} else if (gameProtocol != rProtocol) {
						// Fail
						cl.getLogger().error("Handshake failure! Game version mismatch!");
						cl.disconnect("connect.error.connectfailure.versionmismatch",
								new String[] { rVer, gameVersion });
						event.failHandshake();
						return;
					}
					cl.getLogger().debug("Handshake success!");

					// Run for all components
					for (GridClientComponent comp : components.values())
						if (!event.isHandled())
							comp.programHandshake(event);
				} catch (Exception e) {
					event.failHandshake();
				}
			});

			// Client late handshake
			client.getEventBus().addEventHandler(ClientProgramLateHandshakeEvent.class, event -> {
				// Run for all components
				for (GridClientComponent comp : components.values())
					if (client.isConnected())
						comp.programLateHandshake(event);
			});

			// Connection success
			client.getEventBus().addEventHandler(ClientConnectedEvent.class, event -> {
				try {
					PhoenixClient cl = event.getClient();
					cl.getLogger().info("Grid server connection established!");
					connectionSuccess(cl);
				} catch (Exception e) {
				}
			});

			try {
				// Add components
				components.clear();
				addComponentsToPhoenixClient(client);

				// Check session
				if (!loginManager.isLoggedIn())
					throw new PhoenixConnectException(new DisconnectReason("connect.error.nosessiontoken"));

				// Start client
				client.connect(host, port, cert);
				startSuccess(host, port, cert, client);
			} catch (IOException | PhoenixConnectException e) {
				// Check
				if (isDisconnect) {
					disconnected = true;
					return;
				}

				// Dispatch event
				GridClientConnectFailedEvent eve = new GridClientConnectFailedEvent(client,
						(e instanceof PhoenixConnectException ? ((PhoenixConnectException) e).getDisconnectReason()
								: new DisconnectReason("connection.failed", e.getMessage())));
				client.getEventBus().dispatchEvent(eve);

				// Run for all components
				for (GridClientComponent comp : components.values())
					if (!eve.isHandled())
						comp.connectFailed(eve.getDisconnectReason());

				// Reconnect if needed
				if (!eve.shouldAttemptReconnect()) {
					// Error
					client.getLogger().error("Failed to connect to the Phoenix Grid server!", e);
				} else {
					// Error
					client.getLogger()
							.error("Failed to connect to the Phoenix Grid server! Automatic retry scheduled...", e);
					AsyncTaskManager.runAsync(() -> {
						while (true) {
							// Attempt connection
							try {
								// Check session
								if (!loginManager.isLoggedIn())
									throw new PhoenixConnectException(
											new DisconnectReason("connect.error.nosessiontoken"));

								// Login
								client.connect(host, port, cert);
								startSuccess(host, port, cert, client);
								break;
							} catch (IOException | PhoenixConnectException e2) {
								// Check
								if (isDisconnect) {
									disconnected = true;
									break;
								}

								// Dispatch event
								GridClientConnectFailedEvent ev = new GridClientConnectFailedEvent(client,
										(e instanceof PhoenixConnectException
												? ((PhoenixConnectException) e).getDisconnectReason()
												: new DisconnectReason("connection.failed", e.getMessage())));
								client.getEventBus().dispatchEvent(ev);

								// Run for all components
								for (GridClientComponent comp : components.values())
									if (!ev.isHandled())
										comp.connectFailed(ev.getDisconnectReason());

								// Dont reconnect if cancelled
								if (!ev.shouldAttemptReconnect())
									break;
							}
							try {
								Thread.sleep(10000);
							} catch (InterruptedException e1) {
								break;
							}
						}
					});
				}
			}
		}
	}

	private static void connectionSuccess(PhoenixClient client) {
		// Run for all components
		for (GridClientComponent comp : components.values())
			comp.connected(client);

		// Dispatch event
		client.getEventBus().dispatchEvent(new GridClientConnectedEvent(client));
	}

	private static void startSuccess(String host, int port, PhoenixCertificate cert, PhoenixClient client) {
		// Add handler
		client.getEventBus().addEventHandler(ClientDisconnectedEvent.class, event -> {
			try {
				// Check
				if (isDisconnect) {
					disconnected = true;
					return;
				}

				// Log
				client.getLogger().error("Lost Grid server connection, attempting to reconnect... ("
						+ event.getDisconnectReason().getDisconnectReason() + ")");

				// Run for all components
				for (GridClientComponent comp : components.values())
					comp.disconnect(event.getDisconnectReason());

				// Dispatch event
				client.getEventBus()
						.dispatchEvent(new GridClientDisconnectedEvent(client, event.getDisconnectReason()));

				// Reconnect
				AsyncTaskManager.runAsync(() -> {
					while (true) {
						// Attempt connection
						try {
							// Check session
							if (!loginManager.isLoggedIn())
								throw new PhoenixConnectException(new DisconnectReason("connect.error.nosessiontoken"));

							// Login
							client.connect(host, port, cert);
							break;
						} catch (IOException | PhoenixConnectException e) {
							// Check
							if (isDisconnect) {
								disconnected = true;
								break;
							}

							// Dispatch event
							GridClientConnectFailedEvent ev = new GridClientConnectFailedEvent(client,
									(e instanceof PhoenixConnectException
											? ((PhoenixConnectException) e).getDisconnectReason()
											: new DisconnectReason("connection.failed", e.getMessage())));
							client.getEventBus().dispatchEvent(ev);

							// Run for all components
							for (GridClientComponent comp : components.values())
								if (!ev.isHandled())
									comp.connectFailed(ev.getDisconnectReason());

							// Dont reconnect if cancelled
							if (!ev.shouldAttemptReconnect())
								break;
						}
						try {
							Thread.sleep(10000);
						} catch (InterruptedException e1) {
							break;
						}
					}
				});
			} catch (Exception e) {
			}
		});
	}

	/**
	 * Authenticates the Grid game session
	 * 
	 * @param gridApiVersion Expected Grid API version
	 * @param gridSoftwareID Expected Grid API software ID
	 * @throws IOException If the game cannot be authenticated
	 */
	public static void authenticateGame(String gridApiVersion, String gridSoftwareID) throws IOException {
		// Build URL
		String url = PhoenixEnvironment.defaultAPIServer;
		if (!url.endsWith("/"))
			url += "/";
		url += "grid/gameservice/startgame";

		// Build request
		JsonObject payload = new JsonObject();
		payload.addProperty("clientApiVersion", gridApiVersion);
		payload.addProperty("clientSoftwareID", gridSoftwareID);

		// Contact Phoenix
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.getOutputStream().write(payload.toString().getBytes("UTF-8"));

		// Read response
		JsonObject response = JsonParser.parseString(new String(conn.getInputStream().readAllBytes(), "UTF-8"))
				.getAsJsonObject();
		PhoenixEnvironment.defaultLoginToken = response.get("token").getAsString();
	}

}
