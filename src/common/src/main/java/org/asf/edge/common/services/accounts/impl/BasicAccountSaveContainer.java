package org.asf.edge.common.services.accounts.impl;

import java.io.IOException;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.events.accounts.saves.AccountSaveDeletedEvent;
import org.asf.edge.common.events.accounts.saves.AccountSaveUsernameUpdateEvent;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.minigamedata.MinigameDataManager;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.modules.eventbus.EventBus;

/**
 * 
 * Basic account save container abstract - pre-defines crucial methods for Edge
 * servers
 * 
 * @author Sky Swimmer
 *
 */
public abstract class BasicAccountSaveContainer extends AccountSaveContainer {

	private long time;
	private String saveID;
	private String username;
	private String accID;

	private Logger logger = LogManager.getLogger("AccountManager");
	private BasicAccountManager manager;
	private BasicAccountObject acc;

	private AccountDataContainer data;

	public BasicAccountSaveContainer(String saveID, long time, String username, String accID,
			BasicAccountManager manager, BasicAccountObject acc) {
		this.acc = acc;
		this.manager = manager;
		this.saveID = saveID;
		this.time = time;
		this.username = username;
		this.accID = accID;
	}

	/**
	 * Retrieves the account manager logger
	 * 
	 * @return Logger instance
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Retrieves the account manager
	 * 
	 * @return BasicAccountManager instance
	 */
	protected BasicAccountManager getManager() {
		return manager;
	}

	/**
	 * Retrieves the account object instance
	 * 
	 * @return BasicAccountObject instance
	 */
	protected BasicAccountObject getAccountObject() {
		return acc;
	}

	/**
	 * Retrieves the account ID
	 * 
	 * @return Account ID string
	 */
	protected String getAccountID() {
		return accID;
	}

	/**
	 * Called to update the save username
	 * 
	 * @param name New save username
	 * @return True if successful, false otherwise
	 */
	public abstract boolean performUpdateUsername(String name);

	/**
	 * Called to retrieve the save data container
	 * 
	 * @return AccountDataContainer instance
	 */
	protected abstract AccountDataContainer retrieveSaveData();

	@Override
	public long getCreationTime() {
		return time;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getSaveID() {
		return saveID;
	}

	@Override
	public AccountObject getAccount() {
		return acc;
	}

	@Override
	public boolean updateUsername(String name) {
		if (name.equalsIgnoreCase(username))
			return true;

		// Check validity
		String oldName = username;
		if (!manager.isValidUsername(name))
			return false;

		// Check if its in use
		if (!acc.getUsername().equalsIgnoreCase(name) && manager.isUsernameTaken(name)) {
			return false;
		} else {
			// Check if in use by any saves
			AccountObject accF = acc;
			if (Stream.of(acc.getSaveIDs()).map(t -> accF.getSave(t)).anyMatch(t -> {
				try {
					return t.getUsername().equalsIgnoreCase(name) && t.getSaveData().entryExists("avatar");
				} catch (IOException e) {
					return false;
				}
			})) {
				return false;
			}
		}

		// Check filters
		if (TextFilterService.getInstance().isFiltered(name, true))
			return false;

		// Perform update
		if (performUpdateUsername(name)) {
			username = name;

			// Dispatch event
			EventBus.getInstance().dispatchEvent(new AccountSaveUsernameUpdateEvent(oldName, name, acc, this, manager));

			// Log
			getLogger().info("Updated save username of " + oldName + " (ID " + getSaveID() + ") to '" + getUsername()
					+ "' of account " + getUsername() + " (ID " + getAccountID() + ")");

			// Return
			return true;
		}
		return false;
	}

	/**
	 * Called to delete saves
	 */
	protected abstract void doDeleteSave();

	@Override
	public void deleteSave() {
		doDeleteSave();
		MinigameDataManager.getInstance().deleteDataFor(saveID);
		acc.refreshSaveList();

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new AccountSaveDeletedEvent(acc, this, manager));

		// Log
		getLogger().info("Deleted save " + getUsername() + " (ID " + getSaveID() + ") of account " + getUsername()
				+ " (ID " + getAccountID() + ")");
	}

	@Override
	public AccountDataContainer getSaveData() {
		if (data == null)
			data = retrieveSaveData();
		return data;
	}

}
