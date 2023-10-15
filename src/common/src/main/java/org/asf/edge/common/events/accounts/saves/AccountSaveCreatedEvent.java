package org.asf.edge.common.events.accounts.saves;

import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Account save creation event - called when account saves are created
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("accountmanager.account.save.created")
public class AccountSaveCreatedEvent extends EventObject {

	private AccountObject account;
	private AccountSaveContainer save;
	private AccountManager accountManager;

	@Override
	public String eventPath() {
		return "accountmanager.account.save.created";
	}

	public AccountSaveCreatedEvent(AccountObject account, AccountSaveContainer save, AccountManager accountManager) {
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

}
