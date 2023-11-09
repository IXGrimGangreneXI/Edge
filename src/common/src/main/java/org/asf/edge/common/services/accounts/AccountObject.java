package org.asf.edge.common.services.accounts;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.HashMap;

import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.nexus.tables.TableRow;

/**
 * 
 * Account object abstract
 * 
 * @author Sky Swimmer
 *
 */
public abstract class AccountObject {

	private HashMap<String, Object> sessionMemory = new HashMap<String, Object>();
	private HashMap<String, AccountKvDataContainer> loadedKvContainers = new HashMap<String, AccountKvDataContainer>();
	private HashMap<String, AccountDataTableContainer<?>> loadedTableContainers = new HashMap<String, AccountDataTableContainer<?>>();

	/**
	 * Retrieves session memory objects
	 * 
	 * @param <T>  Object type
	 * @param type Object class
	 * @return Object instance or null
	 */
	@SuppressWarnings("unchecked")
	public <T> T getSessionObject(Class<T> type) {
		return (T) sessionMemory.get(type.getTypeName());
	}

	/**
	 * Stores session memory objects
	 * 
	 * @param <T>    Object type
	 * @param type   Object class
	 * @param object Object instance
	 */
	public <T> void setSessionObject(Class<T> type, T object) {
		sessionMemory.put(type.getTypeName(), object);
	}

	/**
	 * Removes session memory objects
	 * 
	 * @param <T>  Object type
	 * @param type Object class
	 */
	public <T> void removeSessionObject(Class<T> type) {
		sessionMemory.remove(type.getTypeName());
	}

	/**
	 * Retrieves the account username
	 * 
	 * @return Account username string
	 */
	public abstract String getUsername();

	/**
	 * Retrieves the account email
	 * 
	 * @return Account email string
	 */
	public abstract String getAccountEmail();

	/**
	 * Retrieves the account ID
	 * 
	 * @return Account ID string
	 */
	public abstract String getAccountID();

	/**
	 * Retrieves the last time this account was logged into
	 *
	 * @return Login Unix timestamp (seconds)
	 */
	public abstract long getLastLoginTime();

	/**
	 * Retrieves the time on which the account was registered
	 *
	 * @return Login Unix timestamp (seconds)
	 */
	public abstract long getRegistrationTimestamp();

	/**
	 * Changes the username
	 * 
	 * @param name New account username
	 * @return True if successful, false otherwise
	 */
	public abstract boolean updateUsername(String name);

	/**
	 * Changes the email
	 * 
	 * @param name New account email
	 * @return True if successful, false otherwise
	 */
	public abstract boolean updateEmail(String name);

	/**
	 * Changes the password
	 * 
	 * @param newPassword New acccount password
	 * @return True if successful, false otherwise
	 */
	public abstract boolean updatePassword(char[] newPassword);

	/**
	 * Attempts account migration from this guest account, transferring it into a
	 * guest account
	 * 
	 * @param newName  New account name
	 * @param email    New account email
	 * @param password New account password
	 * @return True if successful, false otherwise
	 */
	public abstract boolean migrateToNormalAccountFromGuest(String newName, String email, char[] password);

	/**
	 * Checks if the account is a guest account
	 * 
	 * @return True if the account is a guest account, false otherwise
	 */
	public abstract boolean isGuestAccount();

	/**
	 * Checks if multiplayer is enabled
	 * 
	 * @return True if multiplayer is enabled, false otherwise
	 */
	public abstract boolean isMultiplayerEnabled();

	/**
	 * Checks if chat is enabled
	 * 
	 * @return True if chat is enabled, false otherwise
	 */
	public abstract boolean isChatEnabled();

	/**
	 * Checks if stricter chat filter is enabled
	 * 
	 * @return True if chat is enabled, false otherwise
	 */
	public abstract boolean isStrictChatFilterEnabled();

	/**
	 * Changes if multiplayer is enabled
	 * 
	 * @param state New multiplayer state, true to enable, false to disable
	 */
	public abstract void setMultiplayerEnabled(boolean state);

	/**
	 * Changes if chat is enabled
	 * 
	 * @param state New chat state, true to enable, false to disable
	 */
	public abstract void setChatEnabled(boolean state);

	/**
	 * Checks if stricter chat filter is enabled
	 * 
	 * @param state New chat filter state, true to enable strict mode, false to
	 *              disable strict mode
	 */
	public abstract void setStrictChatFilterEnabled(boolean state);

	/**
	 * Updates the last login timestamp to the current time
	 */
	public abstract void updateLastLoginTime();

	/**
	 * Retrieves all save IDs for this account
	 * 
	 * @return Array of save ID strings
	 */
	public abstract String[] getSaveIDs();

	/**
	 * Creates save data
	 * 
	 * @param username Save username
	 * @return AccountSaveContainer instance or null
	 */
	public abstract AccountSaveContainer createSave(String username);

	/**
	 * Retrieves save data
	 * 
	 * @param id Save data ID
	 * @return AccountSaveContainer instance or null
	 */
	public abstract AccountSaveContainer getSave(String id);

	/**
	 * Retrieves the account-wide player inventory
	 * 
	 * @return PlayerInventory instance
	 */
	public PlayerInventory getInventory() {
		this.sessionMemory = sessionMemory;
		return ItemManager.getInstance().getCommonInventory(null); // FIXME
	}

	/**
	 * Deletes the account
	 * 
	 * @throws IOException If deletion errors
	 */
	public abstract void deleteAccount() throws IOException;

	/**
	 * Checks if the account is online
	 * 
	 * @return True if online, false otherwise
	 */
	public abstract boolean isOnline();

	/**
	 * Optional override to receive account object pings
	 * 
	 * @param addIfNeeded True to add to cache if not present, false otherwise, to
	 *                    prevent abuse, unless the token used for requests is valid
	 *                    for this account, this should be false
	 */
	public void ping(boolean addIfNeeded) {
	}

	/**
	 * Retrieves account key/value data containers
	 * 
	 * @param rootNodeName Root container node name
	 * @return AccountKvDataContainer instance
	 */
	public AccountKvDataContainer getAccountKeyValueContainer(String rootNodeName) {
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
	 * Called to retrieve account data containers
	 * 
	 * @param rootNodeName Root node name
	 * @return AccountKvDataContainer instance
	 */
	protected abstract AccountKvDataContainer getKeyValueContainerInternal(String rootNodeName);

	/**
	 * Called to set up account data containers, called the first time a container
	 * is retrieved
	 * 
	 * @param rootNodeName Root node name
	 */
	protected abstract void setupKeyValueContainer(String rootNodeName);

	/**
	 * Retrieves account data table containers
	 * 
	 * @param <T>       Row object type
	 * @param tableName Root container node name
	 * @param rowType   Row object type
	 * @return AccountDataTableContainer instance
	 */
	@SuppressWarnings("unchecked")
	public <T extends TableRow> AccountDataTableContainer<T> getAccountDataTable(String tableName, Class<T> rowType) {
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
	 * Called to retrieve account data containers
	 * 
	 * @param <T>       Row object type
	 * @param tableName Root node name
	 * @param cls       Row object type
	 * @return AccountDataTableContainer instance
	 */
	protected abstract <T extends TableRow> AccountDataTableContainer<T> getDataTableContainerInternal(String tableName,
			Class<T> cls);

	/**
	 * Called to set up account data containers, called the first time a container
	 * is retrieved
	 * 
	 * @param tableName Table name
	 * @param cont      Container instance
	 */
	protected abstract void setupDataTableContainer(String tableName, AccountDataTableContainer<?> cont);

}
