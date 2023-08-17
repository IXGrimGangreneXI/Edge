package org.asf.edge.common;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import org.asf.edge.common.util.LogWindow;

import com.google.gson.JsonParser;

/**
 * 
 * Called to initialize common code of Edge
 * 
 * @author Sky Swimmer
 *
 */
public class CommonInit {
	private static boolean logInited;
	private static boolean serverIDInited;
	private static boolean debugMode;

	private static String serverID;
	public static boolean restartPending;

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
	public boolean isInDebugMode() {
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
			System.setProperty("log4j2.configurationFile", CommonInit.class.getResource("/log4j2-ide.xml").toString());
			debugMode = true;
		} else {
			System.setProperty("log4j2.configurationFile", CommonInit.class.getResource("/log4j2.xml").toString());
		}

		// Open graphical
		if (System.getProperty("openGuiLog") != null && !System.getProperty("openGuiLog").equalsIgnoreCase("false")) {
			if (!GraphicsEnvironment.isHeadless()) {
				LogWindow.WindowAppender.showWindow();
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
