package org.asf.edge.mmoserver.networking;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.mmoserver.events.clients.ClientConnectedEvent;
import org.asf.edge.mmoserver.events.clients.ClientDisconnectedEvent;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxNetworkObjectUtil;
import org.asf.edge.mmoserver.networking.sfs.OuterSmartfoxPacket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * Smartfox client abstract
 * 
 * @author Sky Swimmer
 *
 */
public abstract class SmartfoxClient {

	private HashMap<String, Object> memory = new HashMap<String, Object>();

	private Logger logger = LogManager.getLogger("smartfox-client");

	/**
	 * Retrieves the client logger
	 * 
	 * @return Logger instance
	 */
	protected Logger getLogger() {
		return logger;
	}

	/**
	 * Retrieves session memory objects
	 * 
	 * @param <T>  Object type
	 * @param type Object class
	 * @return Object instance or null
	 */
	@SuppressWarnings("unchecked")
	public <T> T getObject(Class<T> type) {
		return (T) memory.get(type.getTypeName());
	}

	/**
	 * Stores session memory objects
	 * 
	 * @param <T>    Object type
	 * @param type   Object class
	 * @param object Object instance
	 */
	public <T> void setObject(Class<T> type, T object) {
		memory.put(type.getTypeName(), object);
	}

	/**
	 * Removes session memory objects
	 * 
	 * @param <T>  Object type
	 * @param type Object class
	 */
	public <T> void removeObject(Class<T> type) {
		memory.remove(type.getTypeName());
	}

	/**
	 * Disconnects the client
	 */
	public void disconnect() {
		if (!isConnected())
			return;
		callDisconnectEvents();
		disconnectClient();
	}

	/**
	 * Calls disconnect events
	 */
	protected void callDisconnectEvents() {
		// Call disconnect
		getServer().getEventBus().dispatchEvent(new ClientDisconnectedEvent(getServer(), this));
	}

	/**
	 * Retrieves the server this client is associated with
	 * 
	 * @return SmartfoxServer instance
	 */
	public abstract SmartfoxServer getServer();

	/**
	 * Retrieves the remote socket address
	 * 
	 * @return Remote socket address string
	 */
	public abstract String getRemoteAddress();

	/**
	 * Checks if the client is still connected
	 * 
	 * @return True if connected, false otherwise
	 */
	public abstract boolean isConnected();

	/**
	 * Called to disconnect the client
	 */
	protected abstract void disconnectClient();

	/**
	 * Called to read single packets
	 * 
	 * @return Packet bytes
	 * @throws IOException If reading fails
	 */
	protected abstract byte[] readSingleRawPacket() throws IOException;

	/**
	 * Called to write packets
	 * 
	 * @param packet Packet payload
	 * @throws IOException If writing fails
	 */
	protected abstract void writeSingleRawPacket(byte[] packet) throws IOException;

	void startClient() {
		// Handshake
		try {
			// Read first packet
			byte[] data = readSingleRawPacket();

			// Decode
			Map<String, Object> obj = SmartfoxNetworkObjectUtil.parseSfsObject(data);
			OuterSmartfoxPacket handshakePk = OuterSmartfoxPacket.fromSfsObject(obj);

			// Handle handshake
			data = data;
		} catch (IOException e) {
			getLogger().error("Failed to handshake client " + getRemoteAddress(), e);
			disconnect();
			return;
		}

		// Dispatch connected event
		getServer().getEventBus().dispatchEvent(new ClientConnectedEvent(getServer(), this));

		// Start packet handler
		while (true) {
			// Read packet
			byte[] packet;
			try {
				packet = readSingleRawPacket();
			} catch (IOException e) {
				disconnect();
				return;
			}

			// Decode
			OuterSmartfoxPacket pkt;
			try {
				pkt = OuterSmartfoxPacket.fromSfsObject(SmartfoxNetworkObjectUtil.parseSfsObject(packet));
			} catch (Exception e) {
				logger.debug("Error occured while decoding packet: " + bytesToHex(packet) + " from client "
						+ getRemoteAddress(), e);
				disconnect();
				return;
			}

			// Handle
			try {
				// TODO
			} catch (Exception e) {
				try {
					logger.debug("Error occured while handling packet: " + pkt.targetController + ":" + pkt.packetId
							+ " (payload: " + new ObjectMapper().writeValueAsString(pkt.payload) + ") from client "
							+ getRemoteAddress(), e);
				} catch (JsonProcessingException e1) {
				}
				disconnect();
				return;
			}
		}
	}

	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}
}
