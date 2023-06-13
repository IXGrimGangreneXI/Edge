package org.asf.edge.commonapi.config;

import org.asf.connective.ConnectiveHttpServer;

/**
 * 
 * Common API server configuration
 * 
 * @author Sky Swimmer
 *
 */
public class CommonApiServerConfig {

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
	public int listenPort = 5321;

	/**
	 * True to use HTTPS, false to use HTTP
	 */
	public boolean httpsInternal = false;

	/**
	 * Keystore file path for TLS encryption (for https)
	 */
	public String tlsKeystoreInternal = null;

	/**
	 * Keystore password for TLS encryption (for https)
	 */
	public String tlsKeystorePasswordInternal = null;

	/**
	 * Pre-assigned internal server instance, if assigned, EDGE will use this
	 * instead
	 */
	public ConnectiveHttpServer internalServer;

	/**
	 * Internal server listen address
	 */
	public String internalListenAddress = "0.0.0.0";

	/**
	 * Internal server listen port
	 */
	public int internalListenPort = 5324;

	/**
	 * Account database folder
	 */
	public String accountDatabaseDir = "./account-data";

	/**
	 * Path to register the API request handler on
	 */
	public String apiRequestListenPath = "/";

}
