package org.asf.edge.common.services.accounts;

import java.io.IOException;
import java.util.HashMap;

import org.asf.edge.common.entities.items.PlayerInventory;
import org.asf.edge.common.services.items.ItemManager;

/**
 * 
 * Account object abstract
 * 
 * @author Sky Swimmer
 *
 */
public abstract class AccountObject {

	private HashMap<String, Object> sessionMemory = new HashMap<String, Object>();

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
	 * Retrieves the account data container
	 * 
	 * @return AccountDataContainer instance
	 */
	public abstract AccountDataContainer getAccountData();

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
		return ItemManager.getInstance().getCommonInventory(getAccountData());
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

}
