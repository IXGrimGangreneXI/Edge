package org.asf.edge.common.account.impl.accounts;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.account.AccountDataContainer;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class DatabaseSaveDataContainer extends AccountDataContainer {

	private String id;
	private Connection conn;

	private Logger logger = LogManager.getLogger("AccountManager");

	public DatabaseSaveDataContainer(String id, Connection conn) {
		this.id = id;
		this.conn = conn;
	}

	@Override
	protected JsonElement get(String key) throws IOException {
		try {
			// Create prepared statement
			var statement = conn.prepareStatement("SELECT DATA FROM SAVESPECIFICPLAYERDATA WHERE PATH = ?");
			statement.setString(1, id + "//" + key);
			ResultSet res = statement.executeQuery();
			String data = res.getString("DATA");
			if (data == null)
				return null;
			return JsonParser.parseString(data);
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to retrieve data entry '" + key
					+ "' for ID '" + id + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected void set(String key, JsonElement value) throws IOException {
		try {
			// Create prepared statement
			var statement = conn.prepareStatement("UPDATE SAVESPECIFICPLAYERDATA SET DATA = ? WHERE PATH = ?");
			statement.setString(1, value.toString());
			statement.setString(2, id + "//" + key);
			statement.execute();
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to update data entry '" + key
					+ "' for ID '" + id + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected void create(String key, JsonElement value) throws IOException {
		try {
			// Create prepared statement
			var statement = conn.prepareStatement("INSERT INTO SAVESPECIFICPLAYERDATA VALUES(?, ?)");
			statement.setString(1, id + "//" + key);
			statement.setString(2, value.toString());
			statement.execute();
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to create data entry '" + key
					+ "' for ID '" + id + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected boolean exists(String key) throws IOException {
		try {
			// Create prepared statement
			var statement = conn.prepareStatement("SELECT COUNT(DATA) FROM SAVESPECIFICPLAYERDATA WHERE PATH = ?");
			statement.setString(1, id + "//" + key);
			ResultSet res = statement.executeQuery();
			return res.getInt(1) != 0;
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to check data entry '" + key
					+ "' for ID '" + id + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected void delete(String key) throws IOException {
		try {
			// Create prepared statement
			var statement = conn.prepareStatement("DELETE FROM SAVESPECIFICPLAYERDATA WHERE PATH = ?");
			statement.setString(1, id + "//" + key);
			statement.execute();
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to delete data entry '" + key
					+ "' for ID '" + id + "'", e);
			throw new IOException("SQL error", e);
		}
	}

}
