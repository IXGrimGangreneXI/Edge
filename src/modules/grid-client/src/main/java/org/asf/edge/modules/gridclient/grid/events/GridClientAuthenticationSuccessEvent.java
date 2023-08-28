package org.asf.edge.modules.gridclient.grid.events;

import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;
import org.asf.edge.modules.gridclient.phoenix.auth.LoginManager;

import com.google.gson.JsonObject;

/**
 * 
 * Grid Client Authentication Success Event - called when authentication
 * succeeds
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("grid.client.auth.success")
public class GridClientAuthenticationSuccessEvent extends EventObject {

	private LoginManager manager;

	private String accountID;
	private String displayName;

	private JsonObject rawResponse;

	public GridClientAuthenticationSuccessEvent(LoginManager manager, String accountID, String displayName,
			JsonObject rawResponse) {
		this.manager = manager;
		this.accountID = accountID;
		this.displayName = displayName;
		this.rawResponse = rawResponse;
	}

	/**
	 * Retrieves the login manager
	 * 
	 * @return LoginManager instance
	 */
	public LoginManager getManager() {
		return manager;
	}

	/**
	 * Retrieves the authentication response payload
	 * 
	 * @return JsonObject instance
	 */
	public JsonObject getRawResponse() {
		return rawResponse;
	}

	/**
	 * Retrieves the account ID
	 * 
	 * @return Account ID string
	 */
	public String getAccountID() {
		return accountID;
	}

	/**
	 * Retrieves the account display name
	 * 
	 * @return Account display name
	 */
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public String eventPath() {
		return "grid.client.auth.success";
	}

}
