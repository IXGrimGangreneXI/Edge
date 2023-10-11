package org.asf.edge.mmoserver;

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
import org.asf.edge.mmoserver.events.server.MMOServerSetupDiscoveryZonesEvent;
import org.asf.edge.mmoserver.events.server.MMOServerSetupEvent;
import org.asf.edge.mmoserver.events.server.MMOServerStartupEvent;
import org.asf.edge.mmoserver.events.variables.DynamicRoomVariableSetupEvent;
import org.asf.edge.mmoserver.networking.SmartfoxServer;
import org.asf.edge.mmoserver.networking.channels.extensions.RoomChannel;
import org.asf.edge.mmoserver.networking.impl.BitswarmSmartfoxServer;
import org.asf.edge.mmoserver.services.ZoneManager;
import org.asf.edge.mmoserver.services.impl.ZoneManagerImpl;
import org.asf.edge.modules.eventbus.EventBus;

import com.google.gson.JsonPrimitive;

import org.asf.edge.common.EdgeServerEnvironment;
import org.asf.edge.common.IBaseServer;
import org.asf.edge.common.io.DataWriter;
import org.asf.edge.common.services.ServiceImplementationPriorityLevels;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.common.services.commondata.CommonDataContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.services.items.impl.ItemManagerImpl;
import org.asf.edge.common.services.messages.WsMessageService;
import org.asf.edge.common.services.messages.impl.WsMessageServiceImpl;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.common.util.HttpUpgradeUtil;
import org.asf.edge.common.util.SimpleBinaryMessageClient;
import org.asf.edge.mmoserver.config.MMOServerConfig;
import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.mmoserver.entities.smartfox.RoomVariable;

/**
 * 
 * EDGE MMO server (smartfox sync server implementation)
 * 
 * @author Sky Swimmer
 *
 */
public class EdgeMMOServer implements IBaseServer {
	public static final String MMO_SERVER_VERSION = "a1.6";

	private Logger logger;
	private MMOServerConfig config;

	private SmartfoxServer server;

	private Socket uplinkSocket;
	private ArrayList<String> mmoZones = new ArrayList<String>();

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
		// TODO

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

		// Select zone manager
		logger.info("Setting up zone manager...");
		ServiceManager.registerServiceImplementation(ZoneManager.class, new ZoneManagerImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(ZoneManager.class);

		// Server watchdog
		logger.info("Starting shutdown and restart watchdog...");
		CommonDataContainer cont = CommonDataManager.getInstance().getContainer("EDGECOMMON");
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

}