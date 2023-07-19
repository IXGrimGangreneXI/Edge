package org.asf.edge.common.services.accounts.impl;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.UUID;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.common.CommonInit;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;

import com.google.gson.JsonPrimitive;

/**
 * 
 * Basic account manager implementation abstract that provides core necessities
 * for Edge account managers, use this abstract to more easily create a custom
 * account manager
 * 
 * @author Sky Swimmer
 *
 */
public abstract class BasicAccountManager extends AccountManager {

	protected Logger logger = LogManager.getLogger("AccountManager");
	private static SecureRandom rnd = new SecureRandom();

	private HashMap<String, AccountCache> cache = new HashMap<String, AccountCache>();

	private PublicKey publicKey;
	private PrivateKey privateKey;

	private class AccountCache {
		public long lastUpdate;
		public AccountObject account;
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
	 * Keeps accounts in memory
	 * 
	 * @param acc         Account to keep in memory
	 * @param addIfNeeded True to add if not in memory yet, false to only update if
	 *                    present (use false unless its the actual account owner,
	 *                    otherwise a memory leak exploit can occur because this
	 *                    loads accounts into memory, if offline they shouldnt be
	 *                    loaded into memory)
	 */
	public void keepInMemory(AccountObject acc, boolean addIfNeeded) {
		// Update in cache
		synchronized (cache) {
			if (cache.containsKey(acc.getAccountID())) {
				cache.get(acc.getAccountID()).lastUpdate = System.currentTimeMillis();
			} else if (addIfNeeded) {
				// Add to cache
				AccountCache c = new AccountCache();
				c.lastUpdate = System.currentTimeMillis();
				c.account = acc;
				cache.put(acc.getAccountID(), c);

				// Set player server ID
				try {
					AccountDataContainer cont = acc.getAccountData().getChildContainer("accountdata");
					if (!cont.entryExists("server_id")
							|| !cont.getEntry("server_id").getAsString().equals(CommonInit.getServerID())) {
						// Set
						cont.setEntry("server_id", new JsonPrimitive(CommonInit.getServerID()));
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	/**
	 * Called when the manager is loaded
	 */
	protected abstract void managerLoaded();

	/**
	 * Checks if a login name is in use
	 * 
	 * @param username Username to check
	 * @return True if in use, false otherwise
	 */
	protected abstract boolean isLoginNameInUse(String username);

	/**
	 * Checks if a viking name is in use
	 * 
	 * @param username Username to check
	 * @return True if in use, false otherwise
	 */
	protected abstract boolean isVikingNameInUse(String username);

	/**
	 * Retrieves account objects by ID
	 * 
	 * @param id    Account ID or guest ID (note that a guest ID is NOT a account
	 *              ID)
	 * @param guest True for guest account retrieval, false for normal account
	 *              retrieval
	 * @return AccountObject instance or null
	 */
	protected abstract AccountObject getAccountByID(String id, boolean guest);

	/**
	 * Retrieves password check data (48 bytes typically as of <i>pre-alpha v2
	 * rewrite</i>)
	 * 
	 * @param id Account ID
	 * @return Password check data or null if a error occured
	 */
	protected abstract byte[] getPasswordCheckData(String id);

	/**
	 * Called to register guest accounts
	 * 
	 * @param accID   Account ID that was allocated
	 * @param guestID Guest account ID
	 * @return BasicAccountObject instance or null
	 */
	public abstract BasicAccountObject registerGuest(String accID, String guestID);

	/**
	 * Called to register accounts
	 * 
	 * @param accID    Account ID that was allocated
	 * @param email    Account email
	 * @param username Account login name
	 * @param cred     Account credential bytes
	 * @return BasicAccountObject instance or null
	 */
	public abstract BasicAccountObject registerAccount(String accID, String email, String username, byte[] cred);

	@Override
	public boolean isUsernameTaken(String username) {
		// Check username
		if (isLoginNameInUse(username))
			return true;
		// Check viking name
		if (isVikingNameInUse(username))
			return true;
		return false; // Not in use
	}

	@Override
	public AccountObject getAccount(String id) {
		// Find
		while (true) {
			try {
				if (cache.containsKey(id))
					return cache.get(id).account;
				break;
			} catch (ConcurrentModificationException | NullPointerException e) {
			}
		}

		// Not found
		return getAccountByID(id, false);
	}

	@Override
	public AccountObject getGuestAccount(String guestID) {
		return getAccountByID(guestID, true);
	}

	@Override
	public void loadManager() {
		// Cache watchdog
		AsyncTaskManager.runAsync(() -> {
			while (true) {
				// Pull cache
				AccountCache[] cached;
				synchronized (cache) {
					cached = cache.values().toArray(t -> new AccountCache[t]);
				}

				// Go through cache
				for (AccountCache obj : cached) {
					if ((System.currentTimeMillis() - obj.lastUpdate) > (3 * 60 * 1000)) {
						// Expired, remove from memory as this player is no longer online
						cache.remove(obj.account.getAccountID());
					}
				}

				// Wait
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
				}
			}
		});

		// Call load
		managerLoaded();
	}

	@Override
	public boolean isValidUsername(String username) {
		if (username.replace(" ", "").equals("") || username.length() < 2 || username.length() > 100
				|| !username.matches("^[A-Za-z].*$") || !username.matches("^[A-Za-z0-9@._# ]+$"))
			return false;
		return true;
	}

	@Override
	public boolean isValidPassword(String password) {
		return password.replaceAll("[^A-Za-z0-9]", "").length() >= 6;
	}

	@Override
	public boolean verifyPassword(String id, String password) {
		// Retrieve password data
		byte[] passwordCheckData = getPasswordCheckData(id);
		if (passwordCheckData == null)
			return false;

		// Get salt and hash from data
		byte[] salt = Arrays.copyOfRange(passwordCheckData, 0, 32);
		byte[] hash = Arrays.copyOfRange(passwordCheckData, 32, 48);

		// Get current password
		byte[] current = getHash(salt, password.toCharArray());

		// Verify
		for (int i = 0; i < hash.length; i++) {
			if (hash[i] != current[i]) {
				return false;
			}
		}
		return true;
	}

	@Override
	public TokenParseResult verifyToken(String token) {
		// Init if needed
		keyInit();

		// Parse token
		try {
			// Verify signature
			String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
			String sig = token.split("\\.")[2];
			if (!SessionToken.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig), publicKey)) {
				return TokenParseResult.INVALID_DATA;
			}
			return TokenParseResult.SUCCESS; // Rest is handled by the token API
		} catch (Exception e) {
			return TokenParseResult.INVALID_DATA;
		}
	}

	@Override
	public byte[] signToken(String token) {
		// Init if needed
		keyInit();

		// Sign
		try {
			return SessionToken.sign(token.getBytes("UTF-8"), privateKey);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public AccountObject registerGuestAccount(String guestID) {
		// Check if guest ID exists
		if (getGuestAccount(guestID) != null)
			return null;

		// Generate account ID
		String id = UUID.randomUUID().toString();
		while (accountExists(id))
			id = UUID.randomUUID().toString();

		// Register account
		AccountObject obj = registerGuest(id, guestID);
		if (obj == null)
			return null;

		try {
			// Set account data
			AccountDataContainer settings = obj.getAccountData().getChildContainer("accountdata");
			settings.setEntry("lastlogintime", new JsonPrimitive(System.currentTimeMillis()));
			settings.setEntry("registrationtimestamp", new JsonPrimitive(System.currentTimeMillis()));
			settings.setEntry("isguestaccount", new JsonPrimitive(true));
			settings.setEntry("ismultiplayerenabled", new JsonPrimitive(true));
			settings.setEntry("ischatenabled", new JsonPrimitive(true));
			settings.setEntry("isstrictchatfilterenabled", new JsonPrimitive(false));
		} catch (IOException e) {
			logger.error("Failed to execute database query request while trying to update account settings of ID '" + id
					+ "'", e);
			return null;
		}

		// Return
		return obj;
	}

	@Override
	public AccountObject registerAccount(String username, String email, char[] password) {
		// Check username validity
		if (!isValidUsername(username))
			return null;

		// Check username
		if (isUsernameTaken(username))
			return null;

		// Check password
		if (!isValidPassword(new String(password)))
			return null;

		// Check email
		if (getAccountIDByEmail(email) != null)
			return null;

		// Generate account ID
		String id = UUID.randomUUID().toString();
		while (accountExists(id))
			id = UUID.randomUUID().toString();

		// Create salt and compute password
		byte[] salt = salt();
		byte[] hash = getHash(salt, password);
		byte[] cred = new byte[48];
		for (int i = 0; i < 32; i++)
			cred[i] = salt[i];
		for (int i = 32; i < 48; i++)
			cred[i] = hash[i - 32];

		// Insert information
		AccountObject obj = registerAccount(id, email, username, cred);
		if (obj == null)
			return null;

		try {
			// Set account data
			AccountDataContainer settings = obj.getAccountData().getChildContainer("accountdata");
			settings.setEntry("lastlogintime", new JsonPrimitive(System.currentTimeMillis()));
			settings.setEntry("registrationtimestamp", new JsonPrimitive(System.currentTimeMillis()));
			settings.setEntry("isguestaccount", new JsonPrimitive(false));
			settings.setEntry("ismultiplayerenabled", new JsonPrimitive(true));
			settings.setEntry("ischatenabled", new JsonPrimitive(true));
			settings.setEntry("isstrictchatfilterenabled", new JsonPrimitive(false));
		} catch (IOException e) {
			logger.error("Failed to execute database query request while trying to update account settings of ID '" + id
					+ "'", e);
			return null;
		}

		// Return
		return obj;
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

	private void keyInit() {
		if (publicKey == null || privateKey == null) {
			try {
				// Load or generate keys for JWT signatures
				File publicKey = new File("publickey.pem");
				File privateKey = new File("privatekey.pem");
				if (!publicKey.exists() || !privateKey.exists()) {
					// Generate new keys
					KeyPair pair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

					// Save keys
					Files.writeString(publicKey.toPath(),
							SessionToken.pemEncode(pair.getPublic().getEncoded(), "PUBLIC"));
					Files.writeString(privateKey.toPath(),
							SessionToken.pemEncode(pair.getPrivate().getEncoded(), "PRIVATE"));
				}
				// Load keys
				KeyFactory fac = KeyFactory.getInstance("RSA");
				this.privateKey = fac.generatePrivate(
						new PKCS8EncodedKeySpec(SessionToken.pemDecode(Files.readString(privateKey.toPath()))));
				this.publicKey = fac.generatePublic(
						new X509EncodedKeySpec(SessionToken.pemDecode(Files.readString(publicKey.toPath()))));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public String[] getOnlinePlayerIDs() {
		while (true) {
			try {
				return cache.keySet().toArray(t -> new String[t]);
			} catch (ConcurrentModificationException | NullPointerException e) {
			}
		}
	}

	public boolean isOnline(String accountID) {
		while (true) {
			try {
				return cache.containsKey(accountID);
			} catch (ConcurrentModificationException | NullPointerException e) {
			}
		}
	}

}
