package org.asf.edge.common.events.accounts;

import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Account username update event - called when usernames of accounts are changed
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("accountmanager.account.usernameupdate")
public class AccountUsernameUpdateEvent extends EventObject {

	private AccountObject account;
	private AccountManager accountManager;
	private String oldName;
	private String newName;

	@Override
	public String eventPath() {
		return "accountmanager.account.usernameupdate";
	}

	public AccountUsernameUpdateEvent(String oldName, String newName, AccountObject account,
			AccountManager accountManager) {
		this.oldName = oldName;
		this.newName = newName;
		this.account = account;
		this.accountManager = accountManager;
	}

	/**
	 * Retrieves the account manager
	 * 
	 * @return AccountManager instance
	 */
	public AccountManager getAccountManager() {
		return accountManager;
	}

	/**
	 * Retrieves the account object
	 * 
	 * @return AccountObject instance
	 */
	public AccountObject getAccount() {
		return account;
	}

	/**
	 * Retrieves the old username
	 * 
	 * @return Old username
	 */
	public String getOldUsername() {
		return oldName;
	}

	/**
	 * Retrieves the new username
	 * 
	 * @return New username
	 */
	public String getNewUsername() {
		return newName;
	}

}
