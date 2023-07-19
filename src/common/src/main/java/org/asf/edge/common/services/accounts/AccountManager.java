package org.asf.edge.common.services.accounts;

import org.asf.edge.common.services.AbstractService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.impl.DefaultDatabaseAccountManager;
import org.asf.edge.common.services.accounts.impl.PostgresDatabaseAccountManager;
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
	public static void initAccountManagerServices(int priorityRemote, int priorityDatabase, int priorityPostgres) {
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
			databaseManagerConfig.addProperty("url", "jdbc:locking:sqlite:account-data.db");
			JsonObject props = new JsonObject();
			databaseManagerConfig.add("properties", props);
			accountManagerConfig.add("databaseManager", databaseManagerConfig);
			changed = true;
		} else
			databaseManagerConfig = accountManagerConfig.get("databaseManager").getAsJsonObject();
		JsonObject postgresManagerConfig = new JsonObject();
		if (!accountManagerConfig.has("postgreSQL")) {
			postgresManagerConfig.addProperty("priority", priorityPostgres);
			postgresManagerConfig.addProperty("url", "jdbc:postgresql://localhost/edge");
			JsonObject props = new JsonObject();
			postgresManagerConfig.add("properties", props);
			accountManagerConfig.add("postgreSQL", postgresManagerConfig);
			changed = true;
		} else
			postgresManagerConfig = accountManagerConfig.get("postgreSQL").getAsJsonObject();
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
		ServiceManager.registerServiceImplementation(AccountManager.class, new DefaultDatabaseAccountManager(),
				databaseManagerConfig.get("priority").getAsInt());
		ServiceManager.registerServiceImplementation(AccountManager.class, new PostgresDatabaseAccountManager(),
				postgresManagerConfig.get("priority").getAsInt());
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
	 * Retrieves account IDs by save username
	 * 
	 * @param username Save username to retrieve the account ID for
	 * @return Account ID string or null
	 */
	public abstract String getAccountIdBySaveUsername(String username);

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
	 * Retrieves account objects<br/>
	 * <br/>
	 * <b>Please keep in mind that each time this is called, a new account object is
	 * created that will cache account data, saves, and save data containers, use as
	 * much as possible while needing to interact with account data, and get rid of
	 * as early as possible when done, do NOT keep this in a static field else you
	 * will run into memory leaks.</b>
	 * 
	 * @param id Account ID
	 * @return AccountObject instance or null if not found
	 */
	public abstract AccountObject getAccount(String id);

	/**
	 * Retrieves guest account objects<br/>
	 * <br/>
	 * <b>Please keep in mind that each time this is called, a new account object is
	 * created that will cache account data, saves, and save data containers, use as
	 * much as possible while needing to interact with account data, and get rid of
	 * as early as possible when done, do NOT keep this in a static field else you
	 * will run into memory leaks.</b>
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
	 * Registers a guest account<br/>
	 * <br/>
	 * <b>Please keep in mind that each time this is called, a new account object is
	 * returned that will cache account data, saves, and save data containers, use
	 * as much as possible while needing to interact with account data, and get rid
	 * of as early as possible when done, do NOT keep this in a static field else
	 * you will run into memory leaks.</b>
	 * 
	 * @param guestID Guest account ID
	 * @return AccountObject instance or null
	 */
	public abstract AccountObject registerGuestAccount(String guestID);

	/**
	 * Registers accounts<br/>
	 * <br/>
	 * <b>Please keep in mind that each time this is called, a new account object is
	 * returned that will cache account data, saves, and save data containers, use
	 * as much as possible while needing to interact with account data, and get rid
	 * of as early as possible when done, do NOT keep this in a static field else
	 * you will run into memory leaks.</b>
	 * 
	 * @param username Username to use
	 * @param email    Email to use
	 * @param password Password to use
	 * @return AccountObject instance or null
	 */
	public abstract AccountObject registerAccount(String username, String email, char[] password);

	/**
	 * Retrieves save data by ID<br/>
	 * <br/>
	 * <b>Please keep in mind that each time this is called, a new save container is
	 * returned that will cache save data containers, use as much as possible while
	 * needing to interact with account data, and get rid of as early as possible
	 * when done, do NOT keep this in a static field else you will run into memory
	 * leaks.</b>
	 * 
	 * @param id Save data ID
	 * @return AccountSaveContainer instance or null
	 */
	public abstract AccountSaveContainer getSaveByID(String id);

	/**
	 * Retrieves all player IDs that are online on THIS server
	 * 
	 * @return String of online player IDs
	 */
	public abstract String[] getOnlinePlayerIDs();

	/**
	 * Retrieves all online player IDs
	 * 
	 * @return Array of AccountObject instances
	 */
	public AccountObject[] getOnlinePlayers() {
		return Stream.of(getOnlinePlayerIDs()).map(t -> getAccount(t)).toArray(t -> new AccountObject[t]);
	}

	/**
	 * Runs functions for all accounts
	 * 
	 * @param func Function to run (return true to proceed to the next account,
	 *             return false to stop iterating)
	 */
	public abstract void runForAllAccounts(Function<AccountObject, Boolean> func);

	/**
	 * Called to initialize the account manager
	 */
	public abstract void loadManager();

}
