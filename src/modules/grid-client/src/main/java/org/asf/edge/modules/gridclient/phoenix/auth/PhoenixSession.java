package org.asf.edge.modules.gridclient.phoenix.auth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.asf.edge.modules.gridclient.phoenix.events.SessionRefreshEvent;
import org.asf.edge.modules.gridclient.phoenix.events.SessionRefreshFailureEvent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 
 * Phoenix Session Container
 * 
 * @author Sky Swimmer
 *
 */
public class PhoenixSession {

	private String accountID;
	private String displayName;
	private String token;

	private LoginManager manager;
	private JsonObject rawResponse;

	public PhoenixSession(LoginManager manager, String accountID, String displayName, String token,
			JsonObject rawResponse) {
		this.manager = manager;
		this.accountID = accountID;
		this.displayName = displayName;
		this.token = token;
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

	/**
	 * Retrieves the game session token
	 * 
	 * @return Session token string
	 */
	public String getGameSessionToken() {
		return token;
	}

	/**
	 * Refreshes the session
	 * 
	 * @return True if successful, false otherwise
	 */
	public boolean refresh() {
		try {
			// Refresh token
			String url = manager.apiServer;
			if (!url.endsWith("/"))
				url += "/";
			url += "tokens/refresh";

			// Refresh token
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setRequestProperty("Authorization", "Bearer " + token);
			String tkn = new String(conn.getInputStream().readAllBytes(), "UTF-8");
			if (!tkn.isEmpty()) {
				tkn = tkn.trim();

				// Pull user info
				url = manager.apiServer;
				if (!url.endsWith("/"))
					url += "/";
				url += "identities/pullcurrent";
				conn = (HttpURLConnection) new URL(url).openConnection();
				conn.setRequestProperty("Authorization", "Bearer " + token);
				String result = new String(conn.getInputStream().readAllBytes(), "UTF-8");

				// Parse
				JsonObject obj = JsonParser.parseString(result).getAsJsonObject();
				if (!obj.has("properties")) {
					// Dispatch event
					manager.getEventBus().dispatchEvent(new SessionRefreshFailureEvent(manager, this));
					return false;
				}
				JsonObject props = obj.get("properties").getAsJsonObject();
				if (!props.has("displayName")) {
					// Dispatch event
					manager.getEventBus().dispatchEvent(new SessionRefreshFailureEvent(manager, this));
					return false;
				}
				JsonObject dsp = props.get("displayName").getAsJsonObject();
				if (!dsp.has("value")) {
					// Dispatch event
					manager.getEventBus().dispatchEvent(new SessionRefreshFailureEvent(manager, this));
					return false;
				}
				displayName = dsp.get("value").getAsString();
				token = tkn;
			} else {
				// Dispatch event
				manager.getEventBus().dispatchEvent(new SessionRefreshFailureEvent(manager, this));
				return false;
			}
		} catch (IOException e) {
			// Dispatch event
			manager.getEventBus().dispatchEvent(new SessionRefreshFailureEvent(manager, this));
			return false;
		}

		// Dispatch event
		manager.getEventBus().dispatchEvent(new SessionRefreshEvent(manager, this));
		return true;
	}

}
