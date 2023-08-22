package org.asf.edge.modules.gridapi.events.auth;

import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.modules.eventbus.EventPath;
import org.asf.edge.modules.gridapi.events.GridApiServerEvent;

import com.google.gson.JsonObject;

/**
 * 
 * Grid authentication success event
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("grid.authentication.success")
public class AuthenticationSuccessEvent extends GridApiServerEvent {

	private AccountObject account;
	private JsonObject responseObject;

	public AuthenticationSuccessEvent(AccountObject account, JsonObject responseObject) {
		this.account = account;
		this.responseObject = responseObject;
	}

	@Override
	public String eventPath() {
		return "grid.authentication.success";
	}

	/**
	 * Retrieves the account object instance
	 * 
	 * @return AccountObject instance
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
