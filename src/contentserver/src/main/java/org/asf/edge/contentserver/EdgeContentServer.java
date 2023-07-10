package org.asf.edge.contentserver;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.connective.ConnectiveHttpServer;
import org.asf.edge.contentserver.http.*;
import org.asf.edge.contentserver.http.ContentServerRequestHandler.IPreProcessor;
import org.asf.edge.contentserver.http.postprocessors.ApplicationManifestPreProcessor;
import org.asf.edge.contentserver.http.postprocessors.ServerDownPreprocessor;
import org.asf.edge.modules.eventbus.EventBus;

import org.asf.edge.common.IBaseServer;
import org.asf.edge.contentserver.events.server.ContentServerSetupEvent;
import org.asf.edge.contentserver.events.server.ContentServerStartupEvent;
import org.asf.edge.contentserver.config.ContentServerConfig;

/**
 * 
 * EDGE Content and Assets Server
 * 
 * @author Sky Swimmer
 *
 */
public class EdgeContentServer implements IBaseServer {
	public static final String CONTENT_SERVER_VERSION = "1.0.0.A1";

	private Logger logger;
	private ContentServerConfig config;

	private ConnectiveHttpServer server;

	static void printSplash() {
		System.out.println("-------------------------------------------------------------");
		System.out.println("                                                             ");
		System.out.println("    EDGE - Fan-made server software for School of Dragons    ");
		System.out.println("               Content Server Version 1.0.0.A1               ");
		System.out.println("                                                             ");
		System.out.println("-------------------------------------------------------------");
		System.out.println("");
		System.out.println("This server implements the following endpoints:");
		System.out.println(" - media.schoolofdragons.com");
		System.out.println("");
	}

	public EdgeContentServer(ContentServerConfig config) {
		this.config = config;
		logger = LogManager.getLogger("CONTENTSERVER");
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
	 * Retrieves the logger of the content server
	 * 
	 * @return Logger instance
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Retrieves the content server configuration
	 * 
	 * @return ContentServerConfig instance
	 */
	public ContentServerConfig getConfiguration() {
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
				logger.info("Edge content HTTPS server created with listen address " + config.listenAddress
						+ " and port " + config.listenPort);
			} else {
				HashMap<String, String> props = new HashMap<String, String>();
				props.put("address", config.listenAddress);
				props.put("port", Integer.toString(config.listenPort));
				config.server = ConnectiveHttpServer.createNetworked("HTTP/1.1", props);
				logger.info("Edge content HTTP server created with listen address " + config.listenAddress
						+ " and port " + config.listenPort);
			}
		}

		// Assign server
		server = config.server;

		// Prepare data folder
		File dataPath = new File(config.contentDataPath);
		if (!dataPath.exists()) {
			logger.debug("Creating data folder...");
			if (!dataPath.mkdirs())
				throw new IOException("Failed to create directory: " + dataPath);
		}

		// Call event
		logger.debug("Dispatching event...");
		EventBus.getInstance().dispatchEvent(new ContentServerSetupEvent(config, this));

		// Register handlers
		logger.debug("Configuring server request handlers...");
		server.registerProcessor(new ContentServerRequestHandler(dataPath, config.contentRequestListenPath,
				new IPreProcessor[] { new ApplicationManifestPreProcessor(), new ServerDownPreprocessor(config) },
				this));
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
		logger.info("Starting the content delivery server...");
		server.start();

		// Call event
		EventBus.getInstance().dispatchEvent(new ContentServerStartupEvent(config, this));

		// Log
		logger.info("Content delivery server started successfully!");
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
		logger.info("Shutting down the content delivery server...");
		try {
			server.stop();
		} catch (IOException e) {
		}
		logger.info("Content delivery server stopped successfully!");
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
		logger.info("Forcefully shutting down the content delivery server!");
		try {
			server.stopForced();
		} catch (IOException e) {
		}
		logger.info("Content delivery server stopped successfully!");
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
