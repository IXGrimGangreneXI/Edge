package org.asf.edge.mmoserver.entities.player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;
import org.asf.edge.common.permissions.PermissionContext;
import org.asf.edge.common.permissions.PermissionLevel;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.mmoserver.EdgeMMOServer;
import org.asf.edge.mmoserver.entities.smartfox.GameZone;
import org.asf.edge.mmoserver.entities.smartfox.RoomGroup;
import org.asf.edge.mmoserver.entities.smartfox.RoomInfo;
import org.asf.edge.mmoserver.entities.smartfox.RoomVariable;
import org.asf.edge.mmoserver.events.players.*;
import org.asf.edge.mmoserver.networking.SmartfoxClient;
import org.asf.edge.mmoserver.services.ZoneManager;
import org.asf.nexus.events.EventBus;
import org.asf.nexus.events.EventListener;
import org.asf.nexus.events.IEventReceiver;

/**
 * 
 * Player information class
 * 
 * @author Sky Swimmer
 * 
 */
public class PlayerInfo {

	private Logger logger;
	private EventContainer events;

	private SmartfoxClient client;
	private AccountObject account;
	private AccountSaveContainer save;

	private GameZone zone;
	private GameZone sodZone;

	private ArrayList<RoomGroup> subscribedGroups = new ArrayList<RoomGroup>();
	private ArrayList<RoomInfo> spectatingRooms = new ArrayList<RoomInfo>();
	private ArrayList<RoomInfo> joinedRooms = new ArrayList<RoomInfo>();

	private static Random rnd = new Random();

	public static class PlayerDataController {
		public PlayerInfo player;
		public Runnable onJoinComplete;
	}

	public static PlayerDataController createPlayerInfo(SmartfoxClient client, AccountSaveContainer save,
			AccountObject account, GameZone zone) {
		PlayerDataController cont = new PlayerDataController();
		PlayerInfo info = new PlayerInfo(client, save, account, zone);
		cont.player = info;
		cont.onJoinComplete = () -> {
			info.onJoinZone(zone);
		};
		return cont;
	}

	private PlayerInfo(SmartfoxClient client, AccountSaveContainer save, AccountObject account, GameZone zone) {
		// Load details
		this.save = save;
		this.client = client;
		this.account = account;
		this.zone = zone;

		// Logging
		logger = client.getObject(EdgeMMOServer.class).getLogger();

		// SOD-specific
		sodZone = ZoneManager.getInstance().getZone("ProjectEdge-SchoolOfDragons");
		if (sodZone == null)
			sodZone = ZoneManager.getInstance().createZone("ProjectEdge-SchoolOfDragons");

		// Attach events
		events = new EventContainer();
		EventBus.getInstance().addAllEventsFromReceiver(events);
	}

	/**
	 * Retrieves the player client
	 * 
	 * @return SmartfoxClient instance
	 */
	public SmartfoxClient getClient() {
		return client;
	}

	/**
	 * Retrieves the account used by this plaeyr
	 * 
	 * @return AccountObject instance
	 */
	public AccountObject getAccount() {
		return account;
	}

	/**
	 * Retrieves the account save
	 * 
	 * @return AccountSaveContainer instance
	 */
	public AccountSaveContainer getSave() {
		return save;
	}

	/**
	 * Changes the zone the player is in
	 * 
	 * @param zone New zone instance
	 */
	public void setZone(GameZone zone) {
		// Leave old zone
		onLeaveZone(this.zone);

		// Update zone
		this.zone = zone;

		// Join new zone
		onJoinZone(zone);
	}

	/**
	 * Checks if the player is subscribed to the specified room group
	 * 
	 * @param group Group to check
	 * @return True if subscribed, false otherwise
	 */
	public boolean isSubscribedToGroup(RoomGroup group) {
		return group.isPlayerSubscribed(this);
	}

	/**
	 * Subscribes to room groups
	 * 
	 * @param group Room group to subscribe to
	 */
	public void subscribeToGroup(RoomGroup group) {
		group.subscribePlayer(this);
	}

	/**
	 * De-subscribes from room groups
	 * 
	 * @param group Room group to de-subscribe from
	 */
	public void desubscribeFromGroup(RoomGroup group) {
		group.desubscribePlayer(this);
	}

