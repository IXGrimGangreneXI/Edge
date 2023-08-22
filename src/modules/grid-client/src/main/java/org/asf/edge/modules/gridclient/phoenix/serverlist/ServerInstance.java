package org.asf.edge.modules.gridclient.phoenix.serverlist;

import java.util.Map;

import org.asf.edge.modules.gridclient.phoenix.PhoenixClient;
import org.asf.edge.modules.gridclient.phoenix.networking.DataReader;
import org.asf.edge.modules.gridclient.phoenix.networking.DataWriter;

import java.net.Socket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;

/**
 * 
 * Phoenix server instance
 * 
 * @author Sky Swimmer
 *
 */
public class ServerInstance {
	public boolean secureMode;
	public boolean isLanServer;

	public String gameID;

	public String serverID;
	public String version;

	public int protocolVersion;
	public int phoenixProtocolVersion;

	public String[] addresses = new String[0];
	public int port;

	public Map<String, String> details = new HashMap<String, String>();

	String bestAddress;
	private boolean unreachable;
	private int ping = -1;

	/**
	 * Checks if the server is reachable
	 * 
	 * @return True if reachable, false otherwise
	 */
	public boolean isReachable() {
		if (unreachable)
			return false;
		pingServer();
		return !unreachable;
	}

	/**
	 * Retrieves the server ping
	 * 
	 * @return Server ping time or -1 if unreachable
	 */
	public int getPing() {
		if (unreachable)
			return -1;
		if (ping == -1)
			pingServer();
		return ping;
	}

	/**
	 * Retrieves the best server address for this server
	 * 
	 * @return Server address string or null if unreachable
	 */
	public String getBestAdress() {
		if (bestAddress != null)
			return bestAddress;

		// Try ping
		if (unreachable)
			return null;
		pingServer();
		return bestAddress;
	}

	private void pingServer() {
		if (bestAddress == null) {
			// Find address
			HashMap<String, Integer> pings = new HashMap<String, Integer>();
			for (String address : addresses) {
				Socket socket = new Socket();
				try {
					try {
						// Connect
						long start = System.currentTimeMillis();
						socket.connect(new InetSocketAddress(address, port), 1000);
						DataWriter writer = new DataWriter(socket.getOutputStream());
						DataReader reader = new DataReader(socket.getInputStream());

						// Attempt partial handshake
						byte[] hello = ("PHOENIX/HELLO/" + PhoenixClient.PHOENIX_PROTOCOL_VERSION + "/")
								.getBytes("UTF-8");
						byte[] helloSrv = ("PHOENIX/HELLO/SERVER/" + PhoenixClient.PHOENIX_PROTOCOL_VERSION + "/")
								.getBytes("UTF-8");
						socket.getOutputStream().write(hello);
						for (byte b : helloSrv) {
							byte b2 = reader.readRawByte();
							if (b != b2) {
								writer.writeRawByte((byte) 0);
								throw new IOException();
							}
						}

						// Get ping time
						long time = System.currentTimeMillis() - start;

						// Send endpoint
						writer.writeString(address);
						writer.writeInt(port);

						// Set mode to info
						socket.getOutputStream().write(0);

						// Lets do this cleanly
						reader.readString(); // Game ID
						reader.readString(); // Server ID
						reader.readBoolean(); // Secure-mode

						// Save ping
						pings.put(address, (int) time);
					} catch (IOException e) {
					}
				} finally {
					try {
						socket.close();
					} catch (Exception e) {
					}
				}
			}

			// Find lowest ping
			int lowestPing = -1;
			String bestAddr = null;
			for (String addr : pings.keySet()) {
				if (lowestPing == -1 || pings.get(addr) < lowestPing) {
					lowestPing = pings.get(addr);
					bestAddr = addr;
				}
			}
			if (bestAddr != null) {
				bestAddress = bestAddr;
				ping = lowestPing;
			} else
				unreachable = true;
			return;
		}

		// Ping server
		Socket socket = new Socket();
		try {
			try {
				// Connect
				long start = System.currentTimeMillis();
				socket.connect(new InetSocketAddress(bestAddress, port), 1000);
				DataWriter writer = new DataWriter(socket.getOutputStream());
				DataReader reader = new DataReader(socket.getInputStream());

				// Attempt partial handshake
				byte[] hello = ("PHOENIX/HELLO/" + PhoenixClient.PHOENIX_PROTOCOL_VERSION + "/").getBytes("UTF-8");
				byte[] helloSrv = ("PHOENIX/HELLO/SERVER/" + PhoenixClient.PHOENIX_PROTOCOL_VERSION + "/")
						.getBytes("UTF-8");
				socket.getOutputStream().write(hello);
				for (byte b : helloSrv) {
					byte b2 = reader.readRawByte();
					if (b != b2) {
						writer.writeRawByte((byte) 0);
						throw new IOException();
					}
				}

				// Get ping time
				long time = System.currentTimeMillis() - start;

				// Send endpoint
				writer.writeString(bestAddress);
				writer.writeInt(port);

				// Set mode to info
				socket.getOutputStream().write(0);

				// Lets do this cleanly
				reader.readString(); // Game ID
				reader.readString(); // Server ID
				reader.readBoolean(); // Secure-mode

				// Save ping
				ping = (int) time;
			} catch (IOException e) {
			}
		} finally {
			try {
				socket.close();
			} catch (Exception e) {
			}
		}
	}

}
