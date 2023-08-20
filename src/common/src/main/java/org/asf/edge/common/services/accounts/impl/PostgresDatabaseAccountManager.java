package org.asf.edge.common.services.accounts.impl;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.asf.edge.common.services.accounts.impl.accounts.db.DatabaseRequest;
import org.asf.edge.common.services.config.ConfigProviderService;
import org.postgresql.util.PGobject;

import com.google.gson.JsonObject;

public class PostgresDatabaseAccountManager extends DatabaseAccountManager {

	private String url;
	private Properties props;

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
		if (!accountManagerConfig.has("postgreSQL")) {
			databaseManagerConfig.addProperty("url", "jdbc:postgresql://localhost/edge");
			JsonObject props = new JsonObject();
			props.addProperty("user", "edge");
			props.addProperty("password", "edgesodserver");
			databaseManagerConfig.add("properties", props);
			accountManagerConfig.add("postgreSQL", databaseManagerConfig);

			// Write config
			try {
				ConfigProviderService.getInstance().saveConfig("server", "accountmanager", accountManagerConfig);
			} catch (IOException e) {
				logger.error("Failed to write the account manager configuration!", e);
				return;
			}
		} else
			databaseManagerConfig = accountManagerConfig.get("postgreSQL").getAsJsonObject();

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
			try {
				Statement statement = conn.createStatement();
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS EMAILMAP_V2 (EMAIL TEXT, ID CHAR(36))");
				statement.executeUpdate(
						"CREATE TABLE IF NOT EXISTS USERMAP_V2 (USERNAME TEXT, ID CHAR(36), CREDS bytea)");
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS SAVEUSERNAMEMAP_V2 (USERNAME TEXT, ID CHAR(36))");
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS SAVEMAP_V2 (ACCID CHAR(36), SAVES JSONB)");
				statement.executeUpdate(
						"CREATE TABLE IF NOT EXISTS ACCOUNTWIDEPLAYERDATA_V2 (ACCID CHAR(36), DATAKEY varchar(64), PARENT varchar(64), PARENTCONTAINER varchar(256), DATA JSONB)");
				statement.executeUpdate(
						"CREATE TABLE IF NOT EXISTS SAVESPECIFICPLAYERDATA_V2 (SVID CHAR(36), DATAKEY varchar(64), PARENT varchar(64), PARENTCONTAINER varchar(256), DATA JSONB)");
				statement.close();
			} finally {
				conn.close();
			}
		} catch (SQLException | ClassNotFoundException e) {
			logger.error("Failed to connect to database!", e);
			System.exit(1);
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
				o.setValue(obj);
				st.setObject(i, o);
			}

			@Override
			public PreparedStatement prepareStatement(String query) throws SQLException {
				return conn.prepareStatement(query);
			}

			@Override
			public void close() throws SQLException {
				conn.close();
			}
		};
	}

}
