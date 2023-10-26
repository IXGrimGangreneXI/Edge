package org.asf.edge.common.services.commondata.impl;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.commondata.CommonDataTableContainer;
import org.asf.edge.common.services.commondata.impl.db.DatabaseCommonDataManager;
import org.asf.edge.common.services.commondata.impl.db.DatabaseRequest;
import org.asf.edge.common.services.config.ConfigProviderService;
import org.asf.edge.common.services.tabledata.DataTable.DataTableLayout.EntryLayout;
import org.postgresql.util.PGobject;

import com.google.gson.JsonObject;

public class PostgresDatabaseCommonDataManager extends DatabaseCommonDataManager {

	private String url;
	private Properties props;
	private Logger logger = LogManager.getLogger("CommonDataManager");

	@Override
	public void initService() {
	}

	@Override
	public void loadManager() {
		// Write/load config
		JsonObject commonDataManagerConfig;
		try {
			commonDataManagerConfig = ConfigProviderService.getInstance().loadConfig("server", "commondata");
		} catch (IOException e) {
			logger.error("Failed to load common data manager configuration!", e);
			return;
		}
		if (commonDataManagerConfig == null) {
			commonDataManagerConfig = new JsonObject();
		}
		JsonObject databaseManagerConfig = new JsonObject();
		if (!commonDataManagerConfig.has("postgreSQL")) {
			databaseManagerConfig.addProperty("url", "jdbc:postgresql://localhost/edge");
			JsonObject props = new JsonObject();
			props.addProperty("user", "edge");
			props.addProperty("password", "edgesodserver");
			databaseManagerConfig.add("properties", props);
			commonDataManagerConfig.add("postgreSQL", databaseManagerConfig);

			// Write config
			try {
				ConfigProviderService.getInstance().saveConfig("server", "commondata", commonDataManagerConfig);
			} catch (IOException e) {
				logger.error("Failed to write the common data manager configuration!", e);
				return;
			}
		} else
			databaseManagerConfig = commonDataManagerConfig.get("postgreSQL").getAsJsonObject();

		// Load url
		url = databaseManagerConfig.get("url").getAsString();

		// Load properties
		JsonObject properties = databaseManagerConfig.get("properties").getAsJsonObject();
		props = new Properties();
		for (String key : properties.keySet())
			props.setProperty(key, properties.get(key).getAsString());

		try {
			// Load drivers
			Class.forName("org.postgresql.Driver");

			// Test connection
			DriverManager.getConnection(url, props).close();
		} catch (SQLException | ClassNotFoundException e) {
			logger.error("Failed to connect to database!", e);
			System.exit(1);
		}
	}

	@Override
	protected void setupKeyValueContainer(String rootNodeName) {
		// Create if needed
		try {
			// Create prepared statement
			Connection conn = DriverManager.getConnection(url, props);
			try {
				Statement statement = conn.createStatement();
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS CDC2_" + rootNodeName
						+ " (DATAKEY varchar(64), PARENT varchar(64), PARENTCONTAINER varchar(256), DATA JSONB)");
				statement.close();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to prepare data container '"
					+ rootNodeName + "'", e);
		}
	}

	@Override
	protected void setupDataTableContainer(String tableName, CommonDataTableContainer<?> cont) {
		// Compute data types
		HashMap<String, String> columnDataTypes = new LinkedHashMap<String, String>();
		for (EntryLayout layout : cont.getLayout().getColumns()) {
			switch (layout.columnType) {

			case BOOLEAN:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "BOOLEAN");
				break;

			case BYTE:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "SMALLINT");
				break;

			case BYTE_ARRAY:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "BYTEA");
				break;

			case CHAR:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "CHARACTER");
				break;

			case DOUBLE:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "DOUBLE PRECISION");
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
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "JSONB");
				break;

			case STRING:
				columnDataTypes.put(layout.columnType + "_" + layout.columnName.toUpperCase(), "TEXT");
				break;

			case NULL:
				// Not used
				break;

			}
		}

		// Create creation string
		boolean first = true;
		String createCmd = "CREATE TABLE IF NOT EXISTS CTC2_" + tableName + " (";
		for (String column : columnDataTypes.keySet()) {
			if (!first)
				createCmd += ", ";
			createCmd += column;
			createCmd += " ";
			createCmd += columnDataTypes.get(column);
			first = false;
		}
		createCmd += ")";

		// Create if needed
		try {
			// Create prepared statement
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Create table if needed
				Statement statement = conn.createStatement();
				statement.executeUpdate(createCmd);
				statement.close();

				// Retrieve current columns
				ArrayList<String> newColumns = new ArrayList<String>(columnDataTypes.keySet());
				var st = conn
						.prepareStatement("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'ctc2_"
								+ tableName.toLowerCase() + "'");
				ResultSet res = st.executeQuery();
				while (res.next()) {
					// Get name
					String name = res.getString("COLUMN_NAME");
					if (newColumns.contains(name.toUpperCase()))
						newColumns.remove(name.toUpperCase());
				}
				res.close();
				st.close();

				// Go over new columns
				for (String newColumn : newColumns) {
					// Create
					st = conn.prepareStatement("ALTER TABLE CTC2_" + tableName + " ADD " + newColumn + " "
							+ columnDataTypes.get(newColumn));
					st.execute();
				}
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to prepare data container '" + tableName
					+ "'", e);
		}
	}

	@Override
	public DatabaseRequest createRequest() throws SQLException {
		Connection conn = DriverManager.getConnection(url, props);
		return new DatabaseRequest() {

			@Override
			public void setDataObject(int i, String obj, PreparedStatement st) throws SQLException {
				PGobject o = new PGobject();
				o.setType("jsonb");
				if (obj == null)
					obj = "null";
				o.setValue(obj);
				st.setObject(i, o);
			}

			@Override
			public PreparedStatement createPreparedStatement(String query) throws SQLException {
				return conn.prepareStatement(query);
			}

			@Override
			public void finish() throws SQLException {
				conn.close();
			}
		};
	}

}
