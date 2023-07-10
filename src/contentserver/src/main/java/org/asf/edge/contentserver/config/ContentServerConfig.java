package org.asf.edge.contentserver.config;

import org.asf.connective.ConnectiveHttpServer;

import com.google.gson.JsonObject;

/**
 * 
 * Content server configuration
 * 
 * @author Sky Swimmer
 *
 */
public class ContentServerConfig {

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
	 * Defines the server endpoint that is used should assets not be found
	 */
	public String fallbackAssetServerEndpoint = null;

	/**
	 * Defines the asset manifest modifications for proxying
	 */
	public JsonObject fallbackAssetServerManifestModifications = new JsonObject();

	/**
	 * Defines the server test endpoint contacted to check if the servers are live
	 */
	public String serverTestEndpoint = null;

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
	public int listenPort = 5319;

	/**
	 * Path to register the asset request handler on
	 */
	public String contentRequestListenPath = "/";

	/**
	 * Content data path containing all game assets
	 */
	public String contentDataPath = "content-server/asset-data";

	/**
	 * Defines if indexing of assets should be allowed
	 */
	public boolean allowIndexingAssets = true;

}
