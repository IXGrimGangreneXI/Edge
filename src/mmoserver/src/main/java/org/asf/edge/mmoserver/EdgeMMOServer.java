package org.asf.edge.mmoserver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.mmoserver.events.clients.ClientConnectedEvent;
import org.asf.edge.mmoserver.events.clients.ClientDisconnectedEvent;
import org.asf.edge.mmoserver.events.players.PlayerRoomJoinEvent;
import org.asf.edge.mmoserver.events.players.PlayerRoomJoinSpectatorEvent;
import org.asf.edge.mmoserver.events.server.MMOServerSetupDiscoveryZonesEvent;
import org.asf.edge.mmoserver.events.server.MMOServerSetupEvent;
import org.asf.edge.mmoserver.events.server.MMOServerStartupEvent;
import org.asf.edge.mmoserver.events.variables.DynamicRoomVariableSetupEvent;
import org.asf.edge.mmoserver.events.variables.UserVariableAddedEvent;
import org.asf.edge.mmoserver.events.variables.UserVariableValueUpdateEvent;
import org.asf.edge.mmoserver.networking.SmartfoxClient;
import org.asf.edge.mmoserver.networking.SmartfoxServer;
import org.asf.edge.mmoserver.networking.channels.extensions.ChatChannel;
import org.asf.edge.mmoserver.networking.channels.extensions.RoomChannel;
import org.asf.edge.mmoserver.networking.channels.extensions.ServerTimeChannel;
import org.asf.edge.mmoserver.networking.channels.extensions.UserVarsChannel;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.uservars.ClientboundRefreshUserVarsMessage;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.uservars.ClientboundSetPositionalVarsMessage;
import org.asf.edge.mmoserver.networking.channels.extensions.messages.uservars.ClientboundSetUserVarsMessage;
import org.asf.edge.mmoserver.networking.channels.smartfox.ExtensionChannel;
import org.asf.edge.mmoserver.networking.channels.smartfox.extension.packets.clientbound.ClientboundExtensionMessage;
import org.asf.edge.mmoserver.networking.impl.BitswarmSmartfoxServer;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;
import org.asf.edge.mmoserver.services.ZoneManager;
import org.asf.edge.mmoserver.services.impl.ZoneManagerImpl;
import org.asf.nexus.common.io.DataReader;
import org.asf.nexus.common.io.DataWriter;
import org.asf.nexus.common.services.ServiceImplementationPriorityLevels;
import org.asf.nexus.common.services.ServiceManager;
import org.asf.nexus.events.EventBus;
import org.asf.nexus.events.EventListener;
import org.asf.nexus.events.IEventReceiver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.asf.edge.common.EdgeServerEnvironment;
import org.asf.edge.common.IEdgeBaseServer;
import org.asf.edge.common.services.commondata.CommonKvDataContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.services.items.impl.ItemManagerImpl;
import org.asf.edge.common.services.messages.WsMessageService;
import org.asf.edge.common.services.messages.impl.WsMessageServiceImpl;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.common.util.HttpUpgradeUtil;
import org.asf.edge.common.util.LogWindow;
import org.asf.edge.common.util.SimpleBinaryMessageClient;
import org.asf.edge.mmoserver.config.MMOServerConfig;
import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.mmoserver.entities.positional.PositionalVariableContainer;
import org.asf.edge.mmoserver.entities.smartfox.RoomInfo;
import org.asf.edge.mmoserver.entities.smartfox.RoomVariable;
import org.asf.edge.mmoserver.entities.smartfox.SfsUser;
import org.asf.edge.mmoserver.entities.smartfox.UserVariable;

/**
 * 
 * EDGE MMO server (smartfox sync server implementation)
 * 
 * @author Sky Swimmer
 *
 */
public class EdgeMMOServer implements IEdgeBaseServer {
	public static final String MMO_SERVER_VERSION = "a1.6";

	private Logger logger;
	private MMOServerConfig config;

	private SmartfoxServer server;

	private Socket uplinkSocket;
	private ArrayList<String> mmoZones = new ArrayList<String>();

	private EventContainer events = new EventContainer();

	@Override
	public String getVersion() {
		return MMO_SERVER_VERSION;
	}

