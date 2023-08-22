package org.asf.edge.modules.gridapi.events.auth;

import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.modules.eventbus.EventPath;
import org.asf.edge.modules.gridapi.events.GridApiServerEvent;

import com.google.gson.JsonObject;

/**
 * 
 * Grid authentication event, called when a player is being authenticated
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("grid.authenticate")
public class GridAuthenticateEvent extends GridApiServerEvent {

	private AccountObject account;
	private JsonObject responseObject;
	private JsonObject requestObject;

	public GridAuthenticateEvent(AccountObject account, JsonObject requestObject, JsonObject responseObject) {
		this.account = account;
		this.requestObject = requestObject;
		this.responseObject = responseObject;
	}

	@Override
	public String eventPath() {
		return "grid.authenticate";
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

	/**
	 * Retrieves the request JSON object instance
	 * 
	 * @return Request JsonObject instance
	 */
	public JsonObject getRequestObject() {
		return requestObject;
	}

	/**
	 * Fails login
	 * 
	 * @param error        Error type string
	 * @param errorMessage Error message strin
	 */
	public void fail(String error, String errorMessage) {
		responseObject = new JsonObject();
		responseObject.addProperty("status", "failure");
		responseObject.addProperty("error", error);
		responseObject.addProperty("errorMessage", errorMessage);
		setHandled();
	}

	/**
	 * Defers login
	 * 
	 * @param dataRequestKey Data request key that is missing
	 */
	public void defer(String dataRequestKey) {
		responseObject = new JsonObject();
		responseObject.addProperty("status", "deferred");
		responseObject.addProperty("dataRequestKey", dataRequestKey);
		setHandled();
	}

}
