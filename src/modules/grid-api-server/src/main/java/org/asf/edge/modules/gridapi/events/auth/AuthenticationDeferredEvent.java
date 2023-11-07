package org.asf.edge.modules.gridapi.events.auth;

import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.modules.gridapi.events.GridApiServerEvent;
import org.asf.nexus.events.EventPath;

import com.google.gson.JsonObject;

/**
 * 
 * Grid authentication defer event
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("grid.authentication.deferred")
public class AuthenticationDeferredEvent extends GridApiServerEvent {

	private AccountObject account;
	private JsonObject responseObject;

	public AuthenticationDeferredEvent(AccountObject account, JsonObject responseObject) {
		this.account = account;
		this.responseObject = responseObject;
	}

	@Override
	public String eventPath() {
		return "grid.authentication.deferred";
	}

	/**
	 * Retrieves the data request string
	 * 
	 * @return Data request string
	 */
	public String getDataRequestKey() {
		return responseObject.get("dataRequestKey").getAsString();
	}

	/**
	 * Retrieves the account object instance
	 * 
	 * @return AccountObject instance or null
	 */
	public AccountObject getAccount() {
		return account;
	}

	/**
	 * Retrieves the response JSON object instance
	 * 
	 * @return Response JsonObject instance
	 */
	public JsonObject getResponseObject() {
		return responseObject;
	}

}
