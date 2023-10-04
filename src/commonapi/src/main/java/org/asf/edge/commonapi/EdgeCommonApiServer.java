package org.asf.edge.commonapi;

import java.io.IOException;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.commonapi.http.*;
import org.asf.edge.commonapi.http.handlers.api.core.*;
import org.asf.edge.commonapi.http.handlers.TestConnectionHandler;
import org.asf.edge.commonapi.http.handlers.api.accounts.*;
import org.asf.edge.commonapi.http.handlers.api.avatars.*;
import org.asf.edge.commonapi.http.handlers.api.messaging.*;
import org.asf.edge.commonapi.http.handlers.internal.*;
import org.asf.edge.modules.eventbus.EventBus;
import org.asf.edge.commonapi.events.server.*;

import com.google.gson.JsonPrimitive;

import org.asf.edge.common.EdgeServerEnvironment;
import org.asf.edge.common.IBaseServer;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.services.items.impl.ItemManagerImpl;
import org.asf.edge.common.services.messages.WsMessageService;
import org.asf.edge.common.services.messages.impl.WsMessageServiceImpl;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.common.services.ServiceImplementationPriorityLevels;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.common.services.achievements.AchievementManager;
import org.asf.edge.common.services.achievements.impl.AchievementManagerImpl;
import org.asf.edge.common.services.commondata.CommonDataContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.commonapi.config.CommonApiServerConfig;

/**
 * 
 * EDGE Common API server
 * 
 * @author Sky Swimmer
 *
 */
public class EdgeCommonApiServer implements IBaseServer {
	public static final String COMMON_API_VERSION = "1.0.0.A3";

	private Logger logger;
	private CommonApiServerConfig config;

	private ConnectiveHttpServer server;
	private ConnectiveHttpServer internalServer;

	@Override
	public String getVersion() {
		return COMMON_API_VERSION;
	}

	static void printSplash() {
		System.out.println("-------------------------------------------------------------");
		System.out.println("                                                             ");
		System.out.println("    EDGE - Fan-made server software for School of Dragons    ");
		System.out.println("             Common API Server Version: 1.0.0.A3             ");
		System.out.println("                                                             ");
		System.out.println("-------------------------------------------------------------");
		System.out.println("");
		System.out.println("This server implements the following endpoints:");
		System.out.println(" - common.api.jumpstart.com");
		System.out.println(" - user.api.jumpstart.com");
		System.out.println("");
	}