	/**
	 * Retrieves all subscribed room group names
	 * 
	 * @return Array of group names
	 */
	public String[] getSubscribedRoomGroupNames() {
		synchronized (subscribedGroups) {
			return subscribedGroups.stream().map(t -> t.getName()).toArray(t -> new String[t]);
		}
	}

	/**
	 * Retrieves all subscribed room groups
	 * 
	 * @return Array of RoomGroup instances
	 */
	public RoomGroup[] getSubscribedRoomGroups() {
		synchronized (subscribedGroups) {
			return subscribedGroups.toArray(t -> new RoomGroup[t]);
		}
	}

	/**
	 * Checks if the player is in the specified room
	 * 
	 * @param room Room to check
	 * @return True if subscribed, false otherwise
	 */
	public boolean hasJoinedRoom(RoomInfo room) {
		return room.hasPlayer(this);
	}

	/**
	 * Joins rooms
	 * 
	 * @param room Room to join
	 */
	public void joinRoom(RoomInfo room) {
		joinRoom(room, true);
	}

	/**
	 * Joins rooms
	 * 
	 * @param room        Room to join
	 * @param leaveOthers True to leave other rooms, false otherwise
	 */
	public void joinRoom(RoomInfo room, boolean leaveOthers) {
		if (leaveOthers) {
			for (RoomInfo r : getJoinedRooms())
				leaveRoom(r);
			for (RoomInfo r : getSpectatingRooms())
				stopSpectatingRoom(r);
		}
		room.addPlayer(this);
	}

	/**
	 * Leaves rooms
	 * 
	 * @param room Room to leave
	 */
	public void leaveRoom(RoomInfo room) {
		room.removePlayer(this);
	}

	/**
	 * Retrieves all rooms the player has joined
	 * 
	 * @return Array of RoomInfo instances
	 */
	public RoomInfo[] getJoinedRooms() {
		synchronized (joinedRooms) {
			return joinedRooms.toArray(t -> new RoomInfo[t]);
		}
	}

	/**
	 * Checks if the player is in the specified room as a spectator
	 * 
	 * @param room Room to check
	 * @return True if subscribed, false otherwise
	 */
	public boolean isSpectatingRoom(RoomInfo room) {
		return room.hasSpectatorPlayer(this);
	}

	/**
	 * Joins rooms as spectator
	 * 
	 * @param room Room to join
	 */
	public void startSpectatingRoom(RoomInfo room) {
		startSpectatingRoom(room, true);
	}

	/**
	 * Joins rooms as spectator
	 * 
	 * @param room        Room to join
	 * @param leaveOthers True to leave other rooms, false otherwise
	 */
	public void startSpectatingRoom(RoomInfo room, boolean leaveOthers) {
		if (leaveOthers) {
			for (RoomInfo r : getJoinedRooms())
				leaveRoom(r);
			for (RoomInfo r : getSpectatingRooms())
				stopSpectatingRoom(r);
		}
		room.addSpectatorPlayer(this);
	}

	/**
	 * Leaves rooms
	 * 
	 * @param room Room to leave
	 */
	public void stopSpectatingRoom(RoomInfo room) {
		room.removeSpectatorPlayer(this);
	}

	/**
	 * Retrieves all rooms the player has joined as spectator
	 * 
	 * @return Array of RoomInfo instances
	 */
	public RoomInfo[] getSpectatingRooms() {
		synchronized (spectatingRooms) {
			return spectatingRooms.toArray(t -> new RoomInfo[t]);
		}
	}

	/**
	 * Retrieves the zone the player is connected to
	 * 
	 * @return GameZone instance
	 */
	public GameZone getZone() {
		return zone;
	}

	/**
	 * Disconnects the player
	 */
	public void disconnect() {
		// Handle disconnect preparations
		onLeaveZone(this.zone);
		// TODO

		// Close connection if needed
		if (client.isConnected())
			client.disconnect();

		// Desubscribe groups
		for (RoomGroup group : getSubscribedRoomGroups())
			desubscribeFromGroup(group);

		// Leave rooms
		for (RoomInfo room : getJoinedRooms()) {
			leaveRoom(room);
		}
		for (RoomInfo room : getSpectatingRooms()) {
			stopSpectatingRoom(room);
		}

		// Fire disconnect
		logger.info("Player disconnected: " + account.getAccountID() + " (was " + save.getUsername() + ", ID: "
				+ save.getSaveID() + ")");
		EventBus.getInstance().dispatchEvent(new PlayerDisconnectEvent(this));

		// Detach events
		EventBus.getInstance().removeAllEventsFromReceiver(events);
	}

