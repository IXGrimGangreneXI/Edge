package org.asf.edge.common.services.commondata.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.commondata.impl.db.DatabaseCommonDataManager;
import org.asf.edge.common.services.commondata.impl.db.DatabaseRequest;
import org.postgresql.util.PGobject;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class PostgresDatabaseCommonDataManager extends DatabaseCommonDataManager {

	private String url;
	private Properties props;
	private Logger logger = LogManager.getLogger("CommonDataManager");
	private Connection conn;

	@Override
	public void initService() {
	}

	@Override
	public void loadManager() {
		// Write/load config
		File configFile = new File("commondata.json");
		JsonObject commonDataManagerConfig = new JsonObject();
		if (configFile.exists()) {
			try {
				commonDataManagerConfig = JsonParser.parseString(Files.readString(configFile.toPath()))
						.getAsJsonObject();
			} catch (JsonSyntaxException | IOException e) {
				logger.error("Failed to load common data manager configuration!", e);
				return;
			}
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
				Files.writeString(configFile.toPath(),
						new Gson().newBuilder().setPrettyPrinting().create().toJson(commonDataManagerConfig));
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
			conn = DriverManager.getConnection(url, props);
		} catch (SQLException | ClassNotFoundException e) {
			logger.error("Failed to connect to database!", e);
			System.exit(1);
		}
	}

	@Override
	protected void setupContainer(String rootNodeName) {
		// Create if needed
		try {
			// Create prepared statement
			Connection conn = DriverManager.getConnection(url, props);
			try {
				Statement statement = conn.createStatement();
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS CDC2_" + rootNodeName
						+ " (DATAKEY varchar(64), PARENT varchar(64), PARENTCONTAINER varchar(256), DATA JSONB)");
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to prepare data container '"
					+ rootNodeName + "'", e);
		}
	}

	@Override
	public DatabaseRequest createRequest() throws SQLException {
		return new DatabaseRequest() {

			@Override
			public void setDataObject(int i, String obj, PreparedStatement st) throws SQLException {
				PGobject o = new PGobject();
				o.setType("jsonb");
				o.setValue(obj);
				st.setObject(i, o);
			}

			@Override
			public PreparedStatement createPreparedStatement(String query) throws SQLException {
				return conn.prepareStatement(query);
			}

			@Override
			public void finish() throws SQLException {
			}
		};
	}

}
