package org.asf.edge.mmoserver.networking;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.mmoserver.networking.packets.AbstractPacketChannel;
import org.asf.edge.modules.eventbus.EventBus;

/**
 * 
 * Smartfox server abstract
 * 
 * @author Sky Swimmer
 *
 */
public abstract class SmartfoxServer {

	private Logger logger = LogManager.getLogger("smartfox-server");
	private EventBus eventBus = EventBus.getInstance().createBus();

	private ArrayList<AbstractPacketChannel> registry = new ArrayList<AbstractPacketChannel>();

	/**
	 * Registers packet channels
	 * 
	 * @param channel Channel to register
	 */
	public void registerChannel(AbstractPacketChannel channel) {
		registry.add(channel);
	}

	/**
	 * Retrieves the server-specific event bus
	 * 
	 * @return EventBus instance
	 */
	public EventBus getEventBus() {
		return eventBus;
	}

	/**
	 * Retrieves the client logger
	 * 
	 * @return Logger instance
	 */
	protected Logger getLogger() {
		return logger;
	}

	/**
	 * Checks if the server is running
	 * 
	 * @return True if running, false otherwise
	 */
	public abstract boolean isRunning();

	/**
	 * Starts the server
	 * 
	 * @throws IOException If starting the server fails
	 */
	public abstract void start() throws IOException;

	/**
	 * Stops the server forcefully
	 * 
	 * @throws IOException If stopping the server fails
	 */
	public abstract void stopForced() throws IOException;

	/**
	 * Stops the server cleanly
	 * 
	 * @throws IOException If stopping the server fails
	 */
	public abstract void stop() throws IOException;

	/**
	 * Retrieves all connected clients
	 * 
	 * @return Array of SmartfoxClient instances
	 */
	public abstract SmartfoxClient[] getClients();

	/**
	 * Call after accepting a client, this handles all handshaking code (blocking
	 * until the client disconnects)
	 * 
	 * @param client Client that was accepted
	 */
	protected void onClientAccepted(SmartfoxClient client) {
		// Start client
		client.initRegistry(registry.toArray(t -> new AbstractPacketChannel[t]));
		client.startClient();
	}

}
