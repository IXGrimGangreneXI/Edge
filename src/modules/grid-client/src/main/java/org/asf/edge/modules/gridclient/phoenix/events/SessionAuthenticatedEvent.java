package org.asf.edge.modules.gridclient.phoenix.events;

import org.asf.edge.modules.gridclient.phoenix.auth.LoginManager;
import org.asf.nexus.events.EventPath;

import com.google.gson.JsonObject;

@EventPath("phoenix.session.authenticated")
public class SessionAuthenticatedEvent extends LoginManagerEvent {

	private String accountID;
	private String displayName;

	private JsonObject rawResponse;

	public SessionAuthenticatedEvent(LoginManager manager, String accountID, String displayName,
			JsonObject rawResponse) {
		super(manager);
		this.accountID = accountID;
		this.displayName = displayName;
		this.rawResponse = rawResponse;
	}

	@Override
	public String eventPath() {
		return "phoenix.session.authenticated";
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

}
