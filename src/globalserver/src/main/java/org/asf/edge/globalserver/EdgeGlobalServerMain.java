package org.asf.edge.globalserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.CommonInit;
import org.asf.edge.contentserver.EdgeContentServer;
import org.asf.edge.modules.ModuleManager;

public class EdgeGlobalServerMain {
	public static final String GLOBAL_SERVER_VERSION = "1.0.0.A1";

	public static void main(String[] args) {
		// Print splash
		System.out.println("-------------------------------------------------------------");
		System.out.println("                                                             ");
		System.out.println("    EDGE - Fan-made server software for School of Dragons    ");
		System.out.println("                Full Server Version: 1.0.0.A1                ");
		System.out.println("                                                             ");
		System.out.println("-------------------------------------------------------------");
		System.out.println("");

		// Common init
		CommonInit.initAll();

		// Logger
		Logger logger = LogManager.getLogger("FULLSERVER");
		logger.info("EDGE Global (full) server is starting!");
		logger.info("Content server version: " + EdgeContentServer.CONTENT_SERVER_VERSION);
		logger.info("Global server version: " + GLOBAL_SERVER_VERSION);
		logger.info("Preparing to start...");

		// Load modules
		ModuleManager.init();

		// TODO
	}

}
