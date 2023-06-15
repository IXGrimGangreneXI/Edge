package org.asf.edge.common.account.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.account.AccountManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class DatabaseAccountManager extends AccountManager {

	private Connection conn;
	private Logger logger = LogManager.getLogger("AccountManager");

	@Override
	public void initService() {
	}

	@Override
	public void loadManager() {
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
			databaseManagerConfig.addProperty("url", "jdbc:sqlite:account-data.db");
			JsonObject props = new JsonObject();
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
		String url = databaseManagerConfig.get("url").getAsString();

		// Load properties
		JsonObject properties = databaseManagerConfig.get("properties").getAsJsonObject();
		Properties props = new Properties();
		for (String key : properties.keySet())
			props.setProperty(key, properties.get(key).getAsString());

		try {
			// Load drivers
			Class.forName("com.mysql.cj.jdbc.Driver");

			// Connect
			conn = DriverManager.getConnection(url, props);

			// Create tables
			Statement statement = conn.createStatement();
			statement.executeUpdate(
					"CREATE TABLE IF NOT EXISTS ACCOUNTMAP (USERNAME TEXT, ID CHAR(36), CREDS BLOB(48))");
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS PLAYERDATA (PATH varchar(292), DATA LONGTEXT)");
		} catch (SQLException | ClassNotFoundException e) {
			logger.error("Failed to connect to database!", e);
			System.exit(1);
		}
	}

	@Override
	public boolean isValidUsername(String username) {
		if (username.replace(" ", "").equals("") || username.length() < 2 || username.length() > 100
				|| !username.matches("^[A-Za-z].*$") || !username.matches("^[A-Za-z0-9@._#]+$"))
			return false;
		return true;
	}

	@Override
	public boolean isValidPassword(String password) {
		return password.replaceAll("[^A-Za-z0-9]", "").length() >= 6;
	}

	@Override
	public boolean isUsernameTaken(String username) {
		try {
			// Create prepared statement
			var statement = conn.prepareStatement("SELECT COUNT(ID) FROM ACCOUNTMAP WHERE USERNAME = ?");
			statement.setString(1, username);
			ResultSet res = statement.executeQuery();
			return res.getInt(1) != 0;
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to check if username '" + username
					+ "' is taken", e);
			return false;
		}
	}

	@Override
	public String getAccountID(String username) {
		try {
			// Create prepared statement
			var statement = conn.prepareStatement("SELECT ID FROM ACCOUNTMAP WHERE USERNAME = ?");
			statement.setString(1, username);
			ResultSet res = statement.executeQuery();
			return res.getString("ID");
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to pull user ID of username '"
					+ username + "'", e);
			return null;
		}
	}

}
