package org.asf.edge.common;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.UUID;

import org.asf.edge.common.util.LogWindow;

import com.google.gson.JsonParser;

/**
 * 
 * Edge common server environment
 * 
 * @author Sky Swimmer
 *
 */
public class EdgeServerEnvironment {
	private static final String EDGE_VERSION = "1.0.0.A3";

	private static boolean logInited;
	private static boolean serverIDInited;
	private static boolean debugMode;

	private static ArrayList<String> serverTypes = new ArrayList<String>();

	private static String serverID;
	public static boolean restartPending;

	/**
	 * Adds server types
	 * 
	 * @param type Server type name to add, eg. <code>gameplayapi</code>
	 */
	public static void addServerType(String type) {
		if (!hasServerType(type))
			serverTypes.add(type.toLowerCase());
	}

	/**
	 * Checks if a server type is present in the environment
	 * 
	 * @param type Server type name, eg. <code>gameplayapi</code>
	 * @return True if present, false otherwise
	 */
	public static boolean hasServerType(String type) {
		return serverTypes.contains(type.toLowerCase());
	}

	/**
	 * Retrieves all server types present in the environment
	 * 
	 * @return Array of server type strings
	 */
	public static String[] getServerTypes() {
		return serverTypes.toArray(t -> new String[t]);
	}

	/**
	 * Retrieves the Edge version
	 * 
	 * @return Edge version string
	 */
	public static String getEdgeVersion() {
		return EDGE_VERSION;
	}

	/**
	 * Calls all init methods
	 */
	public static void initAll() {
		initLogging();
		initServerID();
	}

	/**
	 * Initializes the server ID system
	 */
	public static void initServerID() {
		if (serverIDInited)
			return;
		serverIDInited = true;

		// Setup ID
		File idFile = new File("scalability.json");
		try {
			if (!idFile.exists()) {
				serverID = UUID.randomUUID().toString();
				Files.writeString(idFile.toPath(), "{\n"
						//
						+ "    \"__COMMENT1__\": \"This file controls scalability settings, specifically server identification.\",\n"
						//
						+ "    \"__COMMENT2__\": \"The UUID below is used to identify this specific server set, it MUST be shared with a common/gameplay/smartfox server to make sure that multi-server functionality does not degrade.\",\n"
						//
						+ "    \"__COMMENT3__\": \"If the ID below does not have a common/gameplay/smartfox counterpart server active, memory leaks can occur as the load balancer will fail to determine which server the user is active on.\",\n"
						//
						+ "    \"__COMMENT4__\": \"It is imperative for multi-server configurations to have a common, gameplay and smartfox server with the same server set ID.\",\n"
						//
						+ "    \"serverSetID\": \"" + serverID + "\"\n"
						//
						+ "}\n");
			}
			serverID = JsonParser.parseString(Files.readString(idFile.toPath())).getAsJsonObject().get("serverSetID")
					.getAsString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Checks if the servers are in debug (IDE) mode
	 * 
	 * @return True if in debug mode, false otherwise
	 */
	public static boolean isInDebugMode() {
		return debugMode;
	}

	/**
	 * Initializes logging
	 */
	public static void initLogging() {
		if (logInited)
			return;
		logInited = true;

		// Setup logging
		if (System.getProperty("debugMode") != null) {
			System.setProperty("log4j2.configurationFile", EdgeServerEnvironment.class.getResource("/log4j2-ide.xml").toString());
			debugMode = true;
		} else {
			System.setProperty("log4j2.configurationFile", EdgeServerEnvironment.class.getResource("/log4j2.xml").toString());
		}

		// Open graphical
		if (System.getProperty("openGuiLog") != null && !System.getProperty("openGuiLog").equalsIgnoreCase("false")) {
			if (!GraphicsEnvironment.isHeadless()) {
				LogWindow.WindowAppender.showWindow();
				Runtime.getRuntime().addShutdownHook(new Thread(() -> LogWindow.WindowAppender.closeWindow()));
			}
		}
	}

	/**
	 * Retrieves the server ID
	 * 
	 * @return Server ID string
	 */
	public static String getServerID() {
		return serverID;
	}

}
