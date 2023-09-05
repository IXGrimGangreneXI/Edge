package org.asf.edge.modules.gridclient.phoenix.networking.channels;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.modules.gridclient.phoenix.PhoenixClient;
import org.asf.edge.modules.gridclient.phoenix.networking.CorePacketHandler;
import org.asf.edge.modules.gridclient.phoenix.networking.CorePacketSender;
import org.asf.edge.modules.gridclient.phoenix.networking.DataReader;
import org.asf.edge.modules.gridclient.phoenix.networking.packets.IPacketHandler;
import org.asf.edge.modules.gridclient.phoenix.networking.packets.IPhoenixPacket;

/**
 * 
 * Abstract packet channel
 * 
 * @author Sky Swimmer
 *
 */
public abstract class AbstractPacketChannel {

	private PhoenixClient client;
	private ArrayList<IPhoenixPacket> packets = new ArrayList<IPhoenixPacket>();

	@SuppressWarnings("rawtypes")
	private ArrayList<IPacketHandler> handlers = new ArrayList<IPacketHandler>();

	private ArrayList<Function<IPhoenixPacket, Boolean>> singleTimeHandlers = new ArrayList<Function<IPhoenixPacket, Boolean>>();
	private CorePacketSender sendPacketCallback;

	private Logger logger = LogManager.getLogger("Phoenix");

	/**
	 * Called to register packets
	 */
	protected abstract void registerPackets();

	/**
	 * Called to register packet handlers
	 */
	protected abstract void registerPacketHandlers();

	/**
	 * Retrieves all packet definitions
	 * 
	 * @return Array of IPhoenixPacket instances
	 */
	public IPhoenixPacket[] getPacketDefinitions() {
		return packets.toArray(t -> new IPhoenixPacket[t]);
	}

	/**
	 * Retrieves all packet handlers
	 * 
	 * @return Array of IPacketHandler instances
	 */
	public IPacketHandler<?>[] getPacketHandlers() {
		return handlers.toArray(t -> new IPacketHandler<?>[t]);
	}

	/**
	 * Registers packets
	 * 
	 * @param packet Packet to register
	 */
	public void registerPacket(IPhoenixPacket packet) {
		packets.add(packet);
	}

	/**
	 * Registers packet handlers
	 * 
	 * @param handler Packet handler to register
	 * 
	 * @param <T>     Packet type
	 */
	public <T extends IPhoenixPacket> void registerHandler(IPacketHandler<T> handler) {
		handlers.add(handler);
	}

	/**
	 * Retrieves packet definitions
	 * 
	 * @param packetID Packet ID
	 * @return IPhoenixPacket instance or null
	 */
	public IPhoenixPacket getPacketDefinition(int packetID) {
		if (packetID > packets.size())
			return null;
		return packets.get(packetID);
	}

	/**
	 * Retrieves the Phoenix client
	 * 
	 * @return PhoenixClient instance
	 */
	public PhoenixClient getClient() {
		return client;
	}

	/**
	 * Sends packets
	 * 
	 * @param packet Packet to send
	 * @throws IOException If sending fails
	 */
	public void sendPacket(IPhoenixPacket packet) throws IOException {
		// Find
		if (!packets.stream().anyMatch(t -> t.getClass().isAssignableFrom(packet.getClass())))
			throw new IllegalArgumentException("Packet type is not present in channel");

		// Send
		sendPacketCallback.send(this, packet);
	}

	/**
	 * Sends packets
	 * 
	 * @param packet              Packet to send
	 * @param responsePacketClass Response packet class
	 * @param <T>                 Response packet type
	 * @throws IOException If sending fails
	 */
	public <T extends IPhoenixPacket> T sendPacketAndWaitForResponse(IPhoenixPacket packet,
			Class<IPhoenixPacket> responsePacketClass) throws IOException {
		return sendPacketAndWaitForResponse(packet, responsePacketClass, 5000);
	}

