package org.asf.edge.common.events.accounts;

import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Account register event - called when accounts are registered
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("accountmanager.account.registered")
public class AccountRegisteredEvent extends EventObject {

	private AccountObject account;
	private AccountManager accountManager;

	@Override
	public String eventPath() {
		return "accountmanager.account.registered";
	}

	public AccountRegisteredEvent(AccountObject account, AccountManager accountManager) {
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
