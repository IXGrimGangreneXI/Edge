package org.asf.edge.common.services.accounts.impl;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Stream;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.textfilter.TextFilterService;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * 
 * Basic account object abstract - pre-defines crucial methods for Edge servers
 * 
 * @author Sky Swimmer
 *
 */
public abstract class BasicAccountObject extends AccountObject {

	private String id;
	protected String username;

	private Logger logger = LogManager.getLogger("AccountManager");
	private static SecureRandom rnd = new SecureRandom();

	private BasicAccountManager manager;
	private AccountDataContainer accountData;

	public BasicAccountObject(String id, String username, BasicAccountManager manager) {
		this.id = id;
		this.username = username;
		this.manager = manager;
	}

	/**
	 * Retrieves the account manager logger
	 * 
	 * @return Logger instance
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Retrieves the account manager
	 * 
	 * @return BasicAccountManager instance
	 */
	protected BasicAccountManager getManager() {
		return manager;
	}

	/**
	 * Called to update account usernames
	 * 
	 * @param name New account username
	 * @return True if successful, false otherwise
	 */
	protected abstract boolean performUpdateUsername(String name);

	/**
	 * Called to update account passwords
	 * 
	 * @param cred New password bytes (48 bytes typically as of <i>pre-alpha v2
	 *             rewrite</i>)
	 * @return True if successful, false otherwise
	 */
	protected abstract boolean performUpdatePassword(byte[] cred);

	/**
	 * Called to retrieve the account data container
	 * 
	 * @return AccountDataContainer instance
	 */
	public abstract AccountDataContainer retrieveAccountData();

	/**
	 * Called to retrieve save IDs
	 * 
	 * @return Array of save ID strings
	 */
	protected abstract String[] retrieveSaveIDs();

	/**
	 * Called to create saves
	 * 
	 * @param saveID   Save ID
	 * @param username Save username
	 * @return AccountSaveContainer instance or null if errored
	 */
	public abstract BasicAccountSaveContainer performCreateSave(String saveID, String username);

	/**
	 * Called to find saves
	 * 
	 * @param saveID Save ID
	 * @return AccountSaveContainer instance or null
	 */
	public abstract BasicAccountSaveContainer findSave(String saveID);

