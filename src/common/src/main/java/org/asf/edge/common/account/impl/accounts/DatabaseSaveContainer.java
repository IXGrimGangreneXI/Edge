package org.asf.edge.common.account.impl.accounts;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.account.AccountDataContainer;
import org.asf.edge.common.account.AccountManager;
import org.asf.edge.common.account.AccountObject;
import org.asf.edge.common.account.AccountSaveContainer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DatabaseSaveContainer extends AccountSaveContainer {

	private String saveID;
	private long time;
	private String username;
	private String id;

	private Connection conn;
	private AccountManager manager;
	private Logger logger = LogManager.getLogger("AccountManager");
	private AccountObject acc;

	public DatabaseSaveContainer(String saveID, long time, String username, String id, Connection conn,
			AccountManager manager, AccountObject acc) {
		this.saveID = saveID;
		this.time = time;
		this.username = username;
		this.id = id;
		this.conn = conn;
		this.manager = manager;
		this.acc = acc;
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
		// Check validity
		if (!manager.isValidUsername(name))
			return false;

		// Check if its in use
		if (manager.isUsernameTaken(name))
			return false;

		// Check filters
		// FIXME: IMPLEMENT THIS

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
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to update username of save '" + saveID
					+ "' for ID '" + id + "'", e);
			return false;
		}
	}

	@Override
	public AccountDataContainer getSaveData() {
		return new DatabaseSaveDataContainer(saveID, conn);
	}

	@Override
	public void deleteSave() {
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