	/**
	 * Joins MMO rooms
	 * 
	 * @param roomName Room name
	 */
	public void joinMmoRoom(String roomName) {
		// Log
		logger.info("Finding room for player " + save.getUsername() + " of group " + roomName);

		// Create group if needed
		RoomGroup group = sodZone.getRoomGroup(roomName);
		if (group == null)
			group = sodZone.addRoomGroup(roomName);

		// Load permissions
		PermissionContext perms = PermissionContext.getFor(account);

		// Find room
		RoomInfo room = null;

		// Check save data
		try {
			if (save.getSaveData().entryExists("override_mmo_room")) {
				// Load room
				String targetRoom = save.getSaveData().getEntry("override_mmo_room").getAsString();
				save.getSaveData().deleteEntry("override_mmo_room");

				// Try to select it
				RoomInfo r = group.getRoom(targetRoom);
				if (r != null)
					room = r;
			}
		} catch (IOException e) {
		}

		// Select room
		if (room == null) {
			boolean checkSocial = true;
			for (int i = 0; i < 2; i++) {
				for (RoomInfo potentialRoom : Stream.of(group.getRooms())
						.sorted((t1, t2) -> -Integer.compare(t1.getUserCount(), t2.getUserCount()))
						.toArray(t -> new RoomInfo[t])) {
					// Check permission to override limit
					if (!perms.hasPermission("mmo.overrides.moderator.ignoreuserlimit", PermissionLevel.MODERATOR)) {
						if (potentialRoom.getUserCount() >= potentialRoom.getUserLimit()) {
							// Skip room
							continue;
						}
					}

					// Check friends, friends have priority
					if (checkSocial) {
						room = room;
						// TODO
					}

					// Select
					room = potentialRoom;
					break;
				}

				// Check result
				if (room != null)
					break;

				// Try without social checks
				checkSocial = false;
			}
		}

		// Create room if needed
		EdgeMMOServer server = client.getObject(EdgeMMOServer.class);
		if (room == null) {
			// Generate ID
			String id = group.getName() + "_" + Integer.toString(rnd.nextInt(0, Integer.MAX_VALUE), 16);
			while (group.getRoom(id) != null)
				id = group.getName() + "_" + Integer.toString(rnd.nextInt(0, Integer.MAX_VALUE), 16);

			// Create room
			short userLimit = server.getConfiguration().roomUserLimit;
			if (server.getConfiguration().roomUserLimits.containsKey(roomName))
				userLimit = server.getConfiguration().roomUserLimits.get(roomName);
			room = group.addGameRoom(id, false, false, userLimit, (short) 0);
			room.addDynamicVariable("WE_ScoutAttack_End", "sod.rooms.any.vars.we_scoutattack_end");
		}

		// Join room
		joinRoom(room);
	}