	@Override
	public void ping(boolean addIfNeeded) {
		manager.keepInMemory(this, addIfNeeded);
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getAccountID() {
		return id;
	}

	@Override
	public boolean isOnline() {
		return manager.isOnline(getAccountID());
	}

	@Override
	public long getLastLoginTime() {
		try {
			JsonElement ele = getAccountData().getChildContainer("accountdata").getEntry("lastlogintime");
			if (ele != null)
				return ele.getAsLong();
			return -1;
		} catch (IOException e) {
			return -1;
		}
	}

	@Override
	public long getRegistrationTimestamp() {
		try {
			JsonElement ele = getAccountData().getChildContainer("accountdata").getEntry("registrationtimestamp");
			if (ele != null)
				return ele.getAsLong();
			return -1;
		} catch (IOException e) {
			return -1;
		}
	}

	@Override
	public boolean isGuestAccount() {
		try {
			JsonElement ele = getAccountData().getChildContainer("accountdata").getEntry("isguestaccount");
			if (ele == null)
				return false;
			return ele.getAsBoolean();
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public boolean isMultiplayerEnabled() {
		try {
			JsonElement ele = getAccountData().getChildContainer("accountdata").getEntry("ismultiplayerenabled");
			if (ele == null)
				return false;
			return ele.getAsBoolean();
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public boolean isChatEnabled() {
		// Check guest
		if (isGuestAccount())
			return false; // Guests cannot chat

		try {
			JsonElement ele = getAccountData().getChildContainer("accountdata").getEntry("ischatenabled");
			if (ele == null)
				return false;
			return ele.getAsBoolean();
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public boolean isStrictChatFilterEnabled() {
		try {
			JsonElement ele = getAccountData().getChildContainer("accountdata").getEntry("isstrictchatfilterenabled");
			if (ele == null)
				return false;
			return ele.getAsBoolean();
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public boolean updateUsername(String name) {
		// Check validity
		if (!manager.isValidUsername(name))
			return false;

		// Non-migration checks
		if (!isGuestAccount()
				|| !Stream.of(getSaveIDs()).anyMatch(t -> getSave(t).getUsername().equalsIgnoreCase(name))) {
			// Check if its in use
			if (manager.isUsernameTaken(name))
				return false;
		}

		// Check filters
		if (TextFilterService.getInstance().isFiltered(name, true))
			return false;

		// Actually update
		boolean res = performUpdateUsername(name);
		if (res)
			username = name;
		return res;
	}

	@Override
	public boolean updatePassword(char[] newPassword) {
		// Check validity
		if (!manager.isValidPassword(new String(newPassword)))
			return false;

		// Create salt
		byte[] salt = salt();
		byte[] hash = getHash(salt, newPassword);
		byte[] cred = new byte[48];
		for (int i = 0; i < 32; i++)
			cred[i] = salt[i];
		for (int i = 32; i < 48; i++)
			cred[i] = hash[i - 32];

		// Call update
		return performUpdatePassword(cred);
	}

	@Override
	public AccountDataContainer getAccountData() {
		if (accountData == null)
			accountData = retrieveAccountData();
		return accountData;
	}

	@Override
	public boolean migrateToNormalAccountFromGuest(String newName, String email, char[] password) {
		// Check guest
		if (!isGuestAccount())
			return false;

		// Check username validity
		if (!manager.isValidUsername(newName))
			return false;

		// Check username
		if (!Stream.of(getSaveIDs()).anyMatch(t -> getSave(t).getUsername().equalsIgnoreCase(newName))
				&& manager.isUsernameTaken(newName))
			return false;

		// Check password
		if (!manager.isValidPassword(new String(password)))
			return false;

		// Check email
		if (manager.getAccountIDByEmail(email) != null)
			return false;

		// Check filters
		if (TextFilterService.getInstance().isFiltered(newName, true))
			return false;

		// Update username
		if (!updateUsername(newName))
			return false;

		// Update password
		if (!updatePassword(password))
			return false;

		// Insert information
		if (!updateEmail(email))
			return false;

		// Disable guest mode
		try {
			getAccountData().getChildContainer("accountdata").setEntry("isguestaccount", new JsonPrimitive(false));
			return true;
		} catch (IOException e) {
			logger.error("Failed to execute database query request while trying to migrate guest account with  ID '"
					+ id + "' to a normal account", e);
			return false;
		}
	}

	@Override
	public void setMultiplayerEnabled(boolean state) {
		try {
			getAccountData().getChildContainer("accountdata").setEntry("ismultiplayerenabled",
					new JsonPrimitive(state));
		} catch (IOException e) {
			logger.error("Failed to execute database query request while trying to update multiplayer state of ID '"
					+ id + "'", e);
		}
	}

	@Override
	public void setChatEnabled(boolean state) {
		try {
			getAccountData().getChildContainer("accountdata").setEntry("ischatenabled", new JsonPrimitive(state));
		} catch (IOException e) {
			logger.error(
					"Failed to execute database query request while trying to update chat state of ID '" + id + "'", e);
		}
	}

	@Override
	public void setStrictChatFilterEnabled(boolean state) {
		try {
			getAccountData().getChildContainer("accountdata").setEntry("isstrictchatfilterenabled",
					new JsonPrimitive(state));
		} catch (IOException e) {
			logger.error("Failed to execute database query request while trying to update chat filter state of ID '"
					+ id + "'", e);
		}
	}

	@Override
	public void updateLastLoginTime() {
		try {
			getAccountData().getChildContainer("accountdata").setEntry("lastlogintime",
					new JsonPrimitive((System.currentTimeMillis() / 1000l)));
		} catch (IOException e) {
			logger.error(
					"Failed to execute database query request while trying to update login time of ID '" + id + "'", e);
		}
	}

	private Object saveCreateLock = new Object();
	private String[] saveIDs;

	@Override
	public String[] getSaveIDs() {
		if (saveIDs != null)
			return saveIDs;
		synchronized (saveCreateLock) {
			if (saveIDs != null)
				return saveIDs;
			String[] ids = retrieveSaveIDs();
			saveIDs = ids;
			return ids;
		}
	}

	@Override
	public AccountSaveContainer createSave(String username) {
		// Check username
		if (!manager.isValidUsername(username))
			return null;

		// Check if taken
		if (!username.equalsIgnoreCase(this.username) && manager.isUsernameTaken(username))
			return null;

		// Check filters
		if (TextFilterService.getInstance().isFiltered(username, true))
			return null;

		// Generate save ID
		String saveID = UUID.randomUUID().toString();
		while (true) {
			String svID = saveID;
			if (Stream.of(getSaveIDs()).anyMatch(t -> t.equals(svID)))
				saveID = UUID.randomUUID().toString();
			else
				break;
		}

		// Create save
		AccountSaveContainer sv = performCreateSave(saveID, username);
		if (sv != null) {
			// Add to save list
			synchronized (saveCreateLock) {
				if (saveIDs == null) {
					String[] ids = retrieveSaveIDs();
					saveIDs = ids;
				}
				String[] newIds = new String[saveIDs.length + 1];
				for (int i = 0; i < newIds.length; i++)
					if (i == saveIDs.length)
						newIds[i] = sv.getSaveID();
					else
						newIds[i] = saveIDs[i];
				saveIDs = newIds;
			}
		}
		return sv;
	}

	private HashMap<String, AccountSaveContainer> saves = new HashMap<String, AccountSaveContainer>();

	@Override
	public AccountSaveContainer getSave(String id) {
		// Try to retrieve
		while (true) {
			try {
				if (saves.containsKey(id))
					return saves.get(id);
				break;
			} catch (ConcurrentModificationException e) {
			}
		}

		// Add if needed
		synchronized (saves) {
			if (saves.containsKey(id))
				return saves.get(id); // Some other thread already added it

			// Find and add
			AccountSaveContainer sv = findSave(id);
			if (sv != null)
				saves.put(id, sv);
			return sv;
		}
	}

	public void refreshSaveList() {
		saveIDs = null;
		getSaveIDs();
	}

	// Salt and hash
	private static byte[] salt() {
		byte[] salt = new byte[32];
		rnd.nextBytes(salt);
		return salt;
	}

	private static byte[] getHash(byte[] salt, char[] password) {
		KeySpec spec = new PBEKeySpec(password, salt, 65536, 128);
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
			return factory.generateSecret(spec).getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			return null;
		}
	}

}
