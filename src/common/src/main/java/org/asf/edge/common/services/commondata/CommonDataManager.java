package org.asf.edge.common.services.commondata;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.commondata.impl.DefaultDatabaseCommonDataManager;
import org.asf.edge.common.services.commondata.impl.PostgresDatabaseCommonDataManager;
import org.asf.edge.common.services.config.ConfigProviderService;
import org.asf.edge.common.services.tabledata.TableRow;
import org.asf.nexus.common.services.AbstractService;
import org.asf.nexus.common.services.ServiceManager;

import com.google.gson.JsonObject;

/**
 * 
 * Common Data Manager API
 * 
 * @author Sky Swimmer
 *
 */
public abstract class CommonDataManager extends AbstractService {

	private HashMap<String, CommonKvDataContainer> loadedKvContainers = new HashMap<String, CommonKvDataContainer>();
	private HashMap<String, CommonDataTableContainer<?>> loadedTableContainers = new HashMap<String, CommonDataTableContainer<?>>();
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
	public static void initCommonDataManagerServices(int priorityDatabase, int priorityPostgres) {
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
		ServiceManager.registerServiceImplementation(CommonDataManager.class, new DefaultDatabaseCommonDataManager(),
				databaseManagerConfig.get("priority").getAsInt());
		ServiceManager.registerServiceImplementation(CommonDataManager.class, new PostgresDatabaseCommonDataManager(),
				postgresManagerConfig.get("priority").getAsInt());
	}

	/**
	 * Called to retrieve containers
	 * 
	 * @param rootNodeName Root node name
	 * @return CommonKvDataContainer instance
	 */
	protected abstract CommonKvDataContainer getKeyValueContainerInternal(String rootNodeName);

	/**
	 * Called to set up containers, called the first time a container is retrieved
	 * 
	 * @param rootNodeName Root node name
	 */
	protected abstract void setupKeyValueContainer(String rootNodeName);

	/**
	 * Retrieves key/value data containers
	 * 
	 * @param rootNodeName Root node name
	 * @return CommonKvDataContainer instance
	 */
	public CommonKvDataContainer getKeyValueContainer(String rootNodeName) {
		if (!rootNodeName.matches("^[A-Za-z0-9_]+$"))
			throw new IllegalArgumentException(
					"Root node name can only contain alphanumeric characters and underscores");
		rootNodeName = rootNodeName.toUpperCase();
		while (true) {
			try {
				if (loadedKvContainers.containsKey(rootNodeName))
					return loadedKvContainers.get(rootNodeName);
				break;
			} catch (ConcurrentModificationException e) {
			}
		}

		// Lock
		synchronized (loadedKvContainers) {
			if (loadedKvContainers.containsKey(rootNodeName))
				return loadedKvContainers.get(rootNodeName); // Seems another thread had added it before we got the lock

			// Add container
			CommonKvDataContainer cont = getKeyValueContainerInternal(rootNodeName);
			setupKeyValueContainer(rootNodeName);
			loadedKvContainers.put(rootNodeName, cont);
			return cont;
		}
	}

	/**
	 * Called to retrieve common data containers
	 * 
	 * @param <T>       Row object type
	 * @param tableName Root node name
	 * @param cls       Row object type
	 * @return CommonDataTableContainer instance
	 */
	protected abstract <T extends TableRow> CommonDataTableContainer<T> getDataTableContainerInternal(String tableName,
			Class<T> cls);

	/**
	 * Called to set up common data containers, called the first time a container is
	 * retrieved
	 * 
	 * @param tableName Table name
	 * @param cont      Container instance
	 */
	protected abstract void setupDataTableContainer(String tableName, CommonDataTableContainer<?> cont);

	/**
	 * Called to retrieve containers
	 * 
	 * @param <T>       Row object type
	 * @param tableName Root container node name
	 * @param rowType   Row object type
	 * @return CommonDataTableContainer instance
	 */
	@SuppressWarnings("unchecked")
	public <T extends TableRow> CommonDataTableContainer<T> getDataTable(String tableName, Class<T> rowType) {
		if (!tableName.matches("^[A-Za-z0-9_]+$"))
			throw new IllegalArgumentException("Table name can only contain alphanumeric characters and underscores");
		tableName = tableName.toUpperCase();
		while (true) {
			try {
				if (loadedTableContainers.containsKey(tableName))
					return (CommonDataTableContainer<T>) loadedTableContainers.get(tableName);
				break;
			} catch (ConcurrentModificationException e) {
			}
		}

		// Lock
		synchronized (loadedTableContainers) {
			if (loadedTableContainers.containsKey(tableName)) {
				// Seems another thread had added it before we got the lock
				return (CommonDataTableContainer<T>) loadedTableContainers.get(tableName);
			}

			// Add container
			CommonDataTableContainer<T> cont = getDataTableContainerInternal(tableName, rowType);
			setupDataTableContainer(tableName, cont);
			loadedTableContainers.put(tableName, cont);
			return cont;
		}
	}

	/**
	 * Called to initialize the common data manager
	 */
	public abstract void loadManager();

}