	/**
	 * Joins MMO rooms
	 * 
	 * @param roomName Room name
	 * @param roomID   Specific room ID to join
	 */
	public void joinMmoRoom(String roomName, String roomID) {
		// Create group if needed
		RoomGroup group = sodZone.getRoomGroup(roomName);
		if (group == null)
			group = sodZone.addRoomGroup(roomName);

		// Find room
		RoomInfo room = group.getRoom(roomID);

		// Create room if needed
		EdgeMMOServer server = client.getObject(EdgeMMOServer.class);
		if (room == null) {
			// Create room
			short userLimit = server.getConfiguration().roomUserLimit;
			if (server.getConfiguration().roomUserLimits.containsKey(roomName))
				userLimit = server.getConfiguration().roomUserLimits.get(roomName);
			room = group.addGameRoom(roomID, false, roomID.equalsIgnoreCase("admin")
					|| roomID.equalsIgnoreCase("moderator") || roomID.equalsIgnoreCase("staff"), userLimit, (short) 0);
			room.addDynamicVariable("WE_ScoutAttack_End", "sod.rooms.any.vars.we_scoutattack_end");
		}

		// Check
		if (room != null && (room.isPasswordProtected() || room.getVariable("EDGE_PermissionLevel_Requirement") != null
				|| room.getVariable("EDGE_Permission_Requirement") != null)) {
			// Protected room
			// Use join key

			// Get perm level
			String levelName = "moderator";
			PermissionLevel level = PermissionLevel.MODERATOR;
			if (roomID.equalsIgnoreCase("admin")) {
				levelName = "admin";
				level = PermissionLevel.ADMINISTRATOR;
			}
			RoomVariable permVar = room.getVariable("EDGE_PermissionLevel_Requirement");
			if (permVar != null) {
				String lv = permVar.getValue().toString();
				switch (lv) {

				case "guest": {
					levelName = "guest";
					level = PermissionLevel.GUEST;
					break;
				}

				case "player": {
					levelName = "player";
					level = PermissionLevel.PLAYER;
					break;
				}

				case "trial_moderator": {
					levelName = "trialmoderator";
					level = PermissionLevel.TRIAL_MODERATOR;
					break;
				}

				case "admin":
				case "administrator": {
					levelName = "admin";
					level = PermissionLevel.ADMINISTRATOR;
					break;
				}

				case "developer": {
					levelName = "developer";
					level = PermissionLevel.DEVELOPER;
					break;
				}

				case "operator": {
					levelName = "operator";
					level = PermissionLevel.OPERATOR;
					break;
				}

				}
			}

			// Permission
			String perm = "mmo.overrides.moderator." + levelName + "." + roomID;
			if (room.getVariable("EDGE_Permission_Requirement") != null)
				perm = room.getVariable("EDGE_Permission_Requirement").getValue().toString();

			// Load permissions
			PermissionContext perms = PermissionContext.getFor(account);

			// Check perms
			if (!perms.hasPermission(perm, level)) {
				// Reject
				logger.warn("Player " + save.getUsername() + " (" + save.getSaveID()
						+ ") attempted to join restricted room " + roomID + " (" + roomName + ")");
				return;
			}
		}

		// Join room
		joinRoom(room);
	}

	private void onJoinZone(GameZone zone) {
		// Join zone
		zone = zone;
		// TODO

		// Dispatch join zone event
		EventBus.getInstance().dispatchEvent(new PlayerJoinZoneEvent(this, this.zone));
	}

	private void onLeaveZone(GameZone zone) {
		// Leave zone
		zone = zone;
		// TODO

		// Dispatch leave zone event
		EventBus.getInstance().dispatchEvent(new PlayerLeaveZoneEvent(this, this.zone));
	}

	private class EventContainer implements IEventReceiver {

		@EventListener
		public void joinedGroup(PlayerRoomGroupSubscribeEvent ev) {
			// Check
			if (ev.getPlayer().getSave().getSaveID().equals(getSave().getSaveID())) {
				// Add
				synchronized (subscribedGroups) {
					subscribedGroups.add(ev.getRoomGroup());
				}
			}
		}

		@EventListener
		public void leftGroup(PlayerRoomGroupDesubscribeEvent ev) {
			// Check
			if (ev.getPlayer().getSave().getSaveID().equals(getSave().getSaveID())) {
				// Remove
				synchronized (subscribedGroups) {
					subscribedGroups.remove(ev.getRoomGroup());
				}
			}
		}

		@EventListener
		public void joinedRoom(PlayerRoomJoinEvent ev) {
			// Check
			if (ev.getPlayer().getSave().getSaveID().equals(getSave().getSaveID())) {
				// Add
				synchronized (joinedRooms) {
					joinedRooms.add(ev.getRoom());
				}
			}
		}

		@EventListener
		public void leftRoom(PlayerRoomLeaveEvent ev) {
			// Check
			if (ev.getPlayer().getSave().getSaveID().equals(getSave().getSaveID())) {
				// Remove
				synchronized (joinedRooms) {
					joinedRooms.remove(ev.getRoom());
				}
			}
		}

		@EventListener
		public void joinedSpectatorRoom(PlayerRoomJoinSpectatorEvent ev) {
			// Check
			if (ev.getPlayer().getSave().getSaveID().equals(getSave().getSaveID())) {
				// Add
				synchronized (spectatingRooms) {
					spectatingRooms.add(ev.getRoom());
				}
			}
		}

		@EventListener
		public void leftSpectatorRoom(PlayerRoomLeaveSpectatorEvent ev) {
			// Check
			if (ev.getPlayer().getSave().getSaveID().equals(getSave().getSaveID())) {
				// Remove
				synchronized (spectatingRooms) {
					spectatingRooms.remove(ev.getRoom());
				}
			}
		}

	}

}
