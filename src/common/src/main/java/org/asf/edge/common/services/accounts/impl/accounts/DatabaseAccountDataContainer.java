package org.asf.edge.common.services.accounts.impl.accounts;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.accounts.AccountDataContainer;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class DatabaseAccountDataContainer extends AccountDataContainer {

	private String id;
	private String url;
	private Properties props;

	private Logger logger = LogManager.getLogger("AccountManager");

	public DatabaseAccountDataContainer(String id, String url, Properties props) {
		this.id = id;
		this.url = url;
		this.props = props;
	}

	@Override
	protected JsonElement get(String key) throws IOException {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("SELECT DATA FROM ACCOUNTWIDEPLAYERDATA WHERE PATH = ?");
				statement.setString(1, id + "//" + key);
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return null;
				String data = res.getString("DATA");
				if (data == null)
					return null;
				return JsonParser.parseString(data);
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to retrieve data entry '" + key
					+ "' for ID '" + id + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected void set(String key, JsonElement value) throws IOException {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("UPDATE ACCOUNTWIDEPLAYERDATA SET DATA = ? WHERE PATH = ?");
				statement.setString(1, value.toString());
				statement.setString(2, id + "//" + key);
				statement.execute();
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
				var statement = conn.prepareStatement("INSERT INTO ACCOUNTWIDEPLAYERDATA VALUES(?, ?)");
				statement.setString(1, id + "//" + key);
				statement.setString(2, value.toString());
				statement.execute();
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
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("SELECT COUNT(DATA) FROM ACCOUNTWIDEPLAYERDATA WHERE PATH = ?");
				statement.setString(1, id + "//" + key);
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return false;
				return res.getInt(1) != 0;
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
				var statement = conn.prepareStatement("DELETE FROM ACCOUNTWIDEPLAYERDATA WHERE PATH = ?");
				statement.setString(1, id + "//" + key);
				statement.execute();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to delete data entry '" + key
					+ "' for ID '" + id + "'", e);
			throw new IOException("SQL error", e);
		}
	}

}
