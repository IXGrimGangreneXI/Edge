package org.asf.edge.gameplayapi;

import java.io.IOException;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.gameplayapi.http.*;
import org.asf.edge.gameplayapi.http.handlers.gameplayapi.*;
import org.asf.edge.gameplayapi.http.handlers.itemstore.*;
import org.asf.edge.gameplayapi.services.quests.QuestManager;
import org.asf.edge.gameplayapi.services.quests.impl.QuestManagerImpl;
import org.asf.edge.gameplayapi.util.InventoryUtils;
import org.asf.edge.modules.eventbus.EventBus;

import com.google.gson.JsonPrimitive;

import org.asf.edge.gameplayapi.http.handlers.achievements.*;
import org.asf.edge.common.CommonInit;
import org.asf.edge.common.IBaseServer;
import org.asf.edge.common.services.ServiceImplementationPriorityLevels;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.common.services.commondata.CommonDataContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.services.items.impl.ItemManagerImpl;
import org.asf.edge.gameplayapi.events.server.GameplayApiServerSetupEvent;
import org.asf.edge.gameplayapi.events.server.GameplayApiServerStartupEvent;
import org.asf.edge.gameplayapi.config.GameplayApiServerConfig;

/**
 * 
 * EDGE Gameplay API server (content api)
 * 
 * @author Sky Swimmer
 *
 */
public class EdgeGameplayApiServer implements IBaseServer {
	public static final String GAMEPLAY_API_VERSION = "1.0.0.A1";

	private Logger logger;
	private GameplayApiServerConfig config;

	private ConnectiveHttpServer server;

	static void printSplash() {
		System.out.println("-------------------------------------------------------------");
		System.out.println("                                                             ");
		System.out.println("    EDGE - Fan-made server software for School of Dragons    ");
		System.out.println("            Gameplay API Server Version: 1.0.0.A1            ");
		System.out.println("                                                             ");
		System.out.println("-------------------------------------------------------------");
		System.out.println("");
		System.out.println("This server implements the following endpoints:");
		System.out.println(" - contentserver.api.jumpstart.com");
		System.out.println(" - itemstoremission.api.jumpstart.com");
		System.out.println(" - achievement.api.jumpstart.com");
		System.out.println("");
	}

	public EdgeGameplayApiServer(GameplayApiServerConfig config) {
		this.config = config;
		logger = LogManager.getLogger("GAMEPLAYAPI");
	}

	/**
	 * Retrieves the HTTP server instance
	 * 
	 * @return ConnectiveHttpServer instance
	 */
	public ConnectiveHttpServer getServer() {
		return server;
	}

	/**
	 * Retrieves the logger of the gameplay api server
	 * 
	 * @return Logger instance
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Retrieves the gameplay api server configuration
	 * 
	 * @return GameplayApiServerConfig instance
	 */
	public GameplayApiServerConfig getConfiguration() {
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
			// Create adapter
			if (config.https) {
				HashMap<String, String> props = new HashMap<String, String>();
				props.put("address", config.listenAddress);
				props.put("port", Integer.toString(config.listenPort));
				props.put("keystore", config.tlsKeystore);
				props.put("keystore-password", config.tlsKeystorePassword);
				config.server = ConnectiveHttpServer.createNetworked("HTTPS/1.1", props);
				logger.info("Edge gameplay api HTTPS server created with listen address " + config.listenAddress
						+ " and port " + config.listenPort);
			} else {
				HashMap<String, String> props = new HashMap<String, String>();
				props.put("address", config.listenAddress);
				props.put("port", Integer.toString(config.listenPort));
				config.server = ConnectiveHttpServer.createNetworked("HTTP/1.1", props);
				logger.info("Edge gameplay api HTTP server created with listen address " + config.listenAddress
						+ " and port " + config.listenPort);
			}
		}

		// Assign server
		server = config.server;

		// Register content source
		logger.debug("Adding case-insensitive content source...");
		server.setContentSource(new CaseInsensitiveContentSource());

		// Call event
		logger.debug("Dispatching event...");
		EventBus.getInstance().dispatchEvent(new GameplayApiServerSetupEvent(config, this));

		// Register handlers
		logger.debug("Configuring server request handlers...");
		server.registerProcessor(new ContentWebServiceV1Processor(this));
		server.registerProcessor(new ContentWebServiceV2Processor(this));
		server.registerProcessor(new ContentWebServiceV3Processor(this));
		server.registerProcessor(new ContentWebServiceV4Processor(this));
		server.registerProcessor(new RatingWebServiceV1Processor(this));
		server.registerProcessor(new RatingWebServiceV2Processor(this));
		server.registerProcessor(new ChallengeWebServiceProcessor(this));
		server.registerProcessor(new InviteFriendWebServiceProcessor(this));
		server.registerProcessor(new MobileStoreWebServiceProcessor(this));
		server.registerProcessor(new PaymentWebServiceV1Processor(this));
		server.registerProcessor(new PaymentWebServiceV2Processor(this));
		server.registerProcessor(new ScoreWebServiceProcessor(this));
		server.registerProcessor(new ItemStoreWebServiceProcessor(this));
		server.registerProcessor(new MissionWebServiceProcessor(this));
		server.registerProcessor(new AchievementWebServiceV1Processor(this));
		server.registerProcessor(new AchievementWebServiceV2Processor(this));

		// Select item manager
		logger.info("Setting up item manager...");
		ServiceManager.registerServiceImplementation(ItemManager.class, new ItemManagerImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(ItemManager.class);
		InventoryUtils.init();

		// Select quest manager
		logger.info("Setting up quest manager...");
		ServiceManager.registerServiceImplementation(QuestManager.class, new QuestManagerImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(QuestManager.class);

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
		logger.info("Starting the Gameplay API server...");
		server.start();

		// Call event
		EventBus.getInstance().dispatchEvent(new GameplayApiServerStartupEvent(config, this));

		// Log
		logger.info("Gameplay API server started successfully!");
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
		logger.info("Shutting down the Gameplay API server...");
		try {
			server.stop();
		} catch (IOException e) {
		}
		logger.info("Gameplay API server stopped successfully!");
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
		logger.info("Forcefully shutting down the Gameplay API server!");
		try {
			server.stopForced();
		} catch (IOException e) {
		}
		logger.info("Gameplay API server stopped successfully!");
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