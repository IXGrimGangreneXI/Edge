package org.asf.edge.mmoserver.networking;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.mmoserver.events.variables.RoomVariableAddedEvent;
import org.asf.edge.mmoserver.events.variables.RoomVariableRemovedEvent;
import org.asf.edge.mmoserver.events.variables.RoomVariableValueUpdateEvent;
import org.asf.edge.mmoserver.events.zones.RoomCreatedEvent;
import org.asf.edge.mmoserver.events.zones.RoomDeletedEvent;
import org.asf.edge.mmoserver.events.zones.RoomGroupCreatedEvent;
import org.asf.edge.mmoserver.events.zones.RoomGroupDeletedEvent;
import org.asf.edge.mmoserver.events.zones.ZoneCreatedEvent;
import org.asf.edge.mmoserver.events.zones.ZoneDeletedEvent;
import org.asf.edge.mmoserver.networking.channels.SystemChannel;
import org.asf.edge.mmoserver.networking.channels.system.packets.clientbound.ClientboundRoomCreatePacket;
import org.asf.edge.mmoserver.networking.channels.system.packets.clientbound.ClientboundRoomDeletePacket;
import org.asf.edge.mmoserver.networking.channels.system.packets.clientbound.ClientboundSetRoomVariablePacket;
import org.asf.edge.mmoserver.networking.packets.AbstractPacketChannel;
import org.asf.edge.modules.eventbus.EventBus;
import org.asf.edge.modules.eventbus.EventListener;
import org.asf.edge.modules.eventbus.IEventReceiver;

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
	private ServerEvents events = new ServerEvents();

	// Important events that need to be attached for the sfs server to work
	public class ServerEvents implements IEventReceiver {

		@EventListener
		public void createdZone(ZoneCreatedEvent ev) {
			// TODO
			ev = ev;
		}

		@EventListener
		public void deletedZone(ZoneDeletedEvent ev) {
			// TODO
			ev = ev;
		}

		@EventListener
		public void createdGroup(RoomGroupCreatedEvent ev) {
			// TODO
			ev = ev;
		}

		@EventListener
		public void deletedGroup(RoomGroupDeletedEvent ev) {
			// TODO
			ev = ev;
		}

		@EventListener
		public void createdRoom(RoomCreatedEvent ev) {
			// Create update
			ClientboundRoomCreatePacket update = new ClientboundRoomCreatePacket();
			update.room = ev.getRoom();

			// Send
			for (SmartfoxClient cl : getClients()) {
				try {
					cl.getChannel(SystemChannel.class).sendPacket(update);
				} catch (IOException e) {
				}
			}
		}

		@EventListener
		public void deletedRoom(RoomDeletedEvent ev) {
			// Create update
			ClientboundRoomDeletePacket update = new ClientboundRoomDeletePacket();
			update.roomID = ev.getRoom().getRoomID();

			// Send
			for (SmartfoxClient cl : getClients()) {
				try {
					cl.getChannel(SystemChannel.class).sendPacket(update);
				} catch (IOException e) {
				}
			}
		}

		@EventListener
		public void variableCreated(RoomVariableAddedEvent ev) {
			// Check private
			if (ev.getVariable().isPrivate())
				return;

			// Create update
			ClientboundSetRoomVariablePacket update = new ClientboundSetRoomVariablePacket();
			update.roomID = ev.getRoom().getRoomID();
			update.variables.put(ev.getVariable().getName(), ev.getVariable());

			// Send
			for (SmartfoxClient cl : getClients()) {
				try {
					cl.getChannel(SystemChannel.class).sendPacket(update);
				} catch (IOException e) {
				}
			}
		}

		@EventListener
		public void variableValueUpdate(RoomVariableValueUpdateEvent ev) {
			// Check private
			if (ev.getVariable().isPrivate())
				return;

			// Create update
			ClientboundSetRoomVariablePacket update = new ClientboundSetRoomVariablePacket();
			update.roomID = ev.getRoom().getRoomID();
			update.variables.put(ev.getVariable().getName(), ev.getVariable());

			// Send
			for (SmartfoxClient cl : getClients()) {
				try {
					cl.getChannel(SystemChannel.class).sendPacket(update);
				} catch (IOException e) {
				}
			}
		}

		@EventListener
		public void variableRemoved(RoomVariableRemovedEvent ev) {
			// TODO: somehow update the client...
		}

	}

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
	 * Called to start the server
	 * 
	 * @throws IOException If starting the server fails
	 */
	protected abstract void startSrv() throws IOException;

	/**
	 * Called to stop the server cleanly
	 * 
	 * @throws IOException If stopping the server fails
	 */
	protected abstract void stopSrv() throws IOException;

	/**
	 * Called to stop the server forcefully
	 * 
	 * @throws IOException If stopping the server fails
	 */
	protected abstract void stopSrvForced() throws IOException;

	/**
	 * Starts the server
	 * 
	 * @throws IOException If starting the server fails
	 */
	public void start() throws IOException {
		startSrv();
		EventBus.getInstance().addAllEventsFromReceiver(events);
	}

	/**
	 * Stops the server forcefully
	 * 
	 * @throws IOException If stopping the server fails
	 */
	public void stopForced() throws IOException {
		stopSrvForced();
		EventBus.getInstance().removeAllEventsFromReceiver(events);
	}

	/**
	 * Stops the server cleanly
	 * 
	 * @throws IOException If stopping the server fails
	 */
	public void stop() throws IOException {
		stopSrv();
		EventBus.getInstance().removeAllEventsFromReceiver(events);
	}

	/**
	 * Retrieves all connected clients
	 * 
	 * @return Array of SmartfoxClient instances
	 */
	public abstract SmartfoxClient[] getClients();

	/**
	 * Retrieves clients by numeric ID
	 * 
	 * @return SmartfoxClient instance or null
	 */
	public abstract SmartfoxClient getClientByNumericID(int id);

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
