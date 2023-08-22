package org.asf.edge.modules.gridclient.phoenix.serverlist;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.function.Consumer;

import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.modules.gridclient.phoenix.PhoenixClient;
import org.asf.edge.modules.gridclient.phoenix.PhoenixEnvironment;
import org.asf.edge.modules.gridclient.phoenix.networking.DataReader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 
 * Phoenix Server List Scanner
 * 
 * @author Sky Swimmer
 *
 */
public class ServerListScanner {

	private String url;

	private int protocolVersion;
	private String gameID;

	private boolean filterIncompatibleProtocols = true;
	private boolean filterInsecureModeServers = true;

	private ArrayList<Consumer<ServerInstance>> detectServerCallback = new ArrayList<Consumer<ServerInstance>>();

	/**
	 * Adds event handlers that are called when a server is detected
	 * 
	 * @param handler Event handler to add
	 */
	public void addDetectServerEventHandler(Consumer<ServerInstance> handler) {
		synchronized (detectServerCallback) {
			detectServerCallback.add(handler);
		}
	}

	public ServerListScanner(String gameID, int protocolVersion) {
		this.gameID = gameID;
		this.protocolVersion = protocolVersion;
		url = PhoenixEnvironment.defaultAPIServer;
	}

	public ServerListScanner(String gameID, int protocolVersion, boolean filterIncompatibleProtocols) {
		this.gameID = gameID;
		this.protocolVersion = protocolVersion;
		this.filterIncompatibleProtocols = filterIncompatibleProtocols;
		url = PhoenixEnvironment.defaultAPIServer;
	}

	public ServerListScanner(String gameID, int protocolVersion, boolean filterIncompatibleProtocols,
			boolean filterInsecureModeServers) {
		this.gameID = gameID;
		this.protocolVersion = protocolVersion;
		this.filterIncompatibleProtocols = filterIncompatibleProtocols;
		this.filterInsecureModeServers = filterInsecureModeServers;
		url = PhoenixEnvironment.defaultAPIServer;
	}

	public ServerListScanner(String gameID, int protocolVersion, boolean filterIncompatibleProtocols,
			boolean filterInsecureModeServers, String serverURL) {
		this.gameID = gameID;
		this.protocolVersion = protocolVersion;
		this.filterIncompatibleProtocols = filterIncompatibleProtocols;
		this.filterInsecureModeServers = filterInsecureModeServers;
		url = serverURL;
	}

	/**
	 * Scans the public server list for servers
	 * 
	 * @param filters Server list filters
	 * @return Array of ServerInstance objects
	 */
	public ServerInstance[] scanPublicServerList(ServerListFilter... filters) {
		return scanPublicServerList(5000, filters);
	}

	/**
	 * Scans the public server list for servers
	 * 
	 * @param timeout Timeout in miliseconds
	 * @param filters Server list filters
	 * @return Array of ServerInstance objects
	 */
	public ServerInstance[] scanPublicServerList(int timeout, ServerListFilter... filters) {
		// Build filters
		EndMarker marker = new EndMarker();
		JsonObject req = new JsonObject();
		for (ServerListFilter filter : filters) {
			String k = filter.getKey();
			switch (filter.getType()) {

			case STRICT:
				k = "==" + k;
				break;
			case LOOSE:
				k = "=~" + k;
				break;
			case REVERSE_STRICT:
				k = "!=" + k;
				break;
			case REVERSE_LOOSE:
				k = "!~" + k;
				break;
			default:
				break;

			}
			req.addProperty(k, filter.getValue());
		}

		// Find servers
		ArrayList<ServerInstance> servers = new ArrayList<ServerInstance>();
		ArrayList<ServerInstance> instances = new ArrayList<ServerInstance>();
		try {
			// Build URL
			String url = this.url;
			if (!url.endsWith("/"))
				url += "/";
			url += "servers/serverlist/" + gameID;

			// Send request
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.getOutputStream().write(req.toString().getBytes("UTF-8"));

			// Read response
			long time = System.currentTimeMillis();
			String lineBuffer = "";
			while (true) {
				int i = conn.getInputStream().read();
				if (i == -1)
					break;

				// Read
				char ch = (char) i;
				if (ch == '\n') {
					// Read entry
					if (!lineBuffer.isEmpty()) {
						ServerInstance instance = readEntry(lineBuffer);

						// Add entry
						if (instance.gameID.equals(gameID) && (!filterIncompatibleProtocols
								|| (instance.phoenixProtocolVersion == PhoenixClient.PHOENIX_PROTOCOL_VERSION
										&& instance.protocolVersion == this.protocolVersion))) {
							servers.add(instance);
						}
					}
					lineBuffer = "";
				} else if (ch != '\r')
					lineBuffer += ch;
			}
			if (!lineBuffer.isEmpty()) {
				ServerInstance instance = readEntry(lineBuffer);

				// Add entry
				if (instance.gameID.equals(gameID) && (!filterIncompatibleProtocols
						|| (instance.phoenixProtocolVersion == PhoenixClient.PHOENIX_PROTOCOL_VERSION
								&& instance.protocolVersion == this.protocolVersion))) {
					servers.add(instance);
				}
			}
			conn.disconnect();

			// Ping each
			for (ServerInstance inst : servers) {
				// Ping on another thread
				AsyncTaskManager.runAsync(() -> {
					if (!inst.isReachable() || marker.ended)
						return;
					instances.add(inst);
				});
			}

			// Wait
			while ((System.currentTimeMillis() - time) < timeout && instances.size() != servers.size())
				Thread.sleep(100);
		} catch (IOException | InterruptedException e) {
		}
		marker.ended = true;
		return instances.toArray(t -> new ServerInstance[t]);
	}

