package org.asf.edge.common.events.accounts;

import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Account manager load event - called after the account manager has been loaded
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("accountmanager.load")
public class AccountManagerLoadEvent extends EventObject {

	private AccountManager accountManager;

	@Override
	public String eventPath() {
		return "accountmanager.load";
	}

	public AccountManagerLoadEvent(AccountManager accountManager) {
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

}
