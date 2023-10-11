package org.asf.edge.mmoserver.networking.packets;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.mmoserver.networking.CorePacketHandler;
import org.asf.edge.mmoserver.networking.CorePacketSender;
import org.asf.edge.mmoserver.networking.SmartfoxClient;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * Abstract packet channel
 * 
 * @author Sky Swimmer
 *
 */
public abstract class PacketChannel {

	private SmartfoxClient client;
	private ArrayList<ISmartfoxPacket> packetReg = new ArrayList<ISmartfoxPacket>();

	@SuppressWarnings("rawtypes")
	private ArrayList<IPacketHandler> handlers = new ArrayList<IPacketHandler>();

	private ArrayList<Function<ISmartfoxPacket, Boolean>> singleTimeHandlers = new ArrayList<Function<ISmartfoxPacket, Boolean>>();
	private CorePacketSender sendPacketCallback;

	private Logger logger = LogManager.getLogger("smartfox-client");

	/**
	 * Defines the channel ID
	 * 
	 * @return Channel ID
	 */
	public abstract byte channelID();

	/**
	 * Called to create a new instance of the channel type
	 * 
	 * @return PacketChannel instance
	 */
	public abstract PacketChannel createInstance();

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
	 * @return Array of ISmartfoxPacket instances
	 */
	public ISmartfoxPacket[] getPacketDefinitions() {
		return packetReg.toArray(t -> new ISmartfoxPacket[t]);
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
	public void registerPacket(ISmartfoxPacket packet) {
		packetReg.add(packet);
	}

	/**
	 * Registers packet handlers
	 * 
	 * @param handler Packet handler to register
	 * 
	 * @param <T>     Packet type
	 */
	public <T extends ISmartfoxPacket> void registerHandler(IPacketHandler<T> handler) {
		handlers.add(handler);
	}

	/**
	 * Retrieves the Smartfox client
	 * 
	 * @return SmartfoxClient instance
	 */
	public SmartfoxClient getClient() {
		return client;
	}

	/**
	 * Sends packets
	 * 
	 * @param packet Packet to send
	 * @throws IOException If sending fails
	 */
	public void sendPacket(ISmartfoxPacket packet) throws IOException {
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
	public <T extends ISmartfoxPacket> T sendPacketAndWaitForResponse(ISmartfoxPacket packet,
			Class<ISmartfoxPacket> responsePacketClass) throws IOException {
		return sendPacketAndWaitForResponse(packet, responsePacketClass, 5000);
	}

	/**
	 * Sends packets
	 * 
	 * @param packet              Packet to send
	 * @param responsePacketClass Response packet class
	 * @param timeout             Timeout length
	 * @param <T>                 Response packet type
	 * @throws IOException If sending fails
	 */
	@SuppressWarnings("unchecked")
	public <T extends ISmartfoxPacket> T sendPacketAndWaitForResponse(ISmartfoxPacket packet,
			Class<ISmartfoxPacket> responsePacketClass, int timeout) throws IOException {
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
	public CorePacketHandler init(SmartfoxClient client, CorePacketSender sender) {
		if (this.client != null)
			return null;
		this.sendPacketCallback = sender;
		this.client = client;
		registerPackets();
		registerPacketHandlers();
		return (chId, id, data) -> {
			// Find packet
			boolean foundMatch = false;
			for (ISmartfoxPacket pkt : packetReg) {
				if (pkt.packetID() == id && pkt.matches(data)) {
					pkt = pkt.createInstance();
					pkt.parse(data);

					// Handle packet
					if (pkt.isSynchronized()) {
						if (!handlePacket(pkt)) {
							logger.error("Unhandled packet: " + pkt.getClass().getTypeName() + ": ["
									+ new ObjectMapper().writeValueAsString(data.payload.toSfsObject())
									+ "], channel type name: " + getClass().getTypeName() + ", client: "
									+ client.getRemoteAddress());
						}
					} else {
						ISmartfoxPacket pktF = pkt;
						AsyncTaskManager.runAsync(() -> {
							try {
								if (!handlePacket(pktF)) {
									logger.error("Unhandled packet: " + pktF.getClass().getTypeName() + ": ["
											+ new ObjectMapper().writeValueAsString(data.payload.toSfsObject())
											+ "], channel type name: " + getClass().getTypeName() + ", client: "
											+ client.getRemoteAddress());
								}
							} catch (Exception e) {
								if (!(e instanceof SocketException || e instanceof IOException) || client.isConnected())
									logger.error("Error occured while handling packet: " + chId + ":" + id, e);
							}
						});
					}
					foundMatch = true;
					break;
				}
			}

			// Error
			if (!foundMatch)
				logger.error("Unregistered packet: " + chId + ":" + id + " ["
						+ new ObjectMapper().writeValueAsString(data.payload.toSfsObject()) + "], channel type name: "
						+ getClass().getTypeName() + ", client: " + client.getRemoteAddress());
		};
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private boolean handlePacket(ISmartfoxPacket pkt) throws IOException {
		// Run single-time handlers
		boolean handled = false;
		Function[] sHandlers;
		synchronized (singleTimeHandlers) {
			sHandlers = singleTimeHandlers.toArray(t -> new Function[t]);
		}
		for (Function<ISmartfoxPacket, Boolean> handler : sHandlers) {
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
}