	static void printSplash() {
		System.out.println("-------------------------------------------------------------");
		System.out.println("                                                             ");
		System.out.println("    EDGE - Fan-made server software for School of Dragons    ");
		System.out.println("                   MMO Server Version a1.6                   ");
		System.out.println("                                                             ");
		System.out.println("-------------------------------------------------------------");
		System.out.println("");
	}

	public EdgeMMOServer(MMOServerConfig config) {
		this.config = config;
		logger = LogManager.getLogger("MMOSERVER");
	}

	/**
	 * Retrieves the SoD zone names currently registered for server discovery
	 * 
	 * @return Array of SoD zone names (NOT THE SMARTFOX ZONES)
	 */
	public String[] getSodZoneNames() {
		return mmoZones.toArray(t -> new String[t]);
	}

	/**
	 * Adds SoD zones to server discovery
	 * 
	 * @param zoneName Zone to add
	 */
	public void addSodZone(String zoneName) {
		if (mmoZones.contains(zoneName))
			return;
		mmoZones.add(zoneName);
		getLogger().info("Added SoD MMO zone for server discovery: " + zoneName);
	}

	/**
	 * Removes SoD zones from server discovery
	 * 
	 * @param zoneName Zone to remove
	 */
	public void removeSodZone(String zoneName) {
		if (!mmoZones.contains(zoneName))
			return;
		mmoZones.remove(zoneName);
		getLogger().info("Removed SoD MMO zone from server discovery: " + zoneName);
	}

	/**
	 * Retrieves the SFS server instance
	 * 
	 * @return SodSfsServer instance
	 */
	public SmartfoxServer getServer() {
		return server;
	}

	/**
	 * Retrieves the logger of the MMO server
	 * 
	 * @return Logger instance
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Retrieves the MMO server configuration
	 * 
	 * @return MMOServerConfig instance
	 */
	public MMOServerConfig getConfiguration() {
		return config;
	}

