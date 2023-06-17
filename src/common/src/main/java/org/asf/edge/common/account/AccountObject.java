package org.asf.edge.common.account;

import java.io.IOException;

/**
 * 
 * Account object abstract
 * 
 * @author Sky Swimmer
 *
 */
public abstract class AccountObject {

	/**
	 * Retrieves the account username
	 * 
	 * @return Account username string
	 */
	public abstract String getUsername();

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
	 * @param password New account password
	 * @return True if successful, false otherwise
	 */
	public abstract boolean migrateToNormalAccountFromGuest(String newName, char[] password);

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
	 * Deletes the account
	 * 
	 * @throws IOException If deletion errors
	 */
	public abstract void deleteAccount() throws IOException;

	// TODO
	// Among other things i still need to do save management

}
