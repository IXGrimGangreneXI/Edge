package org.asf.edge.common.services.accounts.impl.accounts;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class DatabaseSaveDataContainer extends AccountDataContainer {

	private AccountSaveContainer save;
	private String id;

	private String url;
	private Properties props;

	private AccountObject account;
	private Logger logger = LogManager.getLogger("AccountManager");

	private HashMap<String, JsonElement> dataCache = new HashMap<String, JsonElement>();

	public DatabaseSaveDataContainer(AccountObject account, AccountSaveContainer save, String url, Properties props) {
		this.save = save;
		this.id = save.getSaveID();
		this.url = url;
		this.props = props;
		this.account = account;
	}

	@Override
	protected JsonElement get(String key) throws IOException {
		while (true) {
			try {
				if (dataCache.containsKey(key))
					return dataCache.get(key);
				break;
			} catch (ConcurrentModificationException e) {
			}
		}

		// Add if needed
		synchronized (dataCache) {
			if (dataCache.containsKey(key))
				return dataCache.get(key);

			try {
				Connection conn = DriverManager.getConnection(url, props);
				try {
					// Create prepared statement
					var statement = conn.prepareStatement("SELECT DATA FROM SAVESPECIFICPLAYERDATA WHERE PATH = ?");
					statement.setString(1, id + "//" + key);
					ResultSet res = statement.executeQuery();
					if (!res.next()) {
						dataCache.put(key, null);
						return null;
					}
					String data = res.getString("DATA");
					if (data == null) {
						dataCache.put(key, null);
						return null;
					}
					JsonElement ele = JsonParser.parseString(data);
					dataCache.put(key, ele);
					return ele;
				} finally {
					conn.close();
				}
			} catch (SQLException e) {
				logger.error("Failed to execute database query request while trying to retrieve data entry '" + key
						+ "' for ID '" + id + "'", e);
				throw new IOException("SQL error", e);
			}
		}
	}

	@Override
	protected void set(String key, JsonElement value) throws IOException {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("UPDATE SAVESPECIFICPLAYERDATA SET DATA = ? WHERE PATH = ?");
				statement.setString(1, value.toString());
				statement.setString(2, id + "//" + key);
				statement.execute();
				dataCache.put(key, value);
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to update data entry '" + key
					+ "' for ID '" + id + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected void create(String key, JsonElement value) throws IOException {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("INSERT INTO SAVESPECIFICPLAYERDATA VALUES(?, ?)");
				statement.setString(1, id + "//" + key);
				statement.setString(2, value.toString());
				statement.execute();
				dataCache.put(key, value);
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to create data entry '" + key
					+ "' for ID '" + id + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected boolean exists(String key) throws IOException {
		while (true) {
			try {
				if (dataCache.containsKey(key))
					return dataCache.get(key) != null;
				break;
			} catch (ConcurrentModificationException e) {
			}
		}

		// Check
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("SELECT DATA FROM SAVESPECIFICPLAYERDATA WHERE PATH = ?");
				statement.setString(1, id + "//" + key);
				ResultSet res = statement.executeQuery();
				if (!res.next()) {
					dataCache.put(key, null);
					return false;
				}
				String data = res.getString("DATA");
				if (data == null) {
					dataCache.put(key, null);
					return true;
				}
				JsonElement ele = JsonParser.parseString(data);
				dataCache.put(key, ele);
				return true;
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to check data entry '" + key
					+ "' for ID '" + id + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected void delete(String key) throws IOException {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("DELETE FROM SAVESPECIFICPLAYERDATA WHERE PATH = ?");
				statement.setString(1, id + "//" + key);
				statement.execute();
				dataCache.remove(key);
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to delete data entry '" + key
					+ "' for ID '" + id + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	public AccountObject getAccount() {
		return account;
	}

	@Override
	public AccountSaveContainer getSave() {
		return save;
	}

}