	/**
	 * Called to set up the server
	 * 
	 * @throws IOException If setup fails
	 */
	public void setupServer() throws IOException {
		// Set up the server
		if (config.server == null) {
			config.server = new BitswarmSmartfoxServer(config.listenAddress, config.listenPort);
			logger.info("Edge bitswarm MMO server created with listen address " + config.listenAddress + " and port "
					+ config.listenPort);
		}

		// Assign server
		server = config.server;

		// Call event
		logger.debug("Dispatching event...");
		EventBus.getInstance().dispatchEvent(new MMOServerSetupEvent(config, this));

		// Register handlers
		logger.debug("Configuring server event handlers...");
		server.getEventBus().addEventHandler(ClientConnectedEvent.class, event -> {
			// Add object
			event.getClient().setObject(EdgeMMOServer.class, this);
		});
		server.getEventBus().addEventHandler(ClientDisconnectedEvent.class, event -> {
			// Check
			PlayerInfo player = event.getClient().getObject(PlayerInfo.class);
			if (player != null) {
				// Disconnect player
				player.disconnect();
			}
		});

		// Register packet handlers
		logger.debug("Configuring server packet channels...");
		server.registerChannel(new RoomChannel());
		server.registerChannel(new UserVarsChannel());
		server.registerChannel(new ServerTimeChannel());
		server.registerChannel(new ChatChannel());
		// TODO

		// Bind command handler
		logger.info("Binding command handler to GUI terminal...");
		LogWindow.commandCallback = t -> EdgeServerEnvironment.executeConsoleCommand(t);

		// Select item manager
		logger.info("Setting up item manager...");
		ServiceManager.registerServiceImplementation(ItemManager.class, new ItemManagerImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(ItemManager.class);

		// Select message service
		logger.info("Setting up message service...");
		ServiceManager.registerServiceImplementation(WsMessageService.class, new WsMessageServiceImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(WsMessageService.class);

		// Load filter
		logger.info("Loading text filter...");
		TextFilterService.getInstance();

		// Attach events
		logger.info("Attaching room events...");
		EventBus.getInstance().addEventHandler(DynamicRoomVariableSetupEvent.class, ev -> {
			// Dynamic variables
			setupDynamicVar(ev.getDynamicAssignmentKey(), ev.getVariable());
		});
		EventBus.getInstance().addEventHandler(UserVariableValueUpdateEvent.class, ev -> {
			updateVariable(ev.getUser(), ev.getVariable());
		});
		EventBus.getInstance().addEventHandler(UserVariableAddedEvent.class, ev -> {
			updateVariable(ev.getUser(), ev.getVariable());
		});

		// Select zone manager
		logger.info("Setting up zone manager...");
		ServiceManager.registerServiceImplementation(ZoneManager.class, new ZoneManagerImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(ZoneManager.class);

		// Server watchdog
		logger.info("Starting shutdown and restart watchdog...");
		CommonKvDataContainer cont = CommonDataManager.getInstance().getKeyValueContainer("EDGECOMMON");
		try {
			if (!cont.entryExists("shutdown")) {
				lastShutdownTime = System.currentTimeMillis();
				cont.setEntry("shutdown", new JsonPrimitive(lastShutdownTime));
			} else
				lastShutdownTime = cont.getEntry("shutdown").getAsLong();
			if (!cont.entryExists("restart")) {
				lastRestartTime = System.currentTimeMillis();
				cont.setEntry("restart", new JsonPrimitive(lastRestartTime));
			} else
				lastRestartTime = cont.getEntry("restart").getAsLong();
		} catch (IOException e) {
		}
		AsyncTaskManager.runAsync(() -> {
			while (true) {
				// Check restart and shutdown
				try {
					long shutdown = cont.getEntry("shutdown").getAsLong();
					if (shutdown > lastShutdownTime) {
						// Trigger shutdown
						if (isRunning()) {
							stopServer();
							break;
						}
					}
					long restart = cont.getEntry("restart").getAsLong();
					if (restart > lastRestartTime) {
						// Trigger restart
						if (isRunning()) {
							EdgeServerEnvironment.restartPending = true;
							stopServer();
							break;
						}
					}
				} catch (IOException e) {
				}
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
				}
			}
		});

		// Setup SoD zones
		logger.info("Setting up SoD zones for server discovery...");
		addSodZone("FarmingThawfestDO");
		addSodZone("HubHiddenWorldNBDO");
		addSodZone("HelheimsGateDO");
		addSodZone("RacingDragon");
		addSodZone("DEClubhouseINTDO");
		addSodZone("TargetPracticeDO");
		addSodZone("BerkCloudsDO");
		addSodZone("HatcheryINTDO");
		addSodZone("GlacierIslandDO");
		addSodZone("HubDragonsEdgeDO");
		addSodZone("ArmorWingIslandDO");
		addSodZone("OpenOceanDO");
		addSodZone("GauntletDO");
		addSodZone("HubAuctionIslandDO");
		addSodZone("GreatHallSchoolIntDO");
		addSodZone("DarkDeepDO");
		addSodZone("ShipGraveyardDO");
		addSodZone("HubHiddenWorldDO");
		addSodZone("HubDragonIslandDO");
		addSodZone("HubTradewindIslandDO");
		addSodZone("HubLookoutDO");
		addSodZone("HubEruptodonIslandDO");
		addSodZone("HubSchoolDO");
		addSodZone("HubDragonIslandINTDO");
		addSodZone("HubVanaheimDO");
		addSodZone("HubArctic01DO");
		addSodZone("TitanIslandDO");
		addSodZone("MyRoomINTDO");
		addSodZone("HubBerkDO");
		addSodZone("FarmingDreadfallDO");
		addSodZone("HobblegruntIslandDO");
		addSodZone("PirateQueenShipDO");
		addSodZone("HubWarlordIslandDO");
		addSodZone("GreatHallBerkIntDO");
		addSodZone("DragonRacingDO");
		addSodZone("ZipplebackIslandDO");
		addSodZone("DarkDeepCavesDO");
		addSodZone("FarmingDO");
		addSodZone("HatcheryINT02DO");
		addSodZone("ScuttleclawIslandDO");
		addSodZone("HubWilderness01DO");
		addSodZone("FarmingOceanDO");
		addSodZone("HubBerkNewDO");
		addSodZone("HubTrainingDO");
		addSodZone("HubDeathsongIslandDO");
		addSodZone("HubArcticINTDO");
		addSodZone("ArenaFrenzyDO");
		addSodZone("MudrakerIslandDO");
		addSodZone("BerkFarmDO");
		addSodZone("HubCenoteDO");
		EventBus.getInstance().dispatchEvent(new MMOServerSetupDiscoveryZonesEvent(config, this));
	}

	private long lastRestartTime;
	private long lastShutdownTime;

	/**
	 * Starts the server
	 */
	public void startServer() throws IOException {
		if (server == null)
			throw new IllegalArgumentException("Server has not been set up");
		if (server.isRunning())
			throw new IllegalArgumentException("Server is already running");

		// Start server
		logger.info("Starting the MMO server...");
		server.start();
		EventBus.getInstance().addAllEventsFromReceiver(events);

		// Call event
		EventBus.getInstance().dispatchEvent(new MMOServerStartupEvent(config, this));

		// Start discovery
		logger.info("Attempting to start server discovery...");
		AsyncTaskManager.runAsync(() -> {
			while (true) {
				Socket sock;
				try {
					String uBase = config.commonApiUplinkURL;
					if (!uBase.endsWith("/"))
						uBase += "/";
					sock = HttpUpgradeUtil.upgradeRequest(uBase + "mmoserver/edgemmopublish", "GET", null, -1, Map.of(),
							new HashMap<String, String>(), "EDGEBINPROT/MMOUPLINK", "EDGEBINPROT/MMOUPLINK");
				} catch (IOException e) {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e1) {
					}
					continue;
				}

				// Success, attempt handshake
				try {
					// Write handshake
					DataWriter writer = new DataWriter(sock.getOutputStream());
					writer.writeString(config.discoveryAddress);
					writer.writeInt(config.discoveryPort);
					writer.writeBoolean(config.isBackupServer);
					writer.writeString(config.discoveryRootZone);
					String[] zones = getSodZoneNames();
					writer.writeInt(zones.length);
					for (String zone : zones)
						writer.writeString(zone);
				} catch (IOException e) {
					// Error
					try {
						sock.close();
					} catch (IOException e1) {
					}
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e1) {
					}
					continue;
				}

				// Log
				logger.info("Successfully started MMO uplink.");

				// Handle messages
				try {
					SimpleBinaryMessageClient client = new SimpleBinaryMessageClient((packet, cl) -> {
						try {
							// Handle packet
							if (packet.data.length > 0) {
								// Create reader
								DataReader rd = new DataReader(new ByteArrayInputStream(packet.data));
								byte type = rd.readRawByte();

								// Handle type
								switch (type) {

								// Update message queue
								case 0: {
									// Read user
									String userId = rd.readString();

									// Create packet
									ClientboundExtensionMessage msg = new ClientboundExtensionMessage();
									msg.command = "SPMN";
									msg.payload = new SmartfoxPayload();
									msg.payload.setStringArray("arr", new String[] { "SPMN", "-1" });

									// Find user
									for (SmartfoxClient c : server.getClients()) {
										PlayerInfo plr = c.getObject(PlayerInfo.class);
										if (plr != null && plr.getAccount().getAccountID().equals(userId)) {
											// Send
											c.getChannel(ExtensionChannel.class).sendPacket(msg);
										}
									}

									break;
								}

								}
							}
						} catch (IOException e) {
						}

						// Return
						return true;
					}, sock.getInputStream(), sock.getOutputStream());
					client.start();
				} catch (Exception e) {
				}

				// Disconnected
				logger.info("Disconnected from Edge common API!");
				try {
					sock.close();
				} catch (IOException e1) {
				}
			}
		});

		// Log
		logger.info("MMO server started successfully!");
	}

