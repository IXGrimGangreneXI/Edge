package org.asf.edge.modules.gridclient.phoenix.auth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.function.Consumer;

import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.modules.eventbus.EventBus;
import org.asf.edge.modules.gridclient.phoenix.PhoenixEnvironment;
import org.asf.edge.modules.gridclient.phoenix.events.SessionAuthenticatedEvent;
import org.asf.edge.modules.gridclient.phoenix.events.SessionLogoutEvent;
import org.asf.edge.modules.gridclient.phoenix.events.SessionRefreshFailureEvent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 
 * Phoenix Login Manager
 * 
 * @author Sky Swimmer
 *
 */
public class LoginManager {

	private PhoenixSession session;
	private EventBus eventBus;

	public LoginManager() {
		eventBus = EventBus.getInstance().createBus();
		eventBus.addEventHandler(SessionRefreshFailureEvent.class, event -> logout());
	}

	/**
	 * API server endpoint
	 */
	public String apiServer = PhoenixEnvironment.defaultAPIServer;

	/**
	 * Token used to call the authenticate endpoint
	 */
	public String loginToken;

	/**
	 * Retrieves the login manager event bus
	 * 
	 * @return EventBus instance
	 */
	public EventBus getEventBus() {
		return eventBus;
	}

	/**
	 * Retrieves the Phoenix session
	 * 
	 * @return PhoenixSession instance or null
	 */
	public PhoenixSession getSession() {
		return session;
	}

	/**
	 * Checks if the Phoenix client is logged in
	 * 
	 * @return True if logged in, false otherwise
	 */
	public boolean isLoggedIn() {
		return session != null;
	}

	/**
	 * Logs the session out
	 */
	public void logout() {
		if (isLoggedIn()) {
			getEventBus().dispatchEvent(new SessionLogoutEvent(this, session));
			session = null;
		}
	}

	/**
	 * Logs into the authentication API to retrieve login information
	 * 
	 * @param loginPayload Login payload message
	 * @param onFailure    Login failure handler
	 * @param onDefer      Login deferred handler
	 * @param onSuccess    Login success handler
	 * @return True if successful, false otherwise, note that when login is deferred
	 *         this method will also return false
	 */
	public boolean login(JsonObject loginPayload, Consumer<LoginFailureMessage> onFailure,
			Consumer<LoginDeferredMessage> onDefer, Consumer<PhoenixSession> onSuccess) {
		if (isLoggedIn())
			logout();
		if (loginToken == null && PhoenixEnvironment.defaultLoginToken == null)
			throw new IllegalArgumentException("Missing login token");

		try {
			// Build URL
			String url = apiServer;
			if (!url.endsWith("/"))
				url += "/";
			url += "auth/authenticate";

			// Contact Phoenix
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setRequestProperty("Authorization",
					"Bearer " + (loginToken == null ? PhoenixEnvironment.defaultLoginToken : loginToken));
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.getOutputStream().write(loginPayload.toString().getBytes("UTF-8"));

			// Read response
			JsonObject response = JsonParser
					.parseString(new String((conn.getResponseCode() >= 400 ? conn.getErrorStream().readAllBytes()
							: conn.getInputStream().readAllBytes()), "UTF-8"))
					.getAsJsonObject();
			if (!response.has("status")) {
				// Handle unparseable response
				if (response.has("error")) {
					onFailure.accept(new LoginFailureMessage(response, response.get("error").getAsString(),
							response.has("errorMessage") ? response.get("errorMessage").getAsString()
									: "Missing error message, server error"));
					return false;
				} else {
					throw new Exception("Invalid response data");
				}
			} else {
				// Handle response
				switch (response.get("status").getAsString()) {

				case "success": {
					// Authenticated
					String acc = response.get("accountID").getAsString();
					String dsp = response.get("displayName").getAsString();
					String ses = response.get("sessionToken").getAsString();
					PhoenixSession sesData = new PhoenixSession(this, acc, dsp, ses, response);
					session = sesData;
					onSuccess.accept(sesData);
					response.remove("sessionToken");
					getEventBus().dispatchEvent(new SessionAuthenticatedEvent(this, session.getAccountID(),
							session.getDisplayName(), session.getRawResponse()));

					// Start refresh
					AsyncTaskManager.runAsync(() -> {
						while (isLoggedIn()) {
							// Check session
							if (getSession() != sesData)
								break;

							try {
								// Parse token
								String[] parts = ses.split("\\.");
								String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), "UTF-8");
								JsonObject payload = JsonParser.parseString(payloadJson).getAsJsonObject();
								if ((System.currentTimeMillis() / 1000) + (15 * 60) >= payload.get("exp").getAsLong()) {
									// Refresh
									if (!sesData.refresh()) {
										if (sesData == session)
											logout();
										break;
									}
								}

								// Wait
								Thread.sleep(1000);
							} catch (Exception e) {
								if (isLoggedIn() && getSession() == sesData)
									logout();
								break;
							}
						}
					});
					return true;
				}

				case "deferred": {
					// Deferred
					onDefer.accept(new LoginDeferredMessage(response, response.get("dataRequestKey").getAsString(),
							req -> login(req, onFailure, onDefer, onSuccess)));
					return false;
				}

				case "failure": {
					// Failed
					onFailure.accept(new LoginFailureMessage(response, response.get("error").getAsString(),
							response.get("errorMessage").getAsString()));
					return false;
				}

				default:
					throw new Exception("Invalid response data");

				}
			}
		} catch (IOException e) {
			JsonObject error = new JsonObject();
			error.addProperty("error", "connect_error");
			error.addProperty("errorMessage", "Could not connect to the authentication API");
			onFailure.accept(new LoginFailureMessage(error, error.get("error").getAsString(),
					error.get("errorMessage").getAsString()));
			return false;
		} catch (Exception e) {
			JsonObject error = new JsonObject();
			error.addProperty("error", "processor_error");
			error.addProperty("errorMessage", "Failed to process login request or response, unknown error");
			onFailure.accept(new LoginFailureMessage(error, error.get("error").getAsString(),
					error.get("errorMessage").getAsString()));
			return false;
		}
	}

}
