package org.asf.edge.common;

/**
 * 
 * Called to initialize common code of Edge
 * 
 * @author Sky Swimmer
 *
 */
public class CommonInit {
	private static boolean logInited;
	private static boolean debugMode;
	public static boolean restartPending;

	/**
	 * Calls all init methods
	 */
	public static void initAll() {
		initLogging();
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
	}

}
