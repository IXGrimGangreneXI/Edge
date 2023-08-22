package org.asf.edge.common.events.accounts;

import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Account deletion event - called just before accounts are deleted
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("accountmanager.account.deleted")
public class AccountDeletedEvent extends EventObject {

	private AccountObject account;
	private AccountManager accountManager;

	@Override
	public String eventPath() {
		return "accountmanager.account.deleted";
	}

	public AccountDeletedEvent(AccountObject account, AccountManager accountManager) {
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

}
