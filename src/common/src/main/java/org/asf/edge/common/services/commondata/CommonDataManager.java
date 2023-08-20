package org.asf.edge.common.services.commondata;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.AbstractService;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.common.services.commondata.impl.DefaultDatabaseCommonDataManager;
import org.asf.edge.common.services.commondata.impl.PostgresDatabaseCommonDataManager;
import org.asf.edge.common.services.commondata.impl.http.RemoteHttpCommonDataManager;
import org.asf.edge.common.services.config.ConfigProviderService;

import com.google.gson.JsonObject;

/**
 * 
 * Common Data Manager API
 * 
 * @author Sky Swimmer
 *
 */
public abstract class CommonDataManager extends AbstractService {

	private HashMap<String, CommonDataContainer> loadedContainers = new HashMap<String, CommonDataContainer>();
	private static boolean initedServices;

	/**
	 * Retrieves the common data manager service
	 * 
	 * @return CommonDataManager instance
	 */
	public static CommonDataManager getInstance() {
		return ServiceManager.getService(CommonDataManager.class);
	}

	/**
	 * Internal
	 */
	public static void initCommonDataManagerServices(int priorityRemote, int priorityDatabase, int priorityPostgres) {
		if (initedServices)
			return;
		initedServices = true;

		// Write/load config
		Logger logger = LogManager.getLogger("CommonDataManager");
		JsonObject commonDataManagerConfig;
		try {
			commonDataManagerConfig = ConfigProviderService.getInstance().loadConfig("server", "commondata");
		} catch (IOException e) {
			logger.error("Failed to load common data manager configuration!", e);
			return;
		}
		if (commonDataManagerConfig == null) {
			commonDataManagerConfig = new JsonObject();
		}
		boolean changed = false;
		JsonObject remoteManagerConfig = new JsonObject();
		if (!commonDataManagerConfig.has("remoteHttpManager")) {
			remoteManagerConfig.addProperty("priority", priorityRemote);
			remoteManagerConfig.addProperty("url", "http://127.0.0.1:5324/commondatamanager/");
			commonDataManagerConfig.add("remoteHttpManager", remoteManagerConfig);
			changed = true;
		} else
			remoteManagerConfig = commonDataManagerConfig.get("remoteHttpManager").getAsJsonObject();
		JsonObject databaseManagerConfig = new JsonObject();
		if (!commonDataManagerConfig.has("databaseManager")) {
			databaseManagerConfig.addProperty("priority", priorityDatabase);
			databaseManagerConfig.addProperty("url", "jdbc:sqlite:common-data.db");
			JsonObject props = new JsonObject();
			databaseManagerConfig.add("properties", props);
			commonDataManagerConfig.add("databaseManager", databaseManagerConfig);
			changed = true;
		} else
			databaseManagerConfig = commonDataManagerConfig.get("databaseManager").getAsJsonObject();
		JsonObject postgresManagerConfig = new JsonObject();
		if (!commonDataManagerConfig.has("postgreSQL")) {
			postgresManagerConfig.addProperty("priority", priorityPostgres);
			postgresManagerConfig.addProperty("url", "jdbc:postgresql://localhost/edge");
			JsonObject props = new JsonObject();
			postgresManagerConfig.add("properties", props);
			commonDataManagerConfig.add("postgreSQL", postgresManagerConfig);
			changed = true;
		} else
			postgresManagerConfig = commonDataManagerConfig.get("postgreSQL").getAsJsonObject();
		if (changed) {
			// Write config
			try {
				ConfigProviderService.getInstance().saveConfig("server", "commondata", commonDataManagerConfig);
			} catch (IOException e) {
				logger.error("Failed to write the common data manager configuration!", e);
				return;
			}
		}

		// Register default common data managers
		ServiceManager.registerServiceImplementation(CommonDataManager.class, new RemoteHttpCommonDataManager(),
				remoteManagerConfig.get("priority").getAsInt());
		ServiceManager.registerServiceImplementation(CommonDataManager.class, new DefaultDatabaseCommonDataManager(),
				databaseManagerConfig.get("priority").getAsInt());
		ServiceManager.registerServiceImplementation(CommonDataManager.class, new PostgresDatabaseCommonDataManager(),
				postgresManagerConfig.get("priority").getAsInt());
	}

	/**
	 * Called to retrieve containers
	 * 
	 * @param rootNodeName Root node name
	 * @return CommonDataContainer instance
	 */
	protected abstract CommonDataContainer getContainerInternal(String rootNodeName);

	/**
	 * Called to set up containers, called the first time a container is retrieved
	 * 
	 * @param rootNodeName Root node name
	 */
	protected abstract void setupContainer(String rootNodeName);

	/**
	 * Retrieves containers
	 * 
	 * @param rootNodeName Root node name
	 * @return CommonDataContainer instance
	 */
	public CommonDataContainer getContainer(String rootNodeName) {
		if (!rootNodeName.matches("^[A-Za-z0-9]+$"))
			throw new IllegalArgumentException("Root node name can only contain alphanumeric characters");
		rootNodeName = rootNodeName.toUpperCase();
		while (true) {
			try {
				if (loadedContainers.containsKey(rootNodeName))
					return loadedContainers.get(rootNodeName);
				break;
			} catch (ConcurrentModificationException e) {
			}
		}

		// Lock
		synchronized (loadedContainers) {
			if (loadedContainers.containsKey(rootNodeName))
				return loadedContainers.get(rootNodeName); // Seems another thread had added it before we got the lock

			// Add container
			CommonDataContainer cont = getContainerInternal(rootNodeName);
			setupContainer(rootNodeName);
			loadedContainers.put(rootNodeName, cont);
			return cont;
		}
	}

	/**
	 * Called to initialize the common data manager
	 */
	public abstract void loadManager();

}
