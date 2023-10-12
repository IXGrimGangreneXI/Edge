package org.asf.edge.mmoserver.networking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.mmoserver.events.players.PlayerRoomGroupDesubscribeEvent;
import org.asf.edge.mmoserver.events.players.PlayerRoomGroupSubscribeEvent;
import org.asf.edge.mmoserver.events.players.PlayerRoomJoinEvent;
import org.asf.edge.mmoserver.events.sync.SfsUserCreatedEvent;
import org.asf.edge.mmoserver.events.sync.SfsUserDeletedEvent;
import org.asf.edge.mmoserver.events.variables.RoomVariableAddedEvent;
import org.asf.edge.mmoserver.events.variables.RoomVariableRemovedEvent;
import org.asf.edge.mmoserver.events.variables.RoomVariableValueUpdateEvent;
import org.asf.edge.mmoserver.events.variables.UserVariableAddedEvent;
import org.asf.edge.mmoserver.events.variables.UserVariableRemovedEvent;
import org.asf.edge.mmoserver.events.variables.UserVariableValueUpdateEvent;
import org.asf.edge.mmoserver.events.zones.RoomCreatedEvent;
import org.asf.edge.mmoserver.events.zones.RoomDeletedEvent;
import org.asf.edge.mmoserver.networking.channels.smartfox.SystemChannel;
import org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound.ClientboundGroupSubscribePacket;
import org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound.ClientboundGroupUnsubscribePacket;
import org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound.ClientboundJoinRoomPacket;
import org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound.ClientboundRoomCreatePacket;
import org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound.ClientboundRoomDeletePacket;
import org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound.ClientboundRoomUserCountChangedPacket;
import org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound.ClientboundSetRoomVariablePacket;
import org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound.ClientboundSetUserVariablePacket;
import org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound.sync.ClientboundPlayerJoinRoomPacket;
import org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound.sync.ClientboundPlayerLeaveRoomPacket;
import org.asf.edge.mmoserver.networking.packets.ExtensionMessageChannel;
import org.asf.edge.mmoserver.networking.packets.PacketChannel;
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

	private ArrayList<PacketChannel> registry = new ArrayList<PacketChannel>();
	private ArrayList<ExtensionMessageChannel> extensionRegistry = new ArrayList<ExtensionMessageChannel>();
	private ServerEvents events = new ServerEvents();

	// Important events that need to be attached for the sfs server to work
	public class ServerEvents implements IEventReceiver {

		@EventListener
		public void userCreated(SfsUserCreatedEvent ev) {
			// Send create to other players
			ClientboundPlayerJoinRoomPacket pkt = new ClientboundPlayerJoinRoomPacket();
			pkt.roomID = ev.getRoom().getRoomID();
			pkt.user = ev.getSfsUser();

			// Send
			for (SmartfoxClient cl : getClients()) {
				try {
					// Check if subscribed and not the source player
					PlayerInfo plr = cl.getObject(PlayerInfo.class);
					if (plr != null
							&& ((plr.getZone() != null
									&& plr.getZone().getName().equals(ev.getRoom().getGroup().getZone().getName()))
									|| ev.getRoom().hasPlayer(plr) || ev.getRoom().hasSpectatorPlayer(plr))
							&& !plr.getSave().getSaveID().equals(ev.getSfsUser().getUserID())) {
						// Send
						cl.getChannel(SystemChannel.class).sendPacket(pkt);
					}
				} catch (IOException e) {
				}
			}

			// Create update
			ClientboundRoomUserCountChangedPacket cU = new ClientboundRoomUserCountChangedPacket();
			cU.roomID = ev.getRoom().getRoomID();
			cU.userCount = ev.getRoom().getUserCount();
			if (ev.getRoom().isGame())
				cU.spectatorCount = ev.getRoom().getSpectatorCount();

			// Send
			for (SmartfoxClient cl : getClients()) {
				try {
					// Check if subscribed
					PlayerInfo plr = cl.getObject(PlayerInfo.class);
					if (plr != null && ((plr.getZone() != null
							&& plr.getZone().getName().equals(ev.getRoom().getGroup().getZone().getName()))
							|| ev.getRoom().hasPlayer(plr) || ev.getRoom().hasSpectatorPlayer(plr))) {
						// Send
						cl.getChannel(SystemChannel.class).sendPacket(cU);
					}
				} catch (IOException e) {
				}
			}
		}

		@EventListener
		public void userRemoved(SfsUserDeletedEvent ev) {
			// Create packet
			ClientboundPlayerLeaveRoomPacket pkt = new ClientboundPlayerLeaveRoomPacket();
			pkt.roomID = ev.getRoom().getRoomID();
			pkt.userID = ev.getSfsUser().getUserNumericID();

			// Send
			for (SmartfoxClient cl : getClients()) {
				try {
					// Check if subscribed and not the source player
					PlayerInfo plr = cl.getObject(PlayerInfo.class);
					if (plr != null
							&& ((plr.getZone() != null
									&& plr.getZone().getName().equals(ev.getRoom().getGroup().getZone().getName()))
									|| ev.getRoom().hasPlayer(plr) || ev.getRoom().hasSpectatorPlayer(plr))
							&& !plr.getSave().getSaveID().equals(ev.getSfsUser().getUserID())) {
						// Send
						cl.getChannel(SystemChannel.class).sendPacket(pkt);
					}
				} catch (IOException e) {
				}
			}

			// Create update
			ClientboundRoomUserCountChangedPacket cU = new ClientboundRoomUserCountChangedPacket();
			cU.roomID = ev.getRoom().getRoomID();
			cU.userCount = ev.getRoom().getUserCount();
			if (ev.getRoom().isGame())
				cU.spectatorCount = ev.getRoom().getSpectatorCount();

			// Send
			for (SmartfoxClient cl : getClients()) {
				try {
					// Check if subscribed
					PlayerInfo plr = cl.getObject(PlayerInfo.class);
					if (plr != null && ((plr.getZone() != null
							&& plr.getZone().getName().equals(ev.getRoom().getGroup().getZone().getName()))
							|| ev.getRoom().hasPlayer(plr) || ev.getRoom().hasSpectatorPlayer(plr))) {
						// Send
						cl.getChannel(SystemChannel.class).sendPacket(cU);
					}
				} catch (IOException e) {
				}
			}
		}

		@EventListener
		public void userVariableCreated(UserVariableAddedEvent ev) {
			// Create update
			ClientboundSetUserVariablePacket update = new ClientboundSetUserVariablePacket();
			update.userID = ev.getUser().getUserNumericID();
			update.variables.put(ev.getVariable().getName(), ev.getVariable());

			// Send
			for (SmartfoxClient cl : getClients()) {
				try {
					// Check if subscribed
					PlayerInfo plr = cl.getObject(PlayerInfo.class);
					if (plr != null
							&& Stream.of(plr.getJoinedRooms()).anyMatch(t -> t.hasPlayer(ev.getUser().getUserID()))) {
						// Send
						cl.getChannel(SystemChannel.class).sendPacket(update);
					}
				} catch (IOException e) {
				}
			}
		}

		@EventListener
		public void userVariableValueUpdate(UserVariableValueUpdateEvent ev) {
			// Create update
			ClientboundSetUserVariablePacket update = new ClientboundSetUserVariablePacket();
			update.userID = ev.getUser().getUserNumericID();
			update.variables.put(ev.getVariable().getName(), ev.getVariable());

			// Send
			for (SmartfoxClient cl : getClients()) {
				try {
					// Check if subscribed
					PlayerInfo plr = cl.getObject(PlayerInfo.class);
					if (plr != null
							&& Stream.of(plr.getJoinedRooms()).anyMatch(t -> t.hasPlayer(ev.getUser().getUserID()))) {
						// Send
						cl.getChannel(SystemChannel.class).sendPacket(update);
					}
				} catch (IOException e) {
				}
			}
		}

		@EventListener
		public void userVariableRemoved(UserVariableRemovedEvent ev) {
			// TODO: somehow update the client...
		}

		@EventListener
		public void joinedGroup(PlayerRoomGroupSubscribeEvent ev) {
			// Create packet
			ClientboundGroupSubscribePacket pkt = new ClientboundGroupSubscribePacket();
			pkt.groupID = ev.getRoomGroup().getName();
			pkt.roomList = ev.getRoomGroup().getRooms();

			// Send
			try {
				ev.getPlayer().getClient().getChannel(SystemChannel.class).sendPacket(pkt);
			} catch (IOException e) {
			}
		}

		@EventListener
		public void leftGroup(PlayerRoomGroupDesubscribeEvent ev) {
			// Create packet
			ClientboundGroupUnsubscribePacket pkt = new ClientboundGroupUnsubscribePacket();
			pkt.groupID = ev.getRoomGroup().getName();

			// Send
			try {
				ev.getPlayer().getClient().getChannel(SystemChannel.class).sendPacket(pkt);
			} catch (IOException e) {
			}
		}

		@EventListener
		public void joinedRoom(PlayerRoomJoinEvent ev) {
			// Create packet
			ClientboundJoinRoomPacket pkt = new ClientboundJoinRoomPacket();
			pkt.room = ev.getRoom();
			pkt.users = ev.getRoom().getSfsUserObjects();

			// Send
			try {
				ev.getPlayer().getClient().getChannel(SystemChannel.class).sendPacket(pkt);
			} catch (IOException e) {
			}
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
	public void registerChannel(PacketChannel channel) {
		registry.add(channel);
	}

	/**
	 * Registers extension message channels
	 * 
	 * @param channel Channel to register
	 */
	public void registerChannel(ExtensionMessageChannel channel) {
		extensionRegistry.add(channel);
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
		client.initRegistry(registry.toArray(t -> new PacketChannel[t]),
				extensionRegistry.toArray(t -> new ExtensionMessageChannel[t]));
		client.startClient();
	}

}
