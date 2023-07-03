package org.asf.edge.common.services.accounts.impl.accounts;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class DatabaseSaveContainer extends AccountSaveContainer {

	private String saveID;
	private long time;
	private String username;
	private String id;

	private String url;
	private Properties props;

	private AccountManager manager;
	private Logger logger = LogManager.getLogger("AccountManager");
	private AccountObject acc;

	public DatabaseSaveContainer(String saveID, long time, String username, String id, String url, Properties props,
			AccountManager manager, AccountObject acc) {
		this.saveID = saveID;
		this.time = time;
		this.username = username;
		this.id = id;
		this.url = url;
		this.props = props;
		this.manager = manager;
		this.acc = acc;

		AccountDataContainer sv = this.getSaveData();
		try {
			if (!sv.entryExists("accountid")) {
				sv.setEntry("accountid", new JsonPrimitive(acc.getAccountID()));
			}
		} catch (IOException e) {
		}
	}

	@Override
	public long getCreationTime() {
		return time;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getSaveID() {
		return saveID;
	}

	@Override
	public boolean updateUsername(String name) {
		if (name.equalsIgnoreCase(username))
			return true;

		// Check validity
		if (!manager.isValidUsername(name))
			return false;

		// Check if its in use
		if (!acc.getUsername().equalsIgnoreCase(name) && manager.isUsernameTaken(name)) {
			return false;
		} else {
			// Check if in use by any saves
			AccountObject accF = acc;
			if (Stream.of(acc.getSaveIDs()).map(t -> accF.getSave(t)).anyMatch(t -> {
				try {
					return t.getUsername().equalsIgnoreCase(name) && t.getSaveData().entryExists("avatar");
				} catch (IOException e) {
					return false;
				}
			})) {
				return false;
			}
		}

		// Check filters
		// FIXME: IMPLEMENT THIS

		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Update save info
				var statement = conn.prepareStatement("SELECT SAVES FROM SAVEMAP WHERE ACCID = ?");
				statement.setString(1, id);
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return false;
				JsonArray saves = JsonParser.parseString(res.getString("SAVES")).getAsJsonArray();
				for (JsonElement ele : saves) {
					JsonObject saveObj = ele.getAsJsonObject();
					if (saveObj.get("id").getAsString().equals(saveID)) {
						// Update
						saveObj.remove("username");
						saveObj.addProperty("username", name);

						// Save to db
						statement = conn.prepareStatement("UPDATE SAVEMAP SET SAVES = ? WHERE ACCID = ?");
						statement.setString(1, saves.toString());
						statement.setString(2, id);
						statement.execute();
						break;
					}
				}

				// Update user save map
				statement = conn.prepareStatement("UPDATE SAVEUSERNAMEMAP SET USERNAME = ? WHERE ID = ?");
				statement.setString(1, name);
				statement.setString(2, saveID);
				statement.execute();
				username = name;
				return true;
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to update username of save '" + saveID
					+ "' for ID '" + id + "'", e);
			return false;
		}
	}

	@Override
	public AccountDataContainer getSaveData() {
		return new DatabaseSaveDataContainer(saveID, url, props);
	}

	@Override
	public void deleteSave() {
		try {
			Connection conn = DriverManager.getConnection(url, props);
			try {
				// Delete name lock
				var statement = conn.prepareStatement("DELETE FROM SAVEUSERNAMEMAP WHERE ID = ?");
				statement.setString(1, saveID);
				statement.execute();

				// Delete save data
				getSaveData().deleteContainer();

				// Delete from save list
				statement = conn.prepareStatement("SELECT SAVES FROM SAVEMAP WHERE ACCID = ?");
				statement.setString(1, id);
				ResultSet res = statement.executeQuery();
				if (!res.next())
					return;
				JsonArray saves = JsonParser.parseString(res.getString("SAVES")).getAsJsonArray();
				for (JsonElement ele : saves) {
					JsonObject saveObj = ele.getAsJsonObject();
					if (saveObj.get("id").getAsString().equals(saveID)) {
						// Remove from list
						saves.remove(saveObj);

						// Save to db
						statement = conn.prepareStatement("UPDATE SAVEMAP SET SAVES = ? WHERE ACCID = ?");
						statement.setString(1, saves.toString());
						statement.setString(2, id);
						statement.execute();
						break;
					}
				}
			} finally {
				conn.close();
			}
		} catch (SQLException | IOException e) {
			logger.error("Failed to execute database query request while trying to delete save '" + saveID + "' of ID '"
					+ id + "'", e);
		}
	}

	@Override
	public AccountObject getAccount() {
		return acc;
	}

}
