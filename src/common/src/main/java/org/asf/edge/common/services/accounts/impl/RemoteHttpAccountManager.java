package org.asf.edge.common.services.accounts.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.events.accounts.AccountAuthenticatedEvent;
import org.asf.edge.common.events.accounts.AccountRegisteredEvent;
import org.asf.edge.common.events.accounts.GuestAccountRegisteredEvent;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.accounts.impl.accounts.http.RemoteHttpAccountObject;
import org.asf.edge.common.services.accounts.impl.accounts.http.RemoteHttpSaveContainer;
import org.asf.edge.common.services.config.ConfigProviderService;
import org.asf.edge.common.tokens.TokenParseResult;
import org.asf.edge.common.util.HttpUpgradeUtil;
import org.asf.edge.common.util.SimpleBinaryMessageClient;
import org.asf.edge.modules.eventbus.EventBus;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RemoteHttpAccountManager extends AccountManager {

	private String urlBase = "http://127.0.0.1:5324/accountmanager/";
	private Logger logger = LogManager.getLogger("AccountManager");

	@Override
	public void initService() {
	}

	@Override
	public void loadManager() {
		// Write/load config
		JsonObject accountManagerConfig;
		try {
			accountManagerConfig = ConfigProviderService.getInstance().loadConfig("server", "accountmanager");
		} catch (IOException e) {
			logger.error("Failed to load account manager configuration!", e);
			return;
		}
		if (accountManagerConfig == null) {
			accountManagerConfig = new JsonObject();
		}
		JsonObject config = new JsonObject();
		if (!accountManagerConfig.has("remoteHttpManager")) {
			// Generate config
			config.addProperty("url", urlBase);
			accountManagerConfig.add("remoteHttpManager", config);

			// Write config
			try {
				ConfigProviderService.getInstance().saveConfig("server", "accountmanager", accountManagerConfig);
			} catch (IOException e) {
				logger.error("Failed to write the account manager configuration!", e);
				return;
			}
		} else
			config = accountManagerConfig.get("remoteHttpManager").getAsJsonObject();

		// Load url
		urlBase = config.get("url").getAsString();
		if (!urlBase.endsWith("/"))
			urlBase += "/";
		logger.info("Using remote account manager server, url: " + urlBase);
		logger.warn("Warning: the http-based remote account manager service is not efficient!");
		logger.warn("Warning: its highly recommened to use a different implementation, such as a database server.");
	}

	/**
	 * Creates account manager requests
	 * 
	 * @param function Function name
	 * @param payload  Payload json
	 * @return Response object
	 * @throws IOException If contacting the server fails
	 */
	public JsonObject accountManagerRequest(String function, JsonObject payload) throws IOException {
		// Build url
		String url = urlBase;
		url += function;

		// Open connection
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);

		// Write request
		conn.getOutputStream().write(payload.toString().getBytes("UTF-8"));

		// Check response
		if (conn.getResponseCode() != 200)
			throw new IOException("Server returned HTTP " + conn.getResponseCode() + " " + conn.getResponseMessage());

		// Read response
		try {
			return JsonParser.parseString(new String(conn.getInputStream().readAllBytes(), "UTF-8")).getAsJsonObject();
		} catch (Exception e) {
			throw new IOException("Server returned a non-json response");
		}
	}

	/**
	 * Creates account data manager protocol upgrade requests
	 * 
	 * @param function Function name
	 * @param payload  Payload json
	 * @return Socket instance
	 * @throws IOException If contacting the server fails
	 */
	public Socket accountManagerUpgradeRequest(String function, JsonObject payload, String upgradeProtocol,
			String expectedResponseProtocol) throws IOException {
		// Build url
		String url = urlBase;
		url += function;

		// Open connection
		byte[] body = payload.toString().getBytes("UTF-8");
		return HttpUpgradeUtil.upgradeRequest(url, "POST", new ByteArrayInputStream(body), body.length,
				Map.of("X-Request-ID", UUID.randomUUID().toString()), new HashMap<String, String>(), upgradeProtocol,
				expectedResponseProtocol);
	}

	@Override
	public boolean isValidUsername(String username) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("username", username);
			JsonObject response = accountManagerRequest("isValidUsername", payload);
			return response.get("result").getAsBoolean();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in isValidUsername!", e);
			return false;
		}
	}

	@Override
	public boolean isValidPassword(String password) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("password", password);
			JsonObject response = accountManagerRequest("isValidPassword", payload);
			return response.get("result").getAsBoolean();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in isValidUsername!", e);
			return false;
		}
	}

	@Override
	public boolean isUsernameTaken(String username) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("username", username);
			JsonObject response = accountManagerRequest("isUsernameTaken", payload);
			return response.get("result").getAsBoolean();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in isUsernameTaken!", e);
			return false;
		}
	}

	@Override
	public String getAccountID(String username) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("username", username);
			JsonObject response = accountManagerRequest("getAccountID", payload);
			if (!response.get("success").getAsBoolean())
				return null;
			return response.get("id").getAsString();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in getAccountID!", e);
			return null;
		}
	}

	@Override
	public String getAccountIdBySaveUsername(String username) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("username", username);
			JsonObject response = accountManagerRequest("getAccountIdBySaveUsername", payload);
			if (!response.get("success").getAsBoolean())
				return null;
			return response.get("id").getAsString();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in getAccountID!", e);
			return null;
		}
	}

	@Override
	public boolean verifyPassword(String id, String password) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			payload.addProperty("password", password);
			JsonObject response = accountManagerRequest("verifyPassword", payload);
			if (!response.get("result").getAsBoolean())
				return false;
		} catch (IOException e) {
			logger.error("Account server query failure occurred in isValidUsername!", e);
			return false;
		}

		// Return
		return true;
	}

	@Override
	public boolean accountExists(String id) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			JsonObject response = accountManagerRequest("accountExists", payload);
			return response.get("result").getAsBoolean();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in accountExists!", e);
			return false;
		}
	}

	@Override
	public AccountObject getAccount(String id) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("id", id);
			JsonObject response = accountManagerRequest("getAccount", payload);
			if (!response.get("success").getAsBoolean())
				return null;

			// Return remote account object
			return new RemoteHttpAccountObject(id, response.get("username").getAsString(), this);
		} catch (IOException e) {
			logger.error("Account server query failure occurred in getAccount!", e);
			return null;
		}
	}

	@Override
	public AccountObject getGuestAccount(String guestID) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("guestID", guestID);
			JsonObject response = accountManagerRequest("getGuestAccount", payload);
			if (!response.get("success").getAsBoolean())
				return null;

			// Return remote account object
			return new RemoteHttpAccountObject(response.get("id").getAsString(), response.get("username").getAsString(),
					this);
		} catch (IOException e) {
			logger.error("Account server query failure occurred in getGuestAccount!", e);
			return null;
		}
	}

	@Override
	public String getAccountIDByEmail(String email) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("email", email);
			JsonObject response = accountManagerRequest("getAccountIDByEmail", payload);
			if (!response.get("success").getAsBoolean())
				return null;
			return response.get("id").getAsString();
		} catch (IOException e) {
			logger.error("Account server query failure occurred in getAccountIDByEmail!", e);
			return null;
		}
	}

	@Override
	public TokenParseResult verifyToken(String token) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("token", token);
			JsonObject response = accountManagerRequest("verifyToken", payload);
			return TokenParseResult.values()[response.get("result").getAsInt()];
		} catch (IOException e) {
			logger.error("Account server query failure occurred in verifyToken!", e);
			return null;
		}
	}

	@Override
	public byte[] signToken(String token) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("token", token);
			JsonObject response = accountManagerRequest("signToken", payload);
			return Base64.getDecoder().decode(response.get("result").getAsString());
		} catch (IOException e) {
			logger.error("Account server query failure occurred in signToken!", e);
			return null;
		}
	}

	@Override
	public AccountObject registerGuestAccount(String guestID) {
		// Request
		RemoteHttpAccountObject obj;
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("guestID", guestID);
			JsonObject response = accountManagerRequest("registerGuestAccount", payload);
			if (!response.get("success").getAsBoolean())
				return null;

			// Create remote account object
			obj = new RemoteHttpAccountObject(response.get("id").getAsString(), response.get("username").getAsString(),
					this);
		} catch (IOException e) {
			logger.error("Account server query failure occurred in registerGuestAccount!", e);
			return null;
		}

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new GuestAccountRegisteredEvent(obj, this));

		// Return
		return obj;
	}

	@Override
	public AccountObject registerAccount(String username, String email, char[] password) {
		// Request
		RemoteHttpAccountObject obj;
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("username", username);
			payload.addProperty("email", email);
			payload.addProperty("password", new String(password));
			JsonObject response = accountManagerRequest("registerAccount", payload);
			if (!response.get("success").getAsBoolean())
				return null;

			// Create remote account object
			obj = new RemoteHttpAccountObject(response.get("id").getAsString(), response.get("username").getAsString(),
					this);
		} catch (IOException e) {
			logger.error("Account server query failure occurred in registerAccount!", e);
			return null;
		}

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new AccountRegisteredEvent(obj, this));

		// Return
		return obj;
	}

	@Override
	public AccountSaveContainer getSaveByID(String id) {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			payload.addProperty("save", id);
			JsonObject response = accountManagerRequest("getSaveByID", payload);
			if (!response.get("success").getAsBoolean())
				return null;
			return new RemoteHttpSaveContainer(id, response.get("time").getAsLong(),
					response.get("username").getAsString(), response.get("accid").getAsString(), this,
					getAccount(response.get("accid").getAsString()));
		} catch (IOException e) {
			logger.error("Account server query failure occurred in getSaveByID!", e);
			return null;
		}
	}

	@Override
	public String[] getOnlinePlayerIDs() {
		// Request
		try {
			// Build payload
			JsonObject payload = new JsonObject();
			JsonObject response = accountManagerRequest("getOnlinePlayerIDs", payload);
			JsonArray arr = response.get("players").getAsJsonArray();
			String[] saves = new String[arr.size()];
			int i = 0;
			for (JsonElement ele : arr)
				saves[i++] = ele.getAsString();
			return saves;
		} catch (IOException e) {
			logger.error("Account server query failure occurred in getOnlinePlayerIDs!", e);
			return new String[0];
		}
	}

	@Override
	public void runForAllAccounts(Function<AccountObject, Boolean> func) {
		try {
			// Build url
			String url = urlBase;
			url += "runForAllAccounts";

			// Open connection
			Socket conn = HttpUpgradeUtil.upgradeRequest(url, "POST", null, -1,
					Map.of("X-Request-ID", UUID.randomUUID().toString()), new HashMap<String, String>(),
					"EDGEBINPROT/ACCMANAGER/RUNFORALLACCOUNTS", "EDGEBINPROT/ACCMANAGER/RUNFORALLACCOUNTS");

			// Handle
			SimpleBinaryMessageClient binH = new SimpleBinaryMessageClient((packet, client) -> {
				// Read message
				try {
					String id = new String(packet.data, "UTF-8");

					// Handle
					boolean res = func.apply(getAccount(id));

					// Send response
					client.send(new byte[] { res ? (byte) 1 : (byte) 0 });
					if (!res)
						return false;
				} catch (Exception e) {
					logger.error("Exception occurred while running runForAllAccounts!", e);
					return false;
				}
				return true;
			}, conn.getInputStream(), conn.getOutputStream());
			binH.start();
			conn.close();
		} catch (IOException e) {
			logger.error("Account server query failure occured in runForAllAccounts!", e);
		}
	}

}
