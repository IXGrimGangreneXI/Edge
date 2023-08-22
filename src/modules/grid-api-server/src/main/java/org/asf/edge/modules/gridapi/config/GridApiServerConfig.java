package org.asf.edge.modules.gridapi.config;

import org.asf.connective.ConnectiveHttpServer;

/**
 * 
 * Grid API server configuration
 * 
 * @author Sky Swimmer
 *
 */
public class GridApiServerConfig {

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
	public int listenPort = 16718;

}