	public EdgeCommonApiServer(CommonApiServerConfig config) {
		this.config = config;
		logger = LogManager.getLogger("COMMONAPI");
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
	 * Retrieves the internal HTTP server instance
	 * 
	 * @return ConnectiveHttpServer instance
	 */
	public ConnectiveHttpServer getInternalServer() {
		return internalServer;
	}

	/**
	 * Retrieves the logger of the common api server
	 * 
	 * @return Logger instance
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Retrieves the common api server configuration
	 * 
	 * @return CommonApiServerConfig instance
	 */
	public CommonApiServerConfig getConfiguration() {
		return config;
	}

	/**
	 * Called to set up the server
	 * 
	 * @throws IOException If setup fails
	 */
	public void setupServer() throws IOException {
		// Set up the servers
		if (config.server == null) {
			// Create adapter
			if (config.https) {
				HashMap<String, String> props = new HashMap<String, String>();
				props.put("address", config.listenAddress);
				props.put("port", Integer.toString(config.listenPort));
				props.put("keystore", config.tlsKeystore);
				props.put("keystore-password", config.tlsKeystorePassword);
				config.server = ConnectiveHttpServer.createNetworked("HTTPS/1.1", props);
				logger.info("Edge common api HTTPS server created with listen address " + config.listenAddress
						+ " and port " + config.listenPort);
			} else {
				HashMap<String, String> props = new HashMap<String, String>();
				props.put("address", config.listenAddress);
				props.put("port", Integer.toString(config.listenPort));
				config.server = ConnectiveHttpServer.createNetworked("HTTP/1.1", props);
				logger.info("Edge common api HTTP server created with listen address " + config.listenAddress
						+ " and port " + config.listenPort);
			}
		}
		if (config.internalServer == null) {
			// Create adapter
			if (config.httpsInternal) {
				HashMap<String, String> props = new HashMap<String, String>();
				props.put("address", config.internalListenAddress);
				props.put("port", Integer.toString(config.internalListenPort));
				props.put("keystore", config.tlsKeystoreInternal);
				props.put("keystore-password", config.tlsKeystorePasswordInternal);
				config.internalServer = ConnectiveHttpServer.createNetworked("HTTPS/1.1", props);
				logger.info("Edge common api internal HTTPS server created with listen address "
						+ config.internalListenAddress + " and port " + config.internalListenPort);
			} else {
				HashMap<String, String> props = new HashMap<String, String>();
				props.put("address", config.internalListenAddress);
				props.put("port", Integer.toString(config.internalListenPort));
				config.internalServer = ConnectiveHttpServer.createNetworked("HTTP/1.1", props);
				logger.info("Edge common api internal HTTP server created with listen address "
						+ config.internalListenAddress + " and port " + config.internalListenPort);
			}
		}

		// Assign servers
		server = config.server;
		internalServer = config.internalServer;

		// Register content source
		logger.debug("Adding case-insensitive content sources...");
		server.setContentSource(new CaseInsensitiveContentSource());
		internalServer.setContentSource(new CaseInsensitiveContentSource());

		// Call event
		logger.debug("Dispatching event...");
		EventBus.getInstance().dispatchEvent(new CommonApiServerSetupEvent(config, this));

		// Register handlers: api
		logger.debug("Configuring api server request handlers...");
		server.registerProcessor(new ChatWebServiceProcessor(this));
		server.registerProcessor(new MessagingWebServiceProcessor(this));
		server.registerProcessor(new MembershipWebServiceProcessor(this));
		server.registerProcessor(new ConfigurationWebServiceProcessor(this));
		server.registerProcessor(new AuthenticationWebServiceV1Processor(this));
		server.registerProcessor(new AuthenticationWebServiceV3Processor(this));
		server.registerProcessor(new AvatarWebServiceProcessor(this));
		server.registerProcessor(new SubscriptionWebServiceProcessor(this));
		server.registerProcessor(new RegistrationWebServiceV3Processor(this));
		server.registerProcessor(new RegistrationWebServiceV4Processor(this));
		server.registerProcessor(new ProfileWebServiceProcessor(this));
		server.registerProcessor(new TestConnectionHandler());

		// Register handlers: internal
		logger.debug("Configuring internal server request handlers...");
		internalServer.registerProcessor(new AccountManagerAPI(this));
		internalServer.registerProcessor(new CommonDataManagerAPI(this));

		// Select achievement manager
		logger.info("Setting up achievement manager...");
		ServiceManager.registerServiceImplementation(AchievementManager.class, new AchievementManagerImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(AchievementManager.class);

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

		// Log
		logger.info("Starting the Common API server...");

		// Start
		server.start();
		logger.info("Common API server started successfully!");
		logger.info("Starting the Common API internal server...");
		internalServer.start();

		// Call event
		EventBus.getInstance().dispatchEvent(new CommonApiServerStartupEvent(config, this));

		// Log
		logger.info("Common API internal server started successfully!");
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
		logger.info("Shutting down the Common API server...");
		try {
			server.stop();
		} catch (IOException e) {
		}
		logger.info("Common API server stopped successfully!");
		logger.info("Shutting down the Common API internal server...");
		try {
			internalServer.stop();
		} catch (IOException e) {
		}
		logger.info("Common API internal server stopped successfully!");
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
		logger.info("Forcefully shutting down the Common API server!");
		try {
			server.stopForced();
		} catch (IOException e) {
		}
		logger.info("Common API server stopped successfully!");
		logger.info("Forcefully shutting down the Common API internal server!");
		try {
			internalServer.stopForced();
		} catch (IOException e) {
		}
		logger.info("Common API internal server stopped successfully!");
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
		server.waitForExit();
		internalServer.waitForExit();
	}

}