package org.asf.edge.modules.gridclient.grid.events;

import org.asf.edge.modules.gridclient.phoenix.auth.LoginDeferredMessage;
import org.asf.edge.modules.gridclient.phoenix.auth.LoginManager;
import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

import com.google.gson.JsonObject;

/**
 * 
 * Grid Client Authentication Deferred Event - called when authentication is
 * deferred
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("grid.client.auth.deferred")
public class GridClientAuthenticationDeferredEvent extends EventObject {

	private LoginManager manager;
	private LoginDeferredMessage message;

	public GridClientAuthenticationDeferredEvent(LoginManager manager, LoginDeferredMessage message) {
		this.manager = manager;
		this.message = message;
	}

	/**
	 * Retrieves the login manager
	 * 
	 * @return LoginManager instance
	 */
	public LoginManager getLoginManager() {
		return manager;
	}

	/**
	 * Retrieves the response json
	 * 
	 * @return JsonObject instance
	 */
	public JsonObject getRawResponse() {
		return message.getRawResponse();
	}

	/**
	 * Retrieves the data request key
	 * 
	 * @return Data request key
	 */
	public String getDataRequestKey() {
		return message.getDataRequestKey();
	}

	/**
	 * Retries the login attempt
	 * 
	 * @param request New request message (recommended to send the old message with
	 *                the missing data added to it)
	 */
	public void retry(JsonObject request) {
		setHandled();
		message.retry(request);
	}

	@Override
	public String eventPath() {
		return "grid.client.auth.deferred";
	}

}
