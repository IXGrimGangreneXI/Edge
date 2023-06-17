package org.asf.edge.common.account.impl.accounts;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.account.AccountDataContainer;
import org.asf.edge.common.account.AccountManager;
import org.asf.edge.common.account.AccountObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DatabaseAccountObject extends AccountObject {

	private String id;
	private String username;
	private Connection conn;

	private AccountManager manager;
	private Logger logger = LogManager.getLogger("AccountManager");
	private static SecureRandom rnd = new SecureRandom();

	public DatabaseAccountObject(String id, String username, Connection conn, AccountManager manager) {
		this.id = id;
		this.username = username;
		this.conn = conn;
		this.manager = manager;
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
	public long getLastLoginTime() {
		try {
			// Create prepared statement
			var statement = conn.prepareStatement("SELECT DATA FROM ACCOUNTWIDEPLAYERDATA WHERE PATH = ?");
			statement.setString(1, id + "//accountdata/lastlogintime");
			ResultSet res = statement.executeQuery();
			String data = res.getString("DATA");
			if (data == null)
				return -1;
			return JsonParser.parseString(data).getAsLong();
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to pull login time of ID '" + id + "'",
					e);
			return -1;
		}
	}

	@Override
	public long getRegistrationTimestamp() {
		try {
			// Create prepared statement
			var statement = conn.prepareStatement("SELECT DATA FROM ACCOUNTWIDEPLAYERDATA WHERE PATH = ?");
			statement.setString(1, id + "//accountdata/registrationtimestamp");
			ResultSet res = statement.executeQuery();
			String data = res.getString("DATA");
			if (data == null)
				return -1;
			return JsonParser.parseString(data).getAsLong();
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
			// Create prepared statement
			var statement = conn.prepareStatement("UPDATE USERMAP SET USERNAME = ? WHERE ID = ?");
			statement.setString(1, name);
			statement.setString(2, id);
			statement.execute();
			username = name;
			return true;
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to update username of ID '" + id + "'",
					e);
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
			var statement = conn.prepareStatement("UPDATE USERMAP SET CREDS = ? WHERE ID = ?");
			statement.setBytes(1, cred);
			statement.setString(2, id);
			statement.execute();
			return true;
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to update password of ID '" + id + "'",
					e);
			return false;
		}
	}

	@Override
	public boolean migrateToNormalAccountFromGuest(String newName, char[] password) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isGuestAccount() {
		try {
			// Create prepared statement
			var statement = conn.prepareStatement("SELECT DATA FROM ACCOUNTWIDEPLAYERDATA WHERE PATH = ?");
			statement.setString(1, id + "//accountdata/isguestaccount");
			ResultSet res = statement.executeQuery();
			String data = res.getString("DATA");
			if (data == null)
				return false;
			return JsonParser.parseString(data).getAsBoolean();
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
			// Create prepared statement
			var statement = conn.prepareStatement("SELECT DATA FROM ACCOUNTWIDEPLAYERDATA WHERE PATH = ?");
			statement.setString(1, id + "//accountdata/ismultiplayerenabled");
			ResultSet res = statement.executeQuery();
			String data = res.getString("DATA");
			if (data == null)
				return false;
			return JsonParser.parseString(data).getAsBoolean();
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to check multiplayer state of ID '" + id
					+ "'", e);
			return false;
		}
	}

	@Override
	public boolean isChatEnabled() {
		try {
			// Create prepared statement
			var statement = conn.prepareStatement("SELECT DATA FROM ACCOUNTWIDEPLAYERDATA WHERE PATH = ?");
			statement.setString(1, id + "//accountdata/ischatenabled");
			ResultSet res = statement.executeQuery();
			String data = res.getString("DATA");
			if (data == null)
				return false;
			return JsonParser.parseString(data).getAsBoolean();
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to check chat state of ID '" + id + "'",
					e);
			return false;
		}
	}

	@Override
	public boolean isStrictChatFilterEnabled() {
		try {
			// Create prepared statement
			var statement = conn.prepareStatement("SELECT DATA FROM ACCOUNTWIDEPLAYERDATA WHERE PATH = ?");
			statement.setString(1, id + "//accountdata/isstrictchatfilterenabled");
			ResultSet res = statement.executeQuery();
			String data = res.getString("DATA");
			if (data == null)
				return false;
			return JsonParser.parseString(data).getAsBoolean();
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to check chat filter state of ID '" + id
					+ "'", e);
			return false;
		}
	}

	@Override
	public void setMultiplayerEnabled(boolean state) {
		try {
			// Create prepared statement
			var statement = conn.prepareStatement("UPDATE ACCOUNTWIDEPLAYERDATA SET DATA = ? WHERE PATH = ?");
			statement.setString(1, state ? "true" : "false");
			statement.setString(2, id + "//accountdata/ismultiplayerenabled");
			statement.execute();
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to update multiplayer state of ID '"
					+ id + "'", e);
		}
	}

	@Override
	public void setChatEnabled(boolean state) {
		try {
			// Create prepared statement
			var statement = conn.prepareStatement("UPDATE ACCOUNTWIDEPLAYERDATA SET DATA = ? WHERE PATH = ?");
			statement.setString(1, state ? "true" : "false");
			statement.setString(2, id + "//accountdata/ischatenabled");
			statement.execute();
		} catch (SQLException e) {
			logger.error(
					"Failed to execute database query request while trying to update chat state of ID '" + id + "'", e);
		}
	}

	@Override
	public void setStrictChatFilterEnabled(boolean state) {
		try {
			// Create prepared statement
			var statement = conn.prepareStatement("UPDATE ACCOUNTWIDEPLAYERDATA SET DATA = ? WHERE PATH = ?");
			statement.setString(1, state ? "true" : "false");
			statement.setString(2, id + "//accountdata/isstrictchatfilterenabled");
			statement.execute();
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to update chat filter state of ID '"
					+ id + "'", e);
		}
	}

	@Override
	public void updateLastLoginTime() {
		try {
			// Create prepared statement
			var statement = conn.prepareStatement("UPDATE ACCOUNTWIDEPLAYERDATA SET DATA = ? WHERE PATH = ?");
			statement.setString(1, Long.toString(System.currentTimeMillis() / 1000));
			statement.setString(2, id + "//accountdata/lastlogintime");
			statement.execute();
		} catch (SQLException e) {
			logger.error(
					"Failed to execute database query request while trying to update login time of ID '" + id + "'", e);
		}
	}

	@Override
	public AccountDataContainer getAccountData() {
		return new DatabaseAccountDataContainer(id, conn);
	}

	@Override
	public void deleteAccount() throws IOException {
		// Delete data
		getAccountData().deleteContainer();

		// Delete account
		try {
			// Create prepared statement
			var statement = conn.prepareStatement("DELETE FROM USERMAP WHERE ID = ?");
			statement.setString(1, id);
			statement.execute();
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to delete account '" + id + "'", e);
			throw new IOException("SQL error", e);
		}

		// Delete save list
		try {
			// Pull list
			var statement = conn.prepareStatement("SELECT SAVES FROM SAVEMAP WHERE ACCID = ?");
			statement.setString(1, id);
			ResultSet res = statement.executeQuery();
			JsonArray saves = JsonParser.parseString(res.getString("SAVES")).getAsJsonArray();

			// Delete each save
			for (JsonElement saveEle : saves) {
				JsonObject saveObj = saveEle.getAsJsonObject();

				// Delete save
				new DatabaseSaveDataContainer(saveObj.get("id").getAsString(), conn).deleteContainer();
			}

			// Delete save list
			statement = conn.prepareStatement("DELETE FROM SAVEMAP WHERE ACCID = ?");
			statement.setString(1, id);
			statement.execute();
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

	public static byte[] getHash(byte[] salt, char[] password) {
		KeySpec spec = new PBEKeySpec(password, salt, 65536, 128);
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
			return factory.generateSecret(spec).getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			return null;
		}
	}

}
