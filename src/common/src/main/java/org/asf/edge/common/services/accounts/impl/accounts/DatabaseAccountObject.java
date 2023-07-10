package org.asf.edge.common.services.accounts.impl.accounts;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.accounts.impl.DatabaseAccountManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DatabaseAccountObject extends AccountObject {

	private String id;
	private String username;

	private String url;
	private Properties props;

	private DatabaseAccountManager manager;
	private Logger logger = LogManager.getLogger("AccountManager");
	private static SecureRandom rnd = new SecureRandom();

	public DatabaseAccountObject(String id, String username, String url, Properties props,
			DatabaseAccountManager manager) {
		this.id = id;
		this.username = username;
		this.url = url;
		this.props = props;
		this.manager = manager;
	}

	@Override
	public void ping() {
		manager.keepInMemory(this);
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
	public String getAccountEmail() {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("SELECT EMAIL FROM EMAILMAP WHERE ID = ?");
				statement.setString(1, id);
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return null;
				return res.getString("EMAIL");
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error(
					"Failed to execute database query request while trying to pull account email of ID '" + id + "'",
					e);
			return null;
		}
	}

	@Override
	public long getLastLoginTime() {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("SELECT DATA FROM ACCOUNTWIDEPLAYERDATA WHERE PATH = ?");
				statement.setString(1, id + "//accountdata/lastlogintime");
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return -1;
				String data = res.getString("DATA");
				if (data == null)
					return -1;
				return JsonParser.parseString(data).getAsLong();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to pull login time of ID '" + id + "'",
					e);
			return -1;
		}
	}

	@Override
	public long getRegistrationTimestamp() {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("SELECT DATA FROM ACCOUNTWIDEPLAYERDATA WHERE PATH = ?");
				statement.setString(1, id + "//accountdata/registrationtimestamp");
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return -1;
				String data = res.getString("DATA");
				if (data == null)
					return -1;
				return JsonParser.parseString(data).getAsLong();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error(
					"Failed to execute database query request while trying to pull creation date of ID '" + id + "'",
					e);
			return -1;
		}
	}

	@Override
	public boolean updateUsername(String name) {
		// Check validity
		if (!manager.isValidUsername(name))
			return false;

		// Check if its in use
		if (manager.isUsernameTaken(name))
			return false;

		// Check filters
		// FIXME: IMPLEMENT THIS

		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("UPDATE USERMAP SET USERNAME = ? WHERE ID = ?");
				statement.setString(1, name);
				statement.setString(2, id);
				statement.execute();
				username = name;
				return true;
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to update username of ID '" + id + "'",
					e);
			return false;
		}
	}

	@Override
	public boolean updateEmail(String email) {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("UPDATE EMAILMAP SET EMAIL = ? WHERE ID = ?");
				statement.setString(1, email);
				statement.setString(2, id);
				statement.execute();
				return true;
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to update email of ID '" + id + "'", e);
			return false;
		}
	}

	@Override
	public boolean updatePassword(char[] newPassword) {
		// Check validity
		if (!manager.isValidPassword(new String(newPassword)))
			return false;

		try {
			// Create salt
			byte[] salt = salt();
			byte[] hash = getHash(salt, newPassword);
			byte[] cred = new byte[48];
			for (int i = 0; i < 32; i++)
				cred[i] = salt[i];
			for (int i = 32; i < 48; i++)
				cred[i] = hash[i - 32];

			// Create prepared statement
			Connection conn = DriverManager.getConnection(url, props);
			try {
				var statement = conn.prepareStatement("UPDATE USERMAP SET CREDS = ? WHERE ID = ?");
				statement.setBytes(1, cred);
				statement.setString(2, id);
				statement.execute();
				return true;
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to update password of ID '" + id + "'",
					e);
			return false;
		}
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
		if (manager.isUsernameTaken(newName))
			return false;

		// Check password
		if (!manager.isValidPassword(new String(password)))
			return false;

		// Check email
		if (manager.getAccountIDByEmail(email) != null)
			return false;

		// Check filters
		// FIXME: IMPLEMENT THIS

		// Update username
		if (!updateUsername(newName))
			return false;

		// Update password
		if (!updatePassword(password))
			return false;

		// Insert information
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Insert email
				var statement = conn.prepareStatement("INSERT INTO EMAILMAP VALUES(?, ?)");
				statement.setString(1, email);
				statement.setString(2, id);
				statement.execute();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to migrate guest account with  ID '"
					+ id + "' to a normal account", e);
			return false;
		}

		// Disable guest mode
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("UPDATE ACCOUNTWIDEPLAYERDATA SET DATA = ? WHERE PATH = ?");
				statement.setString(1, "false");
				statement.setString(2, id + "//accountdata/isguestaccount");
				statement.execute();
				return true;
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to migrate guest account with  ID '"
					+ id + "' to a normal account", e);
			return false;
		}
	}

	@Override
	public boolean isGuestAccount() {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("SELECT DATA FROM ACCOUNTWIDEPLAYERDATA WHERE PATH = ?");
				statement.setString(1, id + "//accountdata/isguestaccount");
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return false;
				String data = res.getString("DATA");
				if (data == null)
					return false;
				return JsonParser.parseString(data).getAsBoolean();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error(
					"Failed to execute database query request while trying to check account type of ID '" + id + "'",
					e);
			return false;
		}
	}

	@Override
	public boolean isMultiplayerEnabled() {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("SELECT DATA FROM ACCOUNTWIDEPLAYERDATA WHERE PATH = ?");
				statement.setString(1, id + "//accountdata/ismultiplayerenabled");
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return false;
				String data = res.getString("DATA");
				if (data == null)
					return false;
				return JsonParser.parseString(data).getAsBoolean();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to check multiplayer state of ID '" + id
					+ "'", e);
			return false;
		}
	}

	@Override
	public boolean isChatEnabled() {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("SELECT DATA FROM ACCOUNTWIDEPLAYERDATA WHERE PATH = ?");
				statement.setString(1, id + "//accountdata/ischatenabled");
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return false;
				String data = res.getString("DATA");
				if (data == null)
					return false;
				return JsonParser.parseString(data).getAsBoolean();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to check chat state of ID '" + id + "'",
					e);
			return false;
		}
	}

	@Override
	public boolean isStrictChatFilterEnabled() {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("SELECT DATA FROM ACCOUNTWIDEPLAYERDATA WHERE PATH = ?");
				statement.setString(1, id + "//accountdata/isstrictchatfilterenabled");
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return false;
				String data = res.getString("DATA");
				if (data == null)
					return false;
				return JsonParser.parseString(data).getAsBoolean();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to check chat filter state of ID '" + id
					+ "'", e);
			return false;
		}
	}

	@Override
	public void setMultiplayerEnabled(boolean state) {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("UPDATE ACCOUNTWIDEPLAYERDATA SET DATA = ? WHERE PATH = ?");
				statement.setString(1, state ? "true" : "false");
				statement.setString(2, id + "//accountdata/ismultiplayerenabled");
				statement.execute();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to update multiplayer state of ID '"
					+ id + "'", e);
		}
	}

	@Override
	public void setChatEnabled(boolean state) {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("UPDATE ACCOUNTWIDEPLAYERDATA SET DATA = ? WHERE PATH = ?");
				statement.setString(1, state ? "true" : "false");
				statement.setString(2, id + "//accountdata/ischatenabled");
				statement.execute();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error(
					"Failed to execute database query request while trying to update chat state of ID '" + id + "'", e);
		}
	}

	@Override
	public void setStrictChatFilterEnabled(boolean state) {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("UPDATE ACCOUNTWIDEPLAYERDATA SET DATA = ? WHERE PATH = ?");
				statement.setString(1, state ? "true" : "false");
				statement.setString(2, id + "//accountdata/isstrictchatfilterenabled");
				statement.execute();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to update chat filter state of ID '"
					+ id + "'", e);
		}
	}

	@Override
	public void updateLastLoginTime() {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("UPDATE ACCOUNTWIDEPLAYERDATA SET DATA = ? WHERE PATH = ?");
				statement.setString(1, Long.toString(System.currentTimeMillis() / 1000));
				statement.setString(2, id + "//accountdata/lastlogintime");
				statement.execute();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error(
					"Failed to execute database query request while trying to update login time of ID '" + id + "'", e);
		}
	}

	private DatabaseAccountDataContainer accountData;

	@Override
	public AccountDataContainer getAccountData() {
		if (accountData == null)
			accountData = new DatabaseAccountDataContainer(this, id, url, props);
		return accountData;
	}

	@Override
	public void deleteAccount() throws IOException {
		// Delete data
		getAccountData().deleteContainer();

		// Delete account
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				try {
					// Create prepared statement
					var statement = conn.prepareStatement("DELETE FROM USERMAP WHERE ID = ?");
					statement.setString(1, id);
					statement.execute();
				} catch (SQLException e) {
					logger.error("Failed to execute database query request while trying to delete account '" + id + "'",
							e);
					throw new IOException("SQL error", e);
				}

				// Delete from email map
				try {
					// Create prepared statement
					var statement = conn.prepareStatement("DELETE FROM EMAILMAP WHERE ID = ?");
					statement.setString(1, id);
					statement.execute();
				} catch (SQLException e) {
					logger.error("Failed to execute database query request while trying to delete account '" + id + "'",
							e);
					throw new IOException("SQL error", e);
				}

				// Delete save list
				try {
					// Pull list
					var statement = conn.prepareStatement("SELECT SAVES FROM SAVEMAP WHERE ACCID = ?");
					statement.setString(1, id);
					ResultSet res = statement.executeQuery();
					if (!res.next())
						return;
					JsonArray saves = JsonParser.parseString(res.getString("SAVES")).getAsJsonArray();

					// Delete each save
					for (JsonElement saveEle : saves) {
						JsonObject saveObj = saveEle.getAsJsonObject();

						// Delete name lock
						statement = conn.prepareStatement("DELETE FROM SAVEUSERNAMEMAP WHERE ID = ?");
						statement.setString(1, saveObj.get("id").getAsString());
						statement.execute();

						// Delete save
						new DatabaseSaveDataContainer(this, getSave(saveObj.get("id").getAsString()), url, props)
								.deleteContainer();
					}

					// Delete save list
					statement = conn.prepareStatement("DELETE FROM SAVEMAP WHERE ACCID = ?");
					statement.setString(1, id);
					statement.execute();
				} catch (SQLException e) {
					logger.error("Failed to execute database query request while trying to delete account '" + id + "'",
							e);
					throw new IOException("SQL error", e);
				}
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to delete account '" + id + "'", e);
			throw new IOException("SQL error", e);
		}
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

	private String[] saveIDs;

	@Override
	public String[] getSaveIDs() {
		if (saveIDs != null)
			return saveIDs;
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Pull list
				var statement = conn.prepareStatement("SELECT SAVES FROM SAVEMAP WHERE ACCID = ?");
				statement.setString(1, id);
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return new String[0];
				JsonArray saves = JsonParser.parseString(res.getString("SAVES")).getAsJsonArray();
				String[] ids = new String[saves.size()];
				int i = 0;
				for (JsonElement ele : saves) {
					JsonObject saveObj = ele.getAsJsonObject();
					ids[i++] = saveObj.get("id").getAsString();
				}
				saveIDs = ids;
				return ids;
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to pull save list of ID '" + id + "'",
					e);
			return new String[0];
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
		// FIXME: IMPLEMENT THIS

		// Generate save ID
		String saveID = UUID.randomUUID().toString();
		while (true) {
			// Check if the ID isnt in use
			try {
				Connection conn = DriverManager.getConnection(url, props);
				try {
					// Create prepared statement
					var statement = conn.prepareStatement("SELECT COUNT(USERNAME) FROM SAVEUSERNAMEMAP WHERE ID = ?");
					statement.setString(1, saveID);
					ResultSet res = statement.executeQuery();
					if (!res.next())
						break;
					if (res.getInt(1) == 0)
						break; // Not taken
				} finally {
					conn.close();
				}
			} catch (SQLException e) {
				logger.error("Failed to execute database query request while trying to check if save ID '" + saveID
						+ "' is taken", e);
				return null;
			}
		}

		// Create save
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Pull list
				var statement = conn.prepareStatement("SELECT SAVES FROM SAVEMAP WHERE ACCID = ?");
				statement.setString(1, id);
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return null;
				JsonArray saves = JsonParser.parseString(res.getString("SAVES")).getAsJsonArray();
				for (JsonElement ele : saves) {
					JsonObject saveObj = ele.getAsJsonObject();
					if (saveObj.get("username").getAsString().equals(username))
						return null;
				}

				// Add object
				JsonObject saveObj = new JsonObject();
				saveObj.addProperty("id", saveID);
				saveObj.addProperty("username", username);
				saveObj.addProperty("creationTime", System.currentTimeMillis());
				saves.add(saveObj);

				// Write to db
				statement = conn.prepareStatement("UPDATE SAVEMAP SET SAVES = ? WHERE ACCID = ?");
				statement.setString(1, saves.toString());
				statement.setString(2, id);
				statement.execute();

				// Write username to db
				statement = conn.prepareStatement("INSERT INTO SAVEUSERNAMEMAP VALUES (?, ?)");
				statement.setString(1, username);
				statement.setString(2, saveID);
				statement.execute();

				// Return
				return new DatabaseSaveContainer(saveID, saveObj.get("creationTime").getAsLong(), username, this.id,
						url, props, manager, this);
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to create save '" + saveID
					+ "' for ID '" + id + "'", e);
			return null;
		}

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

			// Add
			try {
				JsonArray saves;
				Connection conn = DriverManager.getConnection(url, props);
				try {
					// Pull list
					var statement = conn.prepareStatement("SELECT SAVES FROM SAVEMAP WHERE ACCID = ?");
					statement.setString(1, this.id);
					ResultSet res = statement.executeQuery();
					if (!res.next())
						return null;
					saves = JsonParser.parseString(res.getString("SAVES")).getAsJsonArray();
				} finally {
					conn.close();
				}
				for (JsonElement ele : saves) {
					JsonObject saveObj = ele.getAsJsonObject();
					if (saveObj.get("id").getAsString().equals(id)) {
						// Found it
						String username = saveObj.get("username").getAsString();
						AccountSaveContainer save = new DatabaseSaveContainer(id,
								saveObj.get("creationTime").getAsLong(), username, this.id, url, props, manager, this);
						this.saves.put(id, save);
						return save;
					}
				}
				return null;
			} catch (SQLException e) {
				logger.error("Failed to execute database query request while trying to pull save '" + id + "' of ID '"
						+ this.id + "'", e);
				return null;
			}
		}
	}

	@Override
	public boolean isOnline() {
		return manager.isOnline(getAccountID());
	}

}
