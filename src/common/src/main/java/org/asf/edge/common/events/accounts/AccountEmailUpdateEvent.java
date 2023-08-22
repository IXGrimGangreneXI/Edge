package org.asf.edge.common.events.accounts;

import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Account email update event - called when emails of accounts are changed
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("accountmanager.account.emailupdate")
public class AccountEmailUpdateEvent extends EventObject {

	private AccountObject account;
	private AccountManager accountManager;
	private String oldMail;
	private String newMail;

	@Override
	public String eventPath() {
		return "accountmanager.account.emailupdate";
	}

	public AccountEmailUpdateEvent(String oldMail, String newMail, AccountObject account,
			AccountManager accountManager) {
		this.oldMail = oldMail;
		this.newMail = newMail;
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
	 * Retrieves the old email address
	 * 
	 * @return Old email address
	 */
	public String getOldEmail() {
		return oldMail;
	}

	/**
	 * Retrieves the new email address
	 * 
	 * @return New email address
	 */
	public String getNewEmail() {
		return newMail;
	}

}
