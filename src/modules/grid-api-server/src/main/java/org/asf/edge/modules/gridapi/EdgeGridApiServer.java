package org.asf.edge.modules.gridapi;

import java.io.IOException;
import java.util.HashMap;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.RemoteClient;
import org.asf.edge.common.IBaseServer;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.common.util.LogWindow;
import org.asf.edge.modules.eventbus.EventBus;
import org.asf.edge.modules.gridapi.commands.CommandContext;
import org.asf.edge.modules.gridapi.config.GridApiServerConfig;
import org.asf.edge.modules.gridapi.events.server.GridApiServerSetupEvent;
import org.asf.edge.modules.gridapi.events.server.GridApiServerStartupEvent;
import org.asf.edge.modules.gridapi.http.CaseInsensitiveContentSource;
import org.asf.edge.modules.gridapi.http.handlers.ApiConnectorHandler;
import org.asf.edge.modules.gridapi.http.services.*;
import org.asf.edge.modules.gridapi.http.services.phoenix.PhoenixAuthWebService;
import org.asf.edge.modules.gridapi.http.services.phoenix.PhoenixIdentityWebService;
import org.asf.edge.modules.gridapi.http.services.phoenix.PhoenixServersWebService;
import org.asf.edge.modules.gridapi.http.services.phoenix.PhoenixTokensWebService;
import org.asf.edge.modules.gridapi.phoenixconnector.IApiConnectorRequestHandler;

import com.google.gson.JsonObject;

public class EdgeGridApiServer implements IBaseServer {
	public static final String GRID_API_VERSION = "1.0.0.A1";

	private Logger logger;
	private GridApiServerConfig config;

	private ConnectiveHttpServer server;

	private HashMap<String, IApiConnectorRequestHandler> connectorHandlers = new HashMap<String, IApiConnectorRequestHandler>();

	@Override
	public String getVersion() {
		return GRID_API_VERSION;
	}

	static void printSplash() {
		System.out.println("-------------------------------------------------------------");
		System.out.println("                                                             ");
		System.out.println("    EDGE - Fan-made server software for School of Dragons    ");
		System.out.println("              Grid API Server Version: 1.0.0.A1              ");
		System.out.println("                                                             ");
		System.out.println("-------------------------------------------------------------");
		System.out.println("");
	}

	public EdgeGridApiServer(GridApiServerConfig config) {
		this.config = config;
		logger = LogManager.getLogger("GRIDAPI");
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
	 * Retrieves the logger of the grid api server
	 * 
	 * @return Logger instance
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Retrieves the grid api server configuration
	 * 
	 * @return GridApiServerConfig instance
	 */
	public GridApiServerConfig getConfiguration() {
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
				logger.info("Edge grid api HTTPS server created with listen address " + config.listenAddress
						+ " and port " + config.listenPort);
			} else {
				HashMap<String, String> props = new HashMap<String, String>();
				props.put("address", config.listenAddress);
				props.put("port", Integer.toString(config.listenPort));
				config.server = ConnectiveHttpServer.createNetworked("HTTP/1.1", props);
				logger.info("Edge grid api HTTP server created with listen address " + config.listenAddress
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
		EventBus.getInstance().dispatchEvent(new GridApiServerSetupEvent(config, this));

		// Register handlers
		logger.debug("Configuring server request handlers...");
		server.registerProcessor(new PhoenixAuthWebService(this));
		server.registerProcessor(new PhoenixServersWebService(this));
		server.registerProcessor(new PhoenixTokensWebService(this));
		server.registerProcessor(new PhoenixIdentityWebService(this));
		server.registerProcessor(new GridAccountManagerWebService(this));
		server.registerProcessor(new GridUtilityWebService(this));
		server.registerProcessor(new GridGameWebService(this));
		server.registerProcessor(new GridGameWebService(this));
		server.registerProcessor(new GridGameplayWebService(this));
		server.registerProcessor(new ApiConnectorHandler(this));
		// TODO

		// Register connector handlers
		logger.debug("Configuring Grid API connector request handlers...");
		// TODO

		// Bind command handler
		if (LogWindow.commandCallback == null) {
			logger.info("Binding command handler to GUI terminal...");
			LogWindow.commandCallback = t -> executeConsoleCommand(t);
		}

		// Load filter
		logger.info("Loading text filter...");
		TextFilterService.getInstance();

		// Load filter
		logger.info("Loading text filter...");
		TextFilterService.getInstance();
	}

	/**
	 * Registers Grid API connector request handlers
	 * 
	 * @param handler Handler to register
	 */
	public void registerApiConnectorRequestHandler(IApiConnectorRequestHandler handler) {
		connectorHandlers.put(handler.requestType(), handler);
	}

	private CommandContext cmdCtx = CommandContext.openSession();

	private void executeConsoleCommand(String command) {
		if (command.isEmpty())
			return;

		// Run
		cmdCtx.runCommand(command, t -> {
			logger.info(t);
		});
	}

	/**
	 * Starts the server
	 */
	public void startServer() throws IOException {
		if (server == null)
			throw new IllegalArgumentException("Server has not been set up");
		if (server.isRunning())
			throw new IllegalArgumentException("Server is already running");

		// Start server
		logger.info("Starting the Grid API server...");
		server.start();

		// Call event
		EventBus.getInstance().dispatchEvent(new GridApiServerStartupEvent(config, this));

		// Log
		logger.info("Grid API server started successfully!");
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
		logger.info("Shutting down the Grid API server...");
		try {
			server.stop();
		} catch (IOException e) {
		}
		logger.info("Grid API server stopped successfully!");
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
		logger.info("Forcefully shutting down the Grid API server!");
		try {
			server.stopForced();
		} catch (IOException e) {
		}
		logger.info("Grid API server stopped successfully!");
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

	/**
	 * Handles Phoenix API connector requests
	 * 
	 * @param type             Request type
	 * @param payload          Request payload
	 * @param client           Requesting client
	 * @param responseCallback Response callback
	 */
	public void handlePhoenixApiConnectorRequest(String type, JsonObject payload, RemoteClient client,
			Consumer<JsonObject> responseCallback) {
		if (connectorHandlers.containsKey(type))
			connectorHandlers.get(type).handle(payload, client, responseCallback);
	}
}