	/**
	 * Stops the server
	 */
	public void stopServer() {
		if (server == null)
			throw new IllegalArgumentException("Server has not been set up");
		if (!server.isRunning())
			throw new IllegalArgumentException("Server is not running");

		// Stop the server
		logger.info("Shutting down the MMO uplink...");
		try {
			if (uplinkSocket != null)
				uplinkSocket.close();
		} catch (IOException e) {
		}
		logger.info("Shutting down the MMO server...");
		try {
			server.stop();
		} catch (IOException e) {
		}
		EventBus.getInstance().removeAllEventsFromReceiver(events);
		logger.info("MMO server stopped successfully!");
	}

	/**
	 * Stops the server forcefully
	 */
	public void killServer() {
		if (server == null)
			throw new IllegalArgumentException("Server has not been set up");
		if (!server.isRunning())
			throw new IllegalArgumentException("Server is not running");

		// Kill the server
		logger.info("Forcefully shutting down the MMO server!");
		try {
			if (uplinkSocket != null)
				uplinkSocket.close();
		} catch (IOException e) {
		}
		try {
			server.stopForced();
		} catch (IOException e) {
		}
		EventBus.getInstance().removeAllEventsFromReceiver(events);
		logger.info("MMO server stopped successfully!");
	}

	/**
	 * Checks if the server is running
	 * 
	 * @return True if running, false otherwise
	 */
	public boolean isRunning() {
		if (server == null)
			return false;
		return server.isRunning();
	}

