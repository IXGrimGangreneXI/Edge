package org.asf.edge.mmoserver.config;

import java.util.LinkedHashMap;

import org.asf.edge.mmoserver.networking.SmartfoxServer;

/**
 * 
 * MMO server configuration
 * 
 * @author Sky Swimmer
 *
 */
public class MMOServerConfig {

	/**
	 * Listen address
	 */
	public String listenAddress = "0.0.0.0";

	/**
	 * Listen port
	 */
	public int listenPort = 5323;

	/**
	 * Discovery address (should be the public IP address)
	 */
	public String discoveryAddress = "localhost";

	/**
	 * Discovery port (should be the public port)
	 */
	public int discoveryPort = 5323;

	/**
	 * Determines if this server should be a backup server in server discovery
	 */
	public boolean isBackupServer = false;

	/**
	 * Defines the discovery root zone
	 */
	public String discoveryRootZone = "JumpStart";

	/**
	 * Common API uplink URL (url to the internal server of the common API, used to
	 * publish the server)
	 */
	public String commonApiUplinkURL = "http://127.0.0.1:5324/";

	/**
	 * Pre-assigned server instance, if assigned, EDGE will use this instead
	 */
	public SmartfoxServer server;

	/**
	 * Defines the default user limit for each MMO room
	 */
	public short roomUserLimit = 30;

	/**
	 * Defines per-map user limits for MMO rooms
	 */
	public LinkedHashMap<String, Short> roomUserLimits = new LinkedHashMap<String, Short>();

}
