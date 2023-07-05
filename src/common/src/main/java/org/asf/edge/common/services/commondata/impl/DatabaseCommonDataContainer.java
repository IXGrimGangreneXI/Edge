package org.asf.edge.common.services.commondata.impl;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.commondata.CommonDataContainer;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class DatabaseCommonDataContainer extends CommonDataContainer {

	private String url;
	private String table;
	private Properties props;
	private Logger logger = LogManager.getLogger("CommonDataManager");
	private static Object syncLock = new Object();

	public DatabaseCommonDataContainer(String url, Properties props, String table) {
		this.url = url;
		this.props = props;
		this.table = table;
	}

	@Override
	protected JsonElement get(String key) throws IOException {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("SELECT DATA FROM " + table + " WHERE PATH = ?");
				statement.setString(1, key);
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
			logger.error("Failed to execute database query request while trying to retrieve common data entry '" + key
					+ "' of table '" + table + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected void set(String key, JsonElement value) throws IOException {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("UPDATE " + table + " SET DATA = ? WHERE PATH = ?");
				statement.setString(1, value.toString());
				statement.setString(2, key);
				statement.execute();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to update common data entry '" + key
					+ "' of table '" + table + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected void create(String key, JsonElement value) throws IOException {
		try {
			synchronized (syncLock) {
				Connection conn = DriverManager.getConnection(url, props);
				try {
					// Create prepared statement
					var statement = conn.prepareStatement("INSERT INTO " + table + " VALUES(?, ?)");
					statement.setString(1, key);
					statement.setString(2, value.toString());
					statement.execute();
				} finally {
					conn.close();
				}
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to create common data entry '" + key
					+ "' in table '" + table + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected boolean exists(String key) throws IOException {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("SELECT DATA FROM " + table + " WHERE PATH = ?");
				statement.setString(1, key);
				ResultSet res = statement.executeQuery();
				return res.next();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to check common data entry '" + key
					+ "' in table '" + table + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected void delete(String key) throws IOException {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("DELETE FROM " + table + " WHERE PATH = ?");
				statement.setString(1, key);
				statement.execute();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to delete common data entry '" + key
					+ "' from table '" + table + "'", e);
			throw new IOException("SQL error", e);
		}
	}

}