	/**
	 * Sends packets
	 * 
	 * @param packet              Packet to send
	 * @param responsePacketClass Response packet class
	 * @param <T>                 Response packet type
	 * @param timeout             Timeout length
	 * @throws IOException If sending fails
	 */
	@SuppressWarnings("unchecked")
	public <T extends IPhoenixPacket> T sendPacketAndWaitForResponse(IPhoenixPacket packet,
			Class<IPhoenixPacket> responsePacketClass, int timeout) throws IOException {
		// Find
		if (!packets.stream().anyMatch(t -> t.getClass().isAssignableFrom(packet.getClass())))
			throw new IllegalArgumentException("Packet type is not present in channel");

		// Create handler
		ResponseContainer resp = new ResponseContainer();
		synchronized (singleTimeHandlers) {
			singleTimeHandlers.add(pk -> {
				if (responsePacketClass.isAssignableFrom(pk.getClass())) {
					resp.resp = pk;
					return true;
				}
				return false;
			});
		}

		// Send
		sendPacketCallback.send(this, packet);

		// Wait for response
		long start = System.currentTimeMillis();
		while ((System.currentTimeMillis() - start) < timeout && resp.resp == null) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
		}

		// Return
		return (T) resp.resp;
	}

	private static class ResponseContainer {
		public Object resp;
	}

	/**
	 * INTERNAL
	 */
	public CorePacketHandler init(PhoenixClient client, CorePacketSender sender) {
		if (this.client != null)
			return null;
		this.sendPacketCallback = sender;
		this.client = client;
		registerPackets();
		return (chId, id, reader) -> {
			// Find packet
			IPhoenixPacket pkt = getPacketDefinition(id);
			if (pkt == null) {
				logger.error("Unhandled packet: " + chId + ":" + id + " [" + bytesToHex(reader.readBytes())
						+ "], channel type name: " + getClass().getTypeName());
				return;
			}
			pkt = pkt.instantiate();

			// Read data into buffer if needed
			byte[] packetBytes = null;
			if (pkt.lengthPrefixed()) {
				reader = new DataReader(new ByteArrayInputStream(packetBytes));
				packetBytes = reader.readBytes();
			}

			// Read
			pkt.parse(reader);

			// Handle packet
			if (pkt.isSynchronized()) {
				if (!handlePacket(pkt)) {
					if (packetBytes != null) {
						logger.error("Unhandled packet: " + pkt.getClass().getTypeName() + ": ["
								+ bytesToHex(packetBytes) + "], channel type name: " + getClass().getTypeName());
					} else {
						logger.error("Unhandled packet: " + pkt.getClass().getTypeName() + ", channel type name: "
								+ getClass().getTypeName());
					}
				}
			} else {
				IPhoenixPacket pktF = pkt;
				byte[] packetBytesF = packetBytes;
				AsyncTaskManager.runAsync(() -> {
					try {
						if (!handlePacket(pktF)) {
							if (packetBytesF != null) {
								logger.error("Unhandled packet: " + pktF.getClass().getTypeName() + ": ["
										+ bytesToHex(packetBytesF) + "], channel type name: "
										+ getClass().getTypeName());
							} else {
								logger.error(
										"Unhandled packet: " + pktF.getClass().getTypeName() + ", channel type name: "
												+ AbstractPacketChannel.this.getClass().getTypeName());
							}
						}
					} catch (Exception e) {
						logger.error("Error occured while handling packet: " + chId + ":" + id, e);
					}
				});
			}
		};
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private boolean handlePacket(IPhoenixPacket pkt) throws IOException {
		// Run single-time handlers
		boolean handled = false;
		Function[] sHandlers;
		synchronized (singleTimeHandlers) {
			sHandlers = singleTimeHandlers.toArray(t -> new Function[t]);
		}
		for (Function<IPhoenixPacket, Boolean> handler : sHandlers) {
			if (handler.apply(pkt)) {
				handled = true;
				singleTimeHandlers.remove(handler);
				break;
			}
		}

		// Find handler
		for (IPacketHandler handler : getPacketHandlers()) {
			if (handler.packetClass().isAssignableFrom(pkt.getClass()) && handler.canHandle(pkt)) {
				// Handle
				if (handler.handle(pkt, this)) {
					handled = true;
					break;
				}
			}
		}

		return handled;
	}

	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

	private static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}
}
