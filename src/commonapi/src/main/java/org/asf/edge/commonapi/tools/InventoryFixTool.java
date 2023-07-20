package org.asf.edge.commonapi.tools;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.CommonInit;
import org.asf.edge.common.services.ServiceImplementationPriorityLevels;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.services.items.impl.ItemManagerImpl;

import com.google.gson.JsonPrimitive;

public class InventoryFixTool {

	private static long accountCount;
	private static long index;

	public static void main(String[] args) {
		// Setup
		CommonInit.initAll();

		// Logger
		Logger logger = LogManager.getLogger("CONVERTER");
		logger.info("Preparing to start...");

		// Prepare services
		logger.info("Loading managers...");
		logger.debug("Loading account manager implementations...");
		AccountManager.initAccountManagerServices(ServiceImplementationPriorityLevels.DEFAULT,
				ServiceImplementationPriorityLevels.NORMAL, -5);
		logger.debug("Selecting account manager implementation...");
		ServiceManager.selectServiceImplementation(AccountManager.class);
		logger.debug("Loading account manager...");
		AccountManager.getInstance().loadManager();
		logger.debug("Loading common data manager implementations...");
		CommonDataManager.initCommonDataManagerServices(ServiceImplementationPriorityLevels.DEFAULT,
				ServiceImplementationPriorityLevels.NORMAL, -5);
		logger.debug("Selecting common data manager implementation...");
		ServiceManager.selectServiceImplementation(CommonDataManager.class);
		logger.debug("Loading common data manager...");
		CommonDataManager.getInstance().loadManager();
		logger.debug("Setting up item manager...");
		ServiceManager.registerServiceImplementation(ItemManager.class, new ItemManagerImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(ItemManager.class);

		// Fix accounts
		logger.info("Indexing...");
		AccountManager.getInstance().runForAllAccounts(acc -> {
			accountCount++;
			return true;
		});
		logger.info("Repairing data of " + accountCount + " accounts...");
		AccountManager.getInstance().runForAllAccounts(acc -> {
			index++;
			logger.info("[ " + index + " / " + accountCount + "] Repairing data of " + acc.getUsername());
			repairInventories(acc.getAccountData(), logger);
			logger.info("[ " + index + " / " + accountCount + "] Repairing data of all saves of " + acc.getUsername());
			for (String id : acc.getSaveIDs()) {
				AccountSaveContainer save = acc.getSave(id);
				logger.info("[ " + index + " / " + accountCount + "] Repairing data of save " + save.getUsername());
				repairInventories(save.getSaveData(), logger);
			}
			return true;
		});
	}

	private static void repairInventories(AccountDataContainer data, Logger logger) {
		try {
			// Retrieve inventory container
			data = data.getChildContainer("commoninventories");

			// Go through items
			for (String key : data.getChildContainers()) {
				if (key.startsWith("c-")) {
					int containerID = Integer.parseInt(key.substring(2));

					// Go through items
					AccountDataContainer container = data.getChildContainer("c-" + containerID);
					for (String key2 : container.getChildContainers()) {
						if (key2.startsWith("d-")) {
							int defID = Integer.parseInt(key2.substring(2));

							// Go through unique IDs
							AccountDataContainer defCont = container.getChildContainer("d-" + defID);
							for (String key3 : defCont.getEntryKeys()) {
								if (key3.startsWith("u-")) {
									int uniqueID = Integer.parseInt(key3.substring(3));

									// Set data
									container.setEntry("u-" + uniqueID, new JsonPrimitive(defID));
								}
							}
						}
					}
				}
			}
		} catch (IOException e) {
			logger.fatal("Failed to repair data of account " + data.getAccount().getUsername(), e);
			System.exit(1);
		}
	}

}
