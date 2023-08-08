package org.asf.edge.mmoserver;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.mmoserver.events.clients.ClientConnectedEvent;
import org.asf.edge.mmoserver.events.server.MMOServerSetupEvent;
import org.asf.edge.mmoserver.events.server.MMOServerStartupEvent;
import org.asf.edge.mmoserver.networking.SmartfoxServer;
import org.asf.edge.mmoserver.networking.impl.BitswarmSmartfoxServer;
import org.asf.edge.modules.eventbus.EventBus;

import com.google.gson.JsonPrimitive;

import org.asf.edge.common.CommonInit;
import org.asf.edge.common.IBaseServer;
import org.asf.edge.common.services.ServiceImplementationPriorityLevels;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.common.services.commondata.CommonDataContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.services.items.impl.ItemManagerImpl;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.mmoserver.config.MMOServerConfig;

/**
 * 
 * EDGE MMO server (smartfox sync server implementation)
 * 
 * @author Sky Swimmer
 *
 */
public class EdgeMMOServer implements IBaseServer {
	public static final String MMO_SERVER_VERSION = "1.0.0.A1";

	private Logger logger;
	private MMOServerConfig config;

	private SmartfoxServer server;

	static void printSplash() {
		System.out.println("-------------------------------------------------------------");
		System.out.println("                                                             ");
		System.out.println("    EDGE - Fan-made server software for School of Dragons    ");
		System.out.println("                 MMO Server Version 1.0.0.A1                 ");
		System.out.println("                                                             ");
		System.out.println("-------------------------------------------------------------");
		System.out.println("");
	}

	public EdgeMMOServer(MMOServerConfig config) {
		this.config = config;
		logger = LogManager.getLogger("MMOSERVER");
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
		logger.debug("Configuring server packet handlers...");
		// TODO

		// Select item manager
		logger.info("Setting up item manager...");
		ServiceManager.registerServiceImplementation(ItemManager.class, new ItemManagerImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(ItemManager.class);

		// Load filter
		logger.info("Loading text filter...");
		TextFilterService.getInstance();

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
							CommonInit.restartPending = true;
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

}