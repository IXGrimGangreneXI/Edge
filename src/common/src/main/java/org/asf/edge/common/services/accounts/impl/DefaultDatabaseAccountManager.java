package org.asf.edge.common.services.accounts.impl;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Properties;

import org.asf.edge.common.services.accounts.AccountDataTableContainer;
import org.asf.edge.common.services.accounts.impl.accounts.db.DatabaseRequest;
import org.asf.edge.common.services.config.ConfigProviderService;
import org.asf.edge.common.services.tabledata.DataTable.DataTableLayout.EntryLayout;

import com.google.gson.JsonObject;

public class DefaultDatabaseAccountManager extends DatabaseAccountManager {

	private String url;
	private Properties props;
	private Connection conn;

	private ArrayList<String> preparedDataContainers = new ArrayList<String>();

	@Override
	protected void managerLoaded() {
		// Write/load config
		JsonObject accountManagerConfig;
		try {
			accountManagerConfig = ConfigProviderService.getInstance().loadConfig("server", "accountmanager");
		} catch (IOException e) {
			logger.error("Failed to load account manager configuration!", e);
			return;
		}
		if (accountManagerConfig == null) {
			accountManagerConfig = new JsonObject();
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
				ConfigProviderService.getInstance().saveConfig("server", "accountmanager", accountManagerConfig);
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
			Class.forName("org.mariadb.jdbc.Driver");
			Class.forName("org.asf.edge.common.jdbc.LoggingProxyDriver");
			Class.forName("org.asf.edge.common.jdbc.LockingDriver");

			// Create tables
			Connection conn = DriverManager.getConnection(url, props);
			if (url.startsWith("jdbc:sqlite:"))
				this.conn = conn;
			try {
				Statement statement = conn.createStatement();
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS EMAILMAP_V3 (EMAIL TEXT, ID CHAR(36))");
				statement.executeUpdate(
						"CREATE TABLE IF NOT EXISTS USERMAP_V3 (USERNAME TEXT, ID CHAR(36), CREDS BINARY(48))");
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS SAVEUSERNAMEMAP_V3 (USERNAME TEXT, ID CHAR(36))");
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS SAVEMAP_V2 (ACCID CHAR(36), SAVES LONGTEXT)");
				statement.close();
			} finally {
				if (!url.startsWith("jdbc:sqlite:"))
					conn.close();
			}
		} catch (SQLException | ClassNotFoundException e) {
			logger.error("Failed to connect to database!", e);
			System.exit(1);
		}
	}

	@Override
	public DatabaseRequest createRequest() throws SQLException {
		boolean nonSingle = false;
		Connection conn = this.conn;
		if (conn == null) {
			nonSingle = true;
			conn = DriverManager.getConnection(url, props);
		}
		Connection connF = conn;
		boolean nonSingleF = nonSingle;
		return new DatabaseRequest() {

			@Override
			public PreparedStatement prepareStatement(String query) throws SQLException {
				return connF.prepareStatement(query);
			}

			@Override
			public void close() throws SQLException {
				if (nonSingleF)
					connF.close();
			}
		};
	}