	/**
	 * Waits for the server to quit
	 */
	public void waitForExit() {
		// Wait for server to stop
		while (isRunning())
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				break;
			}
	}

	private void setupDynamicVar(String dynamicAssignmentKey, RoomVariable variable) {
		switch (dynamicAssignmentKey) {

		// WE_ScoutAttack_End
		case "sod.rooms.any.vars.we_scoutattack_end": {
			// TODO
			variable = variable;
			break;
		}

		// WE_ScoutAttack
		case "sod.rooms.admin.vars.we_scoutattack": {
			// TODO
			variable = variable;
			break;
		}

		// WEN_ScoutAttack
		case "sod.rooms.admin.vars.wen_scoutattack": {
			// TODO
			variable = variable;
			break;
		}

		}
	}

	private void updateVariable(SfsUser user, UserVariable variable) {
		// Check variable
		switch (variable.getName()) {

		// Avatar data
		case "A": {
			// Parse data
			JsonObject avi = JsonParser.parseString(variable.getValue().toString()).getAsJsonObject();
			boolean updated = false;

			// Check ID
			if (avi.get("Id").isJsonNull()) {
				// Update
				avi.addProperty("Id", user.getUserNumericID());
				updated = true;
			}

			// Check
			if (updated) {
				// Save
				variable.setValue(avi.toString());
			}

			// Break
			break;
		}

		}
	}

	private class EventContainer implements IEventReceiver {

		private void joinedRoom(RoomInfo room, PlayerInfo plr) {
			// Sync other players
			for (SfsUser usr : room.getSfsUserObjects()) {
				// Skip self
				if (usr.getUserID().equals(plr.getSave().getSaveID()))
					continue;

				// Create packet
				ClientboundSetUserVarsMessage pkt = new ClientboundSetUserVarsMessage();
				ClientboundSetPositionalVarsMessage positional = new ClientboundSetPositionalVarsMessage();

				// Populate variable data
				ClientboundSetUserVarsMessage.UserVarUpdate u = new ClientboundSetUserVarsMessage.UserVarUpdate();
				u.roomID = room.getRoomID();
				u.userID = usr.getUserNumericID();
				u.vars.put("UID", usr.getUserID());
				for (UserVariable var : usr.getVariables())
					u.vars.put(var.getName(), var.getValue());
				pkt.varUpdates.add(u);

				// Populate positional variables
				PositionalVariableContainer varCont = usr.getObject(PositionalVariableContainer.class);
				if (varCont != null) {
					ClientboundSetPositionalVarsMessage.UserVarUpdate u2 = new ClientboundSetPositionalVarsMessage.UserVarUpdate();
					u2.roomID = room.getRoomID();
					u2.userID = usr.getUserNumericID();
					u2.vars.put("ST", System.currentTimeMillis());
					u2.vars.put("NT", System.currentTimeMillis());
					u2.vars.put("UID", usr.getUserID());
					u2.vars.putAll(varCont.positionalVariables);
					positional.varUpdates.add(u2);
				}

				// Send
				try {
					plr.getClient().getExtensionChannel(UserVarsChannel.class).sendMessage(positional);
				} catch (IOException e) {
				}
				try {
					plr.getClient().getExtensionChannel(UserVarsChannel.class).sendMessage(pkt);
				} catch (IOException e) {
				}

				// Send refresh
				try {
					ClientboundRefreshUserVarsMessage ref = new ClientboundRefreshUserVarsMessage();
					ref.userID = usr.getUserNumericID();
					plr.getClient().getExtensionChannel(UserVarsChannel.class).sendMessage(ref);
				} catch (IOException e) {
				}
			}
		}

		@EventListener
		public void joinRoom(PlayerRoomJoinEvent ev) {
			RoomInfo room = ev.getRoom();
			PlayerInfo plr = ev.getPlayer();

			// Join
			joinedRoom(room, plr);
		}

		@EventListener
		public void joinSpectate(PlayerRoomJoinSpectatorEvent ev) {
			RoomInfo room = ev.getRoom();
			PlayerInfo plr = ev.getPlayer();

			// Join
			joinedRoom(room, plr);
		}
	}

}