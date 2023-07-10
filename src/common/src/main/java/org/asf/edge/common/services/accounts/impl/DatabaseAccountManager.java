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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Base64;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Properties;
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
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.accounts.impl.accounts.DatabaseAccountObject;
import org.asf.edge.common.tokens.SessionToken;
import org.asf.edge.common.tokens.TokenParseResult;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

public class DatabaseAccountManager extends AccountManager {

	private String url;
	private Properties props;
	private Logger logger = LogManager.getLogger("AccountManager");
	private static SecureRandom rnd = new SecureRandom();

	private PublicKey publicKey;
	private PrivateKey privateKey;

	private class AccountCache {
		public long lastUpdate;
		public AccountObject account;
	}

	private HashMap<String, AccountCache> cache = new HashMap<String, AccountCache>();

	@Override
	public void initService() {
	}

	public void keepInMemory(DatabaseAccountObject acc) {
		// Update in cache
		synchronized (cache) {
			if (cache.containsKey(acc.getAccountID())) {
				cache.get(acc.getAccountID()).lastUpdate = System.currentTimeMillis();
			} else {
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

		// Write/load config
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
		JsonObject databaseManagerConfig = new JsonObject();
		if (!accountManagerConfig.has("databaseManager")) {
			databaseManagerConfig.addProperty("url", "jdbc:mysql://localhost/edge");
			JsonObject props = new JsonObject();
			props.addProperty("user", "edge");
			props.addProperty("password", "edgesodserver");
			databaseManagerConfig.add("properties", props);
			accountManagerConfig.add("databaseManager", databaseManagerConfig);

			// Write config
			try {
				Files.writeString(configFile.toPath(),
						new Gson().newBuilder().setPrettyPrinting().create().toJson(accountManagerConfig));
			} catch (IOException e) {
				logger.error("Failed to write the account manager configuration!", e);
				return;
			}
		} else
			databaseManagerConfig = accountManagerConfig.get("databaseManager").getAsJsonObject();

		// Load url
		url = databaseManagerConfig.get("url").getAsString();

		// Load properties
		JsonObject properties = databaseManagerConfig.get("properties").getAsJsonObject();
		props = new Properties();
		for (String key : properties.keySet())
			props.setProperty(key, properties.get(key).getAsString());

		try {
			// Load drivers
			Class.forName("com.mysql.cj.jdbc.Driver");
			Class.forName("org.asf.edge.common.jdbc.LoggingProxyDriver");
			Class.forName("org.asf.edge.common.jdbc.LockingDriver");

			// Create tables
			Connection conn = DriverManager.getConnection(url, props);
			try {
				Statement statement = conn.createStatement();
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS EMAILMAP (EMAIL TEXT, ID CHAR(36))");
				statement.executeUpdate(
						"CREATE TABLE IF NOT EXISTS USERMAP (USERNAME TEXT, ID CHAR(36), CREDS BINARY(48))");
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS SAVEUSERNAMEMAP (USERNAME TEXT, ID CHAR(36))");
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS SAVEMAP (ACCID CHAR(36), SAVES LONGTEXT)");
				statement.executeUpdate(
						"CREATE TABLE IF NOT EXISTS ACCOUNTWIDEPLAYERDATA (PATH varchar(294), DATA LONGTEXT)");
				statement.executeUpdate(
						"CREATE TABLE IF NOT EXISTS SAVESPECIFICPLAYERDATA (PATH varchar(294), DATA LONGTEXT)");
			} finally {
				conn.close();
			}
		} catch (SQLException | ClassNotFoundException e) {
			logger.error("Failed to connect to database!", e);
			System.exit(1);
		}
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
	public boolean isUsernameTaken(String username) {
		try {
			// Create prepared statement
			Connection conn = DriverManager.getConnection(url, props);
			try {
				var statement = conn.prepareStatement("SELECT COUNT(ID) FROM USERMAP WHERE USERNAME = ?");
				statement.setString(1, username);
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return false;
				if (res.getInt(1) != 0)
					return true;
				try {
					// Create prepared statement
					statement = conn.prepareStatement("SELECT COUNT(ID) FROM SAVEUSERNAMEMAP WHERE USERNAME = ?");
					statement.setString(1, username);
					res = statement.executeQuery();
					if (!res.next())
						return false; // Not found
					return res.getInt(1) != 0;
				} catch (SQLException e) {
					logger.error("Failed to execute database query request while trying to check if username '"
							+ username + "' is taken", e);
					return false;
				}
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to check if username '" + username
					+ "' is taken", e);
			return false;
		}
	}

	@Override
	public String getAccountID(String username) {
		try {
			// Create prepared statement
			Connection conn = DriverManager.getConnection(url, props);
			try {
				var statement = conn.prepareStatement("SELECT ID FROM USERMAP WHERE USERNAME = ?");
				statement.setString(1, username);
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return null;
				return res.getString("ID");
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to pull user ID of username '"
					+ username + "'", e);
			return null;
		}
	}

	@Override
	public String getAccountIdBySaveUsername(String username) {
		try {
			// Create prepared statement
			Connection conn = DriverManager.getConnection(url, props);
			try {
				var statement = conn.prepareStatement("SELECT ID FROM SAVEUSERNAMEMAP WHERE USERNAME = ?");
				statement.setString(1, username);
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return null;
				String id = res.getString("ID");

				// Pull account ID
				statement = conn.prepareStatement("SELECT DATA FROM SAVESPECIFICPLAYERDATA WHERE PATH = ?");
				statement.setString(1, id + "//accountid");
				res = statement.executeQuery();
				if (!res.next())
					return null;
				String data = res.getString("DATA");
				if (data == null)
					return null;
				return JsonParser.parseString(data).getAsString();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to pull user ID of save username '"
					+ username + "'", e);
			return null;
		}
	}

	@Override
	public boolean verifyPassword(String id, String password) {
		try {
			// Create prepared statement
			Connection conn = DriverManager.getConnection(url, props);
			try {
				var statement = conn.prepareStatement("SELECT CREDS FROM USERMAP WHERE ID = ?");
				statement.setString(1, id);
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return false;
				byte[] data = res.getBytes("CREDS");

				// Check data
				if (data == null)
					return false;
				if (data.length != 48) {
					logger.error("Detected corrupted credentials for ID '" + id + "' during password verification.");
					return false;
				}

				// Get salt and hash from data
				byte[] salt = Arrays.copyOfRange(data, 0, 32);
				byte[] hash = Arrays.copyOfRange(data, 32, 48);

				// Get current password
				byte[] current = getHash(salt, password.toCharArray());

				// Verify
				for (int i = 0; i < hash.length; i++) {
					if (hash[i] != current[i]) {
						return false;
					}
				}
				return true;
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to verify password for ID '" + id + "'",
					e);
			return false;
		}
	}

	@Override
	public boolean accountExists(String id) {
		try {
			// Create prepared statement
			Connection conn = DriverManager.getConnection(url, props);
			try {
				var statement = conn.prepareStatement("SELECT COUNT(USERNAME) FROM USERMAP WHERE ID = ?");
				statement.setString(1, id);
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return false;
				return res.getInt(1) != 0;
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to check if ID '" + id + "' exists", e);
			return false;
		}
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

		try {
			// Create prepared statement
			Connection conn = DriverManager.getConnection(url, props);
			try {
				var statement = conn.prepareStatement("SELECT USERNAME FROM USERMAP WHERE ID = ?");
				statement.setString(1, id);
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return null;
				String username = res.getString("USERNAME");
				if (username == null)
					return null;
				AccountObject acc = new DatabaseAccountObject(id, username, url, props, this);
				return acc;
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error(
					"Failed to execute database query request while trying to pull account object of ID '" + id + "'",
					e);
			return null;
		}
	}

	@Override
	public AccountObject getGuestAccount(String guestID) {
		try {
			// Create prepared statement
			Connection conn = DriverManager.getConnection(url, props);
			try {
				var statement = conn.prepareStatement("SELECT ID FROM USERMAP WHERE USERNAME = ?");
				statement.setString(1, "g/" + guestID);
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return null;
				String id = res.getString("ID");
				if (id == null)
					return null;
				DatabaseAccountObject acc = new DatabaseAccountObject(id, "g/" + guestID, url, props, this);
				return acc;
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to pull account object of guest ID '"
					+ guestID + "'", e);
			return null;
		}
	}

	@Override
	public String getAccountIDByEmail(String email) {
		try {
			// Create prepared statement
			Connection conn = DriverManager.getConnection(url, props);
			try {
				var statement = conn.prepareStatement("SELECT ID FROM EMAILMAP WHERE EMAIL = ?");
				statement.setString(1, email);
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return null;
				return res.getString("ID");
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error(
					"Failed to execute database query request while trying to pull user ID of email '" + email + "'",
					e);
			return null;
		}
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
		DatabaseAccountObject obj;
		try {
			// Insert information
			Connection conn = DriverManager.getConnection(url, props);
			try {
				obj = new DatabaseAccountObject(id, "g/" + guestID, url, props, this);

				// Insert user
				var statement = conn.prepareStatement("INSERT INTO USERMAP VALUES(?, ?, ?)");
				statement.setString(1, "g/" + guestID);
				statement.setString(2, id);
				statement.setBytes(3, new byte[0]);
				statement.execute();

				// Insert save
				statement = conn.prepareStatement("INSERT INTO SAVEMAP VALUES(?, ?)");
				statement.setString(1, id);
				statement.setString(2, "[]");
				statement.execute();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to register account '" + id + "'", e);
			return null;
		}

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

		// Insert information
		DatabaseAccountObject obj;
		try {
			// Create salt and compute password
			byte[] salt = salt();
			byte[] hash = getHash(salt, password);
			byte[] cred = new byte[48];
			for (int i = 0; i < 32; i++)
				cred[i] = salt[i];
			for (int i = 32; i < 48; i++)
				cred[i] = hash[i - 32];

			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Register account
				obj = new DatabaseAccountObject(id, username, url, props, this);

				// Insert email
				var statement = conn.prepareStatement("INSERT INTO EMAILMAP VALUES(?, ?)");
				statement.setString(1, email);
				statement.setString(2, id);
				statement.execute();

				// Insert user
				statement = conn.prepareStatement("INSERT INTO USERMAP VALUES(?, ?, ?)");
				statement.setString(1, username);
				statement.setString(2, id);
				statement.setBytes(3, cred);
				statement.execute();

				// Insert save
				statement = conn.prepareStatement("INSERT INTO SAVEMAP VALUES(?, ?)");
				statement.setString(1, id);
				statement.setString(2, "[]");
				statement.execute();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to register account '" + id + "'", e);
			return null;
		}

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
	public AccountSaveContainer getSaveByID(String id) {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("SELECT DATA FROM SAVESPECIFICPLAYERDATA WHERE PATH = ?");
				statement.setString(1, id + "//accountid");
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return null;
				String data = res.getString("DATA");
				if (data == null)
					return null;

				// Retrieve account
				String accID = JsonParser.parseString(data).getAsString();
				AccountObject acc = getAccount(accID);
				if (acc == null)
					return null;

				// Retrieve save
				return acc.getSave(id);
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to retrieve save '" + id + "'", e);
			return null;
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
