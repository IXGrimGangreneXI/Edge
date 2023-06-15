package org.asf.edge.common.account;

import org.asf.edge.common.services.AbstractService;
import org.asf.edge.common.services.ServiceManager;

/**
 * 
 * Account manager service
 * 
 * @author Sky Swimmer
 *
 */
public abstract class AccountManager extends AbstractService {

	/**
	 * Retrieves the account manager service
	 * 
	 * @return AccountManager instance
	 */
	public static AccountManager getInstance() {
		return ServiceManager.getService(AccountManager.class);
	}

	/**
	 * Checks if a username is valid
	 * 
	 * @param username Username to check
	 * @return True if valid, false if it contains invalid characters
	 */
	public abstract boolean isValidUsername(String username);

	/**
	 * Checks if a username is in use
	 * 
	 * @param username Username to check
	 * @return True if in use, false otherwise
	 */
	public abstract boolean isUsernameTaken(String username);

	/**
	 * Retrieves account IDs by username
	 * 
	 * @param username Username to retrieve the account ID for
	 * @return Account ID string or null
	 */
	public abstract String getAccountID(String username);

	/**
	 * Called to initialize the account manager
	 */
	public abstract void loadManager();

}
