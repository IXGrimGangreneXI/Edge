package org.asf.edge.mmoserver.networking.packets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Function;

import org.asf.edge.mmoserver.networking.CoreExtensionHandler;
import org.asf.edge.mmoserver.networking.SmartfoxClient;
import org.asf.edge.mmoserver.networking.channels.smartfox.ExtensionChannel;
import org.asf.edge.mmoserver.networking.channels.smartfox.extension.packets.clientbound.ClientboundExtensionMessage;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

/**
 * 
 * Extension message channel
 * 
 * @author Sky Swimmer
 *
 */
public abstract class ExtensionMessageChannel {

	private SmartfoxClient client;
	private ArrayList<ISmartfoxExtensionMessage> messageReg = new ArrayList<ISmartfoxExtensionMessage>();

	@SuppressWarnings("rawtypes")
	private ArrayList<IExtensionMessageHandler> handlers = new ArrayList<IExtensionMessageHandler>();

	private ArrayList<Function<ISmartfoxExtensionMessage, Boolean>> singleTimeHandlers = new ArrayList<Function<ISmartfoxExtensionMessage, Boolean>>();

	/**
	 * Called to create a new instance of the channel type
	 * 
	 * @return ExtensionMessageChannel instance
	 */
	public abstract ExtensionMessageChannel createInstance();

	/**
	 * Called to register messages
	 */
	protected abstract void registerMessages();

	/**
	 * Called to register message handlers
	 */
	protected abstract void registerMessageHandlers();

	/**
	 * Retrieves all message definitions
	 * 
	 * @return Array of ISmartfoxExtensionMessage instances
	 */
	public ISmartfoxExtensionMessage[] getMessageDefinitions() {
		return messageReg.toArray(t -> new ISmartfoxExtensionMessage[t]);
	}

	/**
	 * Retrieves all message handlers
	 * 
	 * @return Array of IExtensionMessageHandler instances
	 */
	public IExtensionMessageHandler<?>[] getMessageHandlers() {
		return handlers.toArray(t -> new IExtensionMessageHandler<?>[t]);
	}

	/**
	 * Registers messages
	 * 
	 * @param message Message to register
	 */
	public void registerMessage(ISmartfoxExtensionMessage message) {
		messageReg.add(message);
	}

	/**
	 * Registers message handlers
	 * 
	 * @param handler Message handler to register
	 * 
	 * @param <T>     Message type
	 */
	public <T extends ISmartfoxExtensionMessage> void registerHandler(IExtensionMessageHandler<T> handler) {
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
	 * Sends messages
	 * 
	 * @param message Message to send
	 * @throws IOException If sending fails
	 */
	public void sendMessage(ISmartfoxExtensionMessage message) throws IOException {
		// Create packet
		ClientboundExtensionMessage msg = new ClientboundExtensionMessage();
		msg.command = message.messageID();
		msg.payload = new SmartfoxPayload();
		message.build(msg.payload);

		// Send
		client.getChannel(ExtensionChannel.class).sendPacket(msg);
	}

	/**
	 * Sends messages
	 * 
	 * @param message              Message to send
	 * @param responseMessageClass Response message class
	 * @param <T>                  Response message type
	 * @throws IOException If sending fails
	 */
	public <T extends ISmartfoxExtensionMessage> T sendMessageAndWaitForResponse(ISmartfoxExtensionMessage message,
			Class<ISmartfoxExtensionMessage> responseMessageClass) throws IOException {
		return sendMessageAndWaitForResponse(message, responseMessageClass, 5000);
	}

	/**
	 * Sends messages
	 * 
	 * @param message              Message to send
	 * @param responseMessageClass Response message class
	 * @param timeout              Timeout length
	 * @param <T>                  Response message type
	 * @throws IOException If sending fails
	 */
	@SuppressWarnings("unchecked")
	public <T extends ISmartfoxExtensionMessage> T sendMessageAndWaitForResponse(ISmartfoxExtensionMessage message,
			Class<ISmartfoxExtensionMessage> responseMessageClass, int timeout) throws IOException {
		// Create handler
		ResponseContainer resp = new ResponseContainer();
		synchronized (singleTimeHandlers) {
			singleTimeHandlers.add(pk -> {
				if (responseMessageClass.isAssignableFrom(pk.getClass())) {
					resp.resp = pk;
					return true;
				}
				return false;
			});
		}

		// Create packet
		ClientboundExtensionMessage msg = new ClientboundExtensionMessage();
		msg.command = message.messageID();
		msg.payload = new SmartfoxPayload();
		message.build(msg.payload);

		// Send
		client.getChannel(ExtensionChannel.class).sendPacket(msg);

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
	public CoreExtensionHandler init(SmartfoxClient client) {
		if (this.client != null)
			return null;
		this.client = client;
		registerMessages();
		registerMessageHandlers();
		return (data) -> {
			// Find packet
			boolean handled = false;
			for (ISmartfoxExtensionMessage pkt : messageReg) {
				if (pkt.messageID().equals(data.command) && pkt.matches(data.payload)) {
					// Parse packet
					pkt = pkt.createInstance();
					pkt.parse(data.payload);

					// Handle message
					if (handleMessage(pkt))
						handled = true;
				}
			}

			// Return
			return handled;
		};
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private boolean handleMessage(ISmartfoxExtensionMessage pkt) throws IOException {
		// Run single-time handlers
		boolean handled = false;
		Function[] sHandlers;
		synchronized (singleTimeHandlers) {
			sHandlers = singleTimeHandlers.toArray(t -> new Function[t]);
		}
		for (Function<ISmartfoxExtensionMessage, Boolean> handler : sHandlers) {
			if (handler.apply(pkt)) {
				handled = true;
				singleTimeHandlers.remove(handler);
				break;
			}
		}

		// Find handler
		for (IExtensionMessageHandler handler : getMessageHandlers()) {
			if (handler.messageClass().isAssignableFrom(pkt.getClass()) && handler.canHandle(pkt)) {
				// Handle
				if (handler.handle(pkt, this)) {
					handled = true;
					break;
				}
			}
		}

		// Return
		return handled;
	}
}
