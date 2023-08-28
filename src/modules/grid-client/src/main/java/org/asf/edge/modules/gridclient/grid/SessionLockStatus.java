package org.asf.edge.modules.gridclient.grid;

public enum SessionLockStatus {

	/**
	 * The token is valid and we still are the only one using the account
	 */
	LOCK_HELD,

	/**
	 * The token is valid but we arent the client that holds the current lock,
	 * someone elsewhere logged into the account
	 */
	LOCK_LOST,

	/**
	 * Token is no longer valid
	 */
	TOKEN_INVALID,

	/**
	 * Couldn't connect to the Grid API
	 */
	CONNECTION_FAILURE

}
