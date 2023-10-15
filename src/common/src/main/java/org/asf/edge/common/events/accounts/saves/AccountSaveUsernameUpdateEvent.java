package org.asf.edge.common.events.accounts.saves;

import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Account save username update event - called when usernames of account saves
 * are changed
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("accountmanager.account.save.usernameupdate")
public class AccountSaveUsernameUpdateEvent extends EventObject {

	private AccountObject account;
	private AccountSaveContainer save;
	private AccountManager accountManager;
	private String oldName;
	private String newName;

	@Override
	public String eventPath() {
		return "accountmanager.account.save.usernameupdate";
	}

	public AccountSaveUsernameUpdateEvent(String oldName, String newName, AccountObject account,
			AccountSaveContainer save, AccountManager accountManager) {
		this.oldName = oldName;
		this.newName = newName;
		this.account = account;
		this.save = save;
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
	 * Retrieves the account save object
	 * 
	 * @return AccountSaveContainer instance
	 */
	public AccountSaveContainer getSave() {
		return save;
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