	@Override
	public void prepareAccountKvDataContainer(String rootNodeName) {
		// Prepare table name
		String table;
		rootNodeName = rootNodeName.toUpperCase();
		if (rootNodeName.equalsIgnoreCase("LEGACY"))
			table = "ACCOUNTWIDEPLAYERDATA_V2";
		else
			table = "UDC2_" + rootNodeName;

		// Check if the container needs to be inited
		synchronized (preparedDataContainers) {
			if (!preparedDataContainers.contains(table)) {
				preparedDataContainers.add(table);
			} else {
				// We dont need to init the container since its already done
				return;
			}
		}

		// Create if needed
		try {
			// Create prepared statement
			Connection conn = this.conn;
			boolean nonSingle = conn == null;
			if (nonSingle)
				conn = DriverManager.getConnection(url, props);
			try {
				Statement statement = conn.createStatement();
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + table
						+ " (ACCID CHAR(36), DATAKEY varchar(64), PARENT varchar(64), PARENTCONTAINER varchar(256), DATA LONGTEXT)");
				statement.close();
			} finally {
				if (nonSingle)
					conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to prepare data container '"
					+ rootNodeName + "'", e);
		}
	}

	@Override
	public void prepareSaveKvDataContainer(String rootNodeName) {
		// Compute table name
		String table;
		rootNodeName = rootNodeName.toUpperCase();
		if (rootNodeName.equalsIgnoreCase("LEGACY"))
			table = "SAVESPECIFICPLAYERDATA_V2";
		else
			table = "SDC2_" + rootNodeName;

		// Check if the container needs to be inited
		synchronized (preparedDataContainers) {
			if (!preparedDataContainers.contains(table)) {
				preparedDataContainers.add(table);
			} else {
				// We dont need to init the container since its already done
				return;
			}
		}

		// Create if needed
		try {
			// Create prepared statement
			Connection conn = this.conn;
			boolean nonSingle = conn == null;
			if (nonSingle)
				conn = DriverManager.getConnection(url, props);
			try {
				Statement statement = conn.createStatement();
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + table
						+ " (SVID CHAR(36), DATAKEY varchar(64), PARENT varchar(64), PARENTCONTAINER varchar(256), DATA LONGTEXT)");
				statement.close();
			} finally {
				if (nonSingle)
					conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to prepare data container '"
					+ rootNodeName + "'", e);
		}
	}

	@Override
	public void prepareAccountDataTableContainer(String tableName, AccountDataTableContainer<?> cont) {
		// Check if the container needs to be inited
		synchronized (preparedDataContainers) {
			if (!preparedDataContainers.contains("UTC2_" + tableName)) {
				preparedDataContainers.add("UTC2_" + tableName);
			} else {
				// We dont need to init the container since its already done
				return;
			}
		}

		// Compute data types
		HashMap<String, String> columnDataTypes = new LinkedHashMap<String, String>();
		for (EntryLayout layout : cont.getLayout().getColumns()) {
			switch (layout.columnType) {

			case DATE:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "DATE");
				break;

			case BOOLEAN:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "BOOLEAN");
				break;

			case BYTE:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "TINYINT");
				break;

			case BYTE_ARRAY:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "LONGBLOB");
				break;

			case CHAR:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "CHAR");
				break;

			case DOUBLE:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "DOUBLE");
				break;

			case FLOAT:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "FLOAT");
				break;

			case SHORT:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "SMALLINT");
				break;

			case INT:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "INT");
				break;

			case LONG:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "BIGINT");
				break;

			case OBJECT:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "LONGTEXT");
				break;

			case STRING:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "LONGTEXT");
				break;

			case NULL:
				// Not used
				break;

			}
		}

		// Create creation string
		String createCmd = "CREATE TABLE IF NOT EXISTS UTC2_" + tableName + " (CID CHAR(36)";
		for (String column : columnDataTypes.keySet()) {
			createCmd += ", ";
			createCmd += column;
			createCmd += " ";
			createCmd += columnDataTypes.get(column);
		}
		createCmd += ")";

		// Create if needed
		try {
			// Create prepared statement
			Connection conn = this.conn;
			boolean nonSingle = conn == null;
			if (nonSingle)
				conn = DriverManager.getConnection(url, props);
			try {
				// Create table if needed
				Statement statement = conn.createStatement();
				statement.executeUpdate(createCmd);
				statement.close();

				// Retrieve current columns
				ArrayList<String> newColumns = new ArrayList<String>(columnDataTypes.keySet());
				if (url.startsWith("jdbc:sqlite:")) {
					// Sqlite
					var st = conn.prepareStatement("PRAGMA table_info('UTC2_" + tableName + "')");
					ResultSet res = st.executeQuery();
					while (res.next()) {
						// Get name
						String name = res.getString("NAME");
						if (newColumns.contains(name.toUpperCase()))
							newColumns.remove(name.toUpperCase());
					}
					res.close();
					st.close();
				} else {
					// Non-sqlite
					var st = conn.prepareStatement(
							"SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'UTC2_" + tableName
									+ "'");
					ResultSet res = st.executeQuery();
					while (res.next()) {
						// Get name
						String name = res.getString("COLUMN_NAME");
						if (newColumns.contains(name.toUpperCase()))
							newColumns.remove(name.toUpperCase());
					}
					res.close();
					st.close();
				}

				// Go over new columns
				for (String newColumn : newColumns) {
					// Create
					var st = conn.prepareStatement("ALTER TABLE UTC2_" + tableName + " ADD " + newColumn + " "
							+ columnDataTypes.get(newColumn));
					st.execute();
				}
			} finally {
				if (nonSingle)
					conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to prepare data container '" + tableName
					+ "'", e);
		}
	}

	@Override
	public void prepareSaveDataTableContainer(String tableName, AccountDataTableContainer<?> cont) {
		// Check if the container needs to be inited
		synchronized (preparedDataContainers) {
			if (!preparedDataContainers.contains("STC2_" + tableName)) {
				preparedDataContainers.add("STC2_" + tableName);
			} else {
				// We dont need to init the container since its already done
				return;
			}
		}

		// Compute data types
		HashMap<String, String> columnDataTypes = new LinkedHashMap<String, String>();
		for (EntryLayout layout : cont.getLayout().getColumns()) {
			switch (layout.columnType) {

			case DATE:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "DATE");
				break;

			case BOOLEAN:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "BOOLEAN");
				break;

			case BYTE:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "TINYINT");
				break;

			case BYTE_ARRAY:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "LONGBLOB");
				break;

			case CHAR:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "CHAR");
				break;

			case DOUBLE:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "DOUBLE");
				break;

			case FLOAT:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "FLOAT");
				break;

			case SHORT:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "SMALLINT");
				break;

			case INT:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "INT");
				break;

			case LONG:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "BIGINT");
				break;

			case OBJECT:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "LONGTEXT");
				break;

			case STRING:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "LONGTEXT");
				break;

			case NULL:
				// Not used
				break;

			}
		}

		// Create creation string
		String createCmd = "CREATE TABLE IF NOT EXISTS STC2_" + tableName + " (CID CHAR(36)";
		for (String column : columnDataTypes.keySet()) {
			createCmd += ", ";
			createCmd += column;
			createCmd += " ";
			createCmd += columnDataTypes.get(column);
		}
		createCmd += ")";

		// Create if needed
		try {
			// Create prepared statement
			Connection conn = this.conn;
			boolean nonSingle = conn == null;
			if (nonSingle)
				conn = DriverManager.getConnection(url, props);
			try {
				// Create table if needed
				Statement statement = conn.createStatement();
				statement.executeUpdate(createCmd);
				statement.close();

				// Retrieve current columns
				ArrayList<String> newColumns = new ArrayList<String>(columnDataTypes.keySet());
				if (url.startsWith("jdbc:sqlite:")) {
					// Sqlite
					var st = conn.prepareStatement("PRAGMA table_info('STC2_" + tableName + "')");
					ResultSet res = st.executeQuery();
					while (res.next()) {
						// Get name
						String name = res.getString("NAME");
						if (newColumns.contains(name.toUpperCase()))
							newColumns.remove(name.toUpperCase());
					}
					res.close();
					st.close();
				} else {
					// Non-sqlite
					var st = conn.prepareStatement(
							"SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'STC2_" + tableName
									+ "'");
					ResultSet res = st.executeQuery();
					while (res.next()) {
						// Get name
						String name = res.getString("COLUMN_NAME");
						if (newColumns.contains(name.toUpperCase()))
							newColumns.remove(name.toUpperCase());
					}
					res.close();
					st.close();
				}

				// Go over new columns
				for (String newColumn : newColumns) {
					// Create
					var st = conn.prepareStatement("ALTER TABLE STC2_" + tableName + " ADD " + newColumn + " "
							+ columnDataTypes.get(newColumn));
					st.execute();
				}
			} finally {
				if (nonSingle)
					conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to prepare data container '" + tableName
					+ "'", e);
		}
	}

}
