package org.asf.edge.common.services.accounts;

import org.asf.edge.common.services.AbstractService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.impl.DatabaseAccountManager;
import org.asf.edge.common.services.accounts.impl.RemoteHttpAccountManager;
import org.asf.edge.common.tokens.TokenParseResult;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * 
 * Account manager service
 * 
 * @author Sky Swimmer
 *
 */
public abstract class AccountManager extends AbstractService {
	private static boolean initedServices;

	/**
	 * Retrieves the account manager service
	 * 
	 * @return AccountManager instance
	 */
	public static AccountManager getInstance() {
		return ServiceManager.getService(AccountManager.class);
	}

	/**
	 * Internal
	 */
	public static void initAccountManagerServices(int priorityRemote, int priorityDatabase) {
		if (initedServices)
			return;
		initedServices = true;

		// Write/load config
		Logger logger = LogManager.getLogger("AccountManager");
		File configFile = new File("accountmanager.json");
		JsonObject accountManagerConfig = new JsonObject();
		if (configFile.exists()) {
			try {
				accountManagerConfig = JsonParser.parseString(Files.readString(configFile.toPath())).getAsJsonObject();
			} catch (JsonSyntaxException | IOException e) {
				logger.error("Failed to load account manager configuration!", e);
				return;
			}
		}
		boolean changed = false;
		JsonObject remoteManagerConfig = new JsonObject();
		if (!accountManagerConfig.has("remoteHttpManager")) {
			remoteManagerConfig.addProperty("priority", priorityRemote);
			remoteManagerConfig.addProperty("url", "http://127.0.0.1:5324/accountmanager/");
			accountManagerConfig.add("remoteHttpManager", remoteManagerConfig);
			changed = true;
		} else
			remoteManagerConfig = accountManagerConfig.get("remoteHttpManager").getAsJsonObject();
		JsonObject databaseManagerConfig = new JsonObject();
		if (!accountManagerConfig.has("databaseManager")) {
			databaseManagerConfig.addProperty("priority", priorityDatabase);
			databaseManagerConfig.addProperty("url", "jdbc:sqlite:account-data.db");
			JsonObject props = new JsonObject();
			databaseManagerConfig.add("properties", props);
			accountManagerConfig.add("databaseManager", databaseManagerConfig);
			changed = true;
		} else
			databaseManagerConfig = accountManagerConfig.get("databaseManager").getAsJsonObject();
		if (changed) {
			// Write config
			try {
				Files.writeString(configFile.toPath(),
						new Gson().newBuilder().setPrettyPrinting().create().toJson(accountManagerConfig));
			} catch (IOException e) {
				logger.error("Failed to write the account manager configuration!", e);
				return;
			}
		}

		// Register default account managers
		ServiceManager.registerServiceImplementation(AccountManager.class, new RemoteHttpAccountManager(),
				remoteManagerConfig.get("priority").getAsInt());
		ServiceManager.registerServiceImplementation(AccountManager.class, new DatabaseAccountManager(),
				databaseManagerConfig.get("priority").getAsInt());
	}

	/**
	 * Checks if a username is of valid format
	 * 
	 * @param username Username to check
	 * @return True if valid, false if it contains invalid characters
	 */
	public abstract boolean isValidUsername(String username);

	/**
	 * Checks if a password is of valid format
	 * 
	 * @param password Password to check
	 * @return True if valid, false if it is too short
	 */
	public abstract boolean isValidPassword(String password);

	/**
	 * Checks if a username is in use
	 * 
	 * @param username Username to check
	 * @return True if in use, false otherwise
	 */
	public abstract boolean isUsernameTaken(String username);

	/**
	 * Retrieves account IDs by email
	 * 
	 * @param email Email to retrieve the account ID for
	 * @return Account ID string or null
	 */
	public abstract String getAccountIDByEmail(String email);

	/**
	 * Retrieves account IDs by username
	 * 
	 * @param username Username to retrieve the account ID for
	 * @return Account ID string or null
	 */
	public abstract String getAccountID(String username);

	/**
	 * Checks if an account ID exists
	 * 
	 * @param id Account ID
	 * @return True if the account exists, false otherwise
	 */
	public abstract boolean accountExists(String id);

	/**
	 * Called to verify passwords
	 * 
	 * @param id       Account ID
	 * @param password Account password
	 * @return True if valid, false otherwise
	 */
	public abstract boolean verifyPassword(String id, String password);

	/**
	 * Retrieves account objects
	 * 
	 * @param id Account ID
	 * @return AccountObject instance or null if not found
	 */
	public abstract AccountObject getAccount(String id);

	/**
	 * Retrieves guest account objects
	 * 
	 * @param guestID Guest ID (not a account ID)
	 * @return AccountObject instance or null if not found
	 */
	public abstract AccountObject getGuestAccount(String guestID);

	/**
	 * Called to verify JWT tokens
	 * 
	 * @param token Token string to verify
	 * @return TokenParseResult value
	 */
	public abstract TokenParseResult verifyToken(String token);

	/**
	 * Called to sign JWT tokens
	 * 
	 * @param token Token string to sign (header + payload)
	 * @return SHA-256 signature
	 */
	public abstract byte[] signToken(String token);

	/**
	 * Registers a guest account
	 * 
	 * @param guestID Guest account ID
	 * @return AccountObject instance or null
	 */
	public abstract AccountObject registerGuestAccount(String guestID);

	/**
	 * Registers accounts
	 * 
	 * @param username Username to use
	 * @param email    Email to use
	 * @param password Password to use
	 * @return AccountObject instance or null
	 */
	public abstract AccountObject registerAccount(String username, String email, char[] password);

	/**
	 * Retrieves save data by ID
	 * 
	 * @param id Save data ID
	 * @return AccountSaveContainer instance or null
	 */
	public abstract AccountSaveContainer getSaveByID(String id);

	/**
	 * Called to initialize the account manager
	 */
	public abstract void loadManager();

}
