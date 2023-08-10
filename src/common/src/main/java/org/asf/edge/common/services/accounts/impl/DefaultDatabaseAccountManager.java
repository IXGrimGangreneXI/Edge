package org.asf.edge.common.services.accounts.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.asf.edge.common.services.accounts.impl.accounts.db.DatabaseRequest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class DefaultDatabaseAccountManager extends DatabaseAccountManager {

	private String url;
	private Properties props;
	private Connection conn;

	@Override
	protected void managerLoaded() {
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
			Class.forName("org.mariadb.jdbc.Driver");
			Class.forName("org.asf.edge.common.jdbc.LoggingProxyDriver");
			Class.forName("org.asf.edge.common.jdbc.LockingDriver");

			// Create tables
			Connection conn = DriverManager.getConnection(url, props);
			if (url.startsWith("jdbc:sqlite:"))
				this.conn = conn;
			try {
				Statement statement = conn.createStatement();
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS EMAILMAP_V2 (EMAIL TEXT, ID CHAR(36))");
				statement.executeUpdate(
						"CREATE TABLE IF NOT EXISTS USERMAP_V2 (USERNAME TEXT, ID CHAR(36), CREDS BINARY(48))");
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS SAVEUSERNAMEMAP_V2 (USERNAME TEXT, ID CHAR(36))");
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS SAVEMAP_V2 (ACCID CHAR(36), SAVES LONGTEXT)");
				statement.executeUpdate(
						"CREATE TABLE IF NOT EXISTS ACCOUNTWIDEPLAYERDATA_V2 (ACCID CHAR(36), DATAKEY varchar(64), PARENT varchar(64), PARENTCONTAINER varchar(256), DATA LONGTEXT)");
				statement.executeUpdate(
						"CREATE TABLE IF NOT EXISTS SAVESPECIFICPLAYERDATA_V2 (SVID CHAR(36), DATAKEY varchar(64), PARENT varchar(64), PARENTCONTAINER varchar(256), DATA LONGTEXT)");
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

}