	private ServerInstance readEntry(String lineBuffer) {
		// Read json
		JsonObject serverInfo = JsonParser.parseString(lineBuffer).getAsJsonObject();
		ServerInstance instance = new ServerInstance();
		instance.secureMode = true;
		instance.gameID = gameID;
		instance.serverID = serverInfo.get("id").getAsString();
		instance.version = serverInfo.get("version").getAsString();
		JsonObject protocol = serverInfo.get("protocol").getAsJsonObject();
		instance.protocolVersion = protocol.get("programVersion").getAsInt();
		instance.phoenixProtocolVersion = protocol.get("phoenixVersion").getAsInt();
		instance.port = serverInfo.get("port").getAsInt();
		JsonArray addrs = serverInfo.get("addresses").getAsJsonArray();
		instance.addresses = new String[addrs.size()];
		int i = 0;
		for (JsonElement ele : addrs) {
			instance.addresses[i++] = ele.getAsString();
		}
		JsonObject details = serverInfo.get("details").getAsJsonObject();
		for (String key : details.keySet())
			instance.details.put(key, details.get(key).getAsString());
		return instance;
	}

	/**
	 * Scans the lan network for lan servers
	 * 
	 * @return Array of ServerInstance objects
	 */
	public ServerInstance[] scanLanNetwork() {
		return scanLanNetwork(5000);
	}

	private class EndMarker {
		public boolean ended;
	}

	/**
	 * Scans the lan network for lan servers
	 * 
	 * @param timeout Timeout in miliseconds
	 * @return Array of ServerInstance objects
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ServerInstance[] scanLanNetwork(int timeout) {
		ArrayList<ServerInstance> instances = new ArrayList<ServerInstance>();
		ArrayList<String> entries = new ArrayList<String>();
		EndMarker marker = new EndMarker();
		try {
			InetSocketAddress grp = new InetSocketAddress(InetAddress.getByName("224.0.2.232"), 16719);
			MulticastSocket sock = new MulticastSocket(16719);
			try {
				// Set options
				sock.setSoTimeout(timeout);

				// Find interfaces
				Enumeration<NetworkInterface> interfaces_enumeration = NetworkInterface.getNetworkInterfaces();
				ArrayList<NetworkInterface> interfaces = new ArrayList<NetworkInterface>();
				while (interfaces_enumeration.hasMoreElements()) {
					interfaces.add(interfaces_enumeration.nextElement());
				}

				// Join groups
				for (NetworkInterface i : interfaces)
					try {
						sock.joinGroup(grp, i);
					} catch (Exception e) {
					}

				// Build magic
				byte[] magic = "PHOENIX/LANDISCOVERY".getBytes("UTF-8");

				// Send discovery command
				DatagramPacket pkt = new DatagramPacket(magic, 0, magic.length, grp);
				sock.send(pkt);

				// Gather all servers
				long time = System.currentTimeMillis();
				while (true) {
					try {
						// Check timeout
						if (time + timeout < System.currentTimeMillis())
							break;

						// Receive
						byte[] buf = new byte[sock.getReceiveBufferSize()];
						DatagramPacket pk = new DatagramPacket(buf, 0, buf.length);
						sock.receive(pk);

						// Wrap stream around it
						ByteArrayInputStream strm = new ByteArrayInputStream(buf);

						// Handshake
						boolean pass = true;
						for (int i = 0; i < magic.length; i++) {
							if (strm.read() != magic[i]) {
								pass = false;
								break;
							}
						}
						if (!pass)
							continue;

						// Read data
						DataReader reader = new DataReader(strm);
						String command = reader.readString();
						if (command.equals("serverinfo")) {
							// Secure mode
							boolean secure = reader.readBoolean();

							// Port
							int port = reader.readInt();

							// Server ID
							String id = reader.readString();

							// Protocol and version
							String gameid = reader.readString();
							reader.readString();
							String version = reader.readString();
							int protocol = reader.readInt();
							int phoenixProtocol = reader.readInt();

							// Details
							HashMap<String, String> details = new HashMap<String, String>();
							int l = reader.readInt();
							for (int i = 0; i < l; i++) {
								details.put(reader.readString(), reader.readString());
							}

							// Read address
							String addr = pk.getAddress().getHostAddress();

							// Found a server
							if (!entries.contains("[" + addr + "]:" + port) && gameid.equals(gameID)
									&& (!filterIncompatibleProtocols
											|| (phoenixProtocol == PhoenixClient.PHOENIX_PROTOCOL_VERSION
													&& protocol == this.protocolVersion))
									&& (!filterInsecureModeServers || secure)) {
								entries.add("[" + addr + "]:" + port);

								// Ping on another thread
								AsyncTaskManager.runAsync(() -> {
									ServerInstance srv = new ServerInstance();
									srv.gameID = gameID;
									srv.serverID = id;
									srv.protocolVersion = protocol;
									srv.phoenixProtocolVersion = phoenixProtocol;
									srv.isLanServer = true;
									srv.secureMode = secure;
									srv.version = version;
									srv.addresses = new String[] { addr };
									srv.bestAddress = addr;
									srv.port = port;
									srv.details = details;
									if (!srv.isReachable() || marker.ended)
										return;
									instances.add(srv);

									Consumer[] handlers;
									synchronized (detectServerCallback) {
										handlers = detectServerCallback.toArray(t -> new Consumer[t]);
									}

									for (Consumer c : handlers)
										c.accept(srv);
								});
							}
						}
					} catch (IOException e) {
					}
				}
			} finally {
				sock.close();
			}
		} catch (IOException e) {
		}
		marker.ended = true;
		return instances.toArray(t -> new ServerInstance[t]);
	}

}
