package org.asf.edge.gameplayapi.config;

import org.asf.connective.ConnectiveHttpServer;

/**
 * 
 * Gameplay API server configuration
 * 
 * @author Sky Swimmer
 *
 */
public class GameplayApiServerConfig {

	/**
	 * True to use HTTPS, false to use HTTP
	 */
	public boolean https = false;

	/**
	 * Keystore file path for TLS encryption (for https)
	 */
	public String tlsKeystore = null;

	/**
	 * Keystore password for TLS encryption (for https)
	 */
	public String tlsKeystorePassword = null;

	/**
	 * Pre-assigned server instance, if assigned, EDGE will use this instead
	 */
	public ConnectiveHttpServer server;

	/**
	 * Listen address
	 */
	public String listenAddress = "0.0.0.0";

	/**
	 * Listen port
	 */
	public int listenPort = 5320;

	/**
	 * Path to register the API request handler on
	 */
	public String apiRequestListenPath = "/";

}
