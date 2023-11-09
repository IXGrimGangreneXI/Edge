package org.asf.edge.common.services.accounts;

import java.util.ConcurrentModificationException;
import java.util.HashMap;

import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.nexus.tables.TableRow;

/**
 * 
 * Account Save Container
 * 
 * @author Sky Swimmer
 *
 */
public abstract class AccountSaveContainer {

	private HashMap<String, AccountKvDataContainer> loadedKvContainers = new HashMap<String, AccountKvDataContainer>();
	private HashMap<String, AccountDataTableContainer<?>> loadedTableContainers = new HashMap<String, AccountDataTableContainer<?>>();

	/**
	 * Retrieves the save username
	 * 
	 * @return Container username string
	 */
	public abstract String getUsername();

	/**
	 * Changes the save username
	 * 
	 * @param name New save username
	 * @return True if successful, false otherwise
	 */
	public abstract boolean updateUsername(String name);

	/**
	 * Retrieves the save ID
	 * 
	 * @return Container ID string
	 */
	public abstract String getSaveID();

	/**
	 * Retrieves the save creation timestamp
	 * 
	 * @return Save creation timestamp
	 */
	public abstract long getCreationTime();

	/**
	 * Retrieves the save inventory
	 * 
	 * @return PlayerInventory instance
	 */
	public PlayerInventory getInventory() {
		loadedTableContainers = loadedTableContainers;
		return ItemManager.getInstance().getCommonInventory(null); // FIXME
	}

	/**
	 * Deletes this save
	 */
	public abstract void deleteSave();

	/**
	 * Retrieves the account of the save
	 * 
	 * @return AccountObject instance
	 */
	public abstract AccountObject getAccount();

	/**
	 * Retrieves save key/value data containers
	 * 
	 * @param rootNodeName Root container node name
	 * @return AccountKvDataContainer instance
	 */
	public AccountKvDataContainer getSaveKeyValueContainer(String rootNodeName) {
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
			AccountKvDataContainer cont = getKeyValueContainerInternal(rootNodeName);
			setupKeyValueContainer(rootNodeName);
			loadedKvContainers.put(rootNodeName, cont);
			return cont;
		}
	}

	/**
	 * Called to retrieve save data containers
	 * 
	 * @param rootNodeName Root node name
	 * @return AccountKvDataContainer instance
	 */
	protected abstract AccountKvDataContainer getKeyValueContainerInternal(String rootNodeName);

	/**
	 * Called to set up save data containers, called the first time a container is
	 * retrieved
	 * 
	 * @param rootNodeName Root node name
	 */
	protected abstract void setupKeyValueContainer(String rootNodeName);

	/**
	 * Retrieves save data table containers
	 * 
	 * @param <T>       Row object type
	 * @param tableName Root container node name
	 * @param rowType   Row object type
	 * @return AccountDataTableContainer instance
	 */
	@SuppressWarnings("unchecked")
	public <T extends TableRow> AccountDataTableContainer<T> getSaveDataTable(String tableName, Class<T> rowType) {
		if (!tableName.matches("^[A-Za-z0-9_]+$"))
			throw new IllegalArgumentException("Table name can only contain alphanumeric characters and underscores");
		tableName = tableName.toUpperCase();
		while (true) {
			try {
				if (loadedTableContainers.containsKey(tableName))
					return (AccountDataTableContainer<T>) loadedTableContainers.get(tableName);
				break;
			} catch (ConcurrentModificationException e) {
			}
		}

		// Lock
		synchronized (loadedTableContainers) {
			if (loadedTableContainers.containsKey(tableName)) {
				// Seems another thread had added it before we got the lock
				return (AccountDataTableContainer<T>) loadedTableContainers.get(tableName);
			}

			// Add container
			AccountDataTableContainer<T> cont = getDataTableContainerInternal(tableName, rowType);
			setupDataTableContainer(tableName, cont);
			loadedTableContainers.put(tableName, cont);
			return cont;
		}
	}

	/**
	 * Called to retrieve save data containers
	 * 
	 * @param <T>       Row object type
	 * @param tableName Root node name
	 * @param cls       Row object type
	 * @return AccountDataTableContainer instance
	 */
	protected abstract <T extends TableRow> AccountDataTableContainer<T> getDataTableContainerInternal(String tableName,
			Class<T> cls);

	/**
	 * Called to set up save data containers, called the first time a container is
	 * retrieved
	 * 
	 * @param tableName Table name
	 * @param cont      Container instance
	 */
	protected abstract void setupDataTableContainer(String tableName, AccountDataTableContainer<?> cont);

}
