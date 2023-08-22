package org.asf.edge.common.events.accounts;

import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Guest account register event - called when guest accounts are registered
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("accountmanager.guestaccount.registered")
public class GuestAccountRegisteredEvent extends EventObject {

	private AccountObject account;
	private AccountManager accountManager;

	@Override
	public String eventPath() {
		return "accountmanager.guestaccount.registered";
	}

	public GuestAccountRegisteredEvent(AccountObject account, AccountManager accountManager) {
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
