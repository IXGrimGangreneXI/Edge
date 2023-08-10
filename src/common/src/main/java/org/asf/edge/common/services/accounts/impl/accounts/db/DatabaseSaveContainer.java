package org.asf.edge.common.services.accounts.impl.accounts.db;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.impl.BasicAccountSaveContainer;
import org.asf.edge.common.services.accounts.impl.DatabaseAccountManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class DatabaseSaveContainer extends BasicAccountSaveContainer {

	private DatabaseAccountManager manager;

	public DatabaseSaveContainer(String saveID, long time, String username, String id, DatabaseAccountManager manager,
			DatabaseAccountObject acc) {
		super(saveID, time, username, id, manager, acc);
		this.manager = manager;
		AccountDataContainer sv = this.getSaveData();
		try {
			if (!sv.entryExists("accountid")) {
				sv.setEntry("accountid", new JsonPrimitive(acc.getAccountID()));
			}
		} catch (IOException e) {
		}
	}

	@Override
	public boolean performUpdateUsername(String name) {
		try {
			DatabaseRequest conn = manager.createRequest();
			try {
				// Update save info
				var statement = conn.prepareStatement("SELECT SAVES FROM SAVEMAP_V2 WHERE ACCID = ?");
				statement.setString(1, getAccountID());
				ResultSet res = statement.executeQuery();
				if (!res.next()) {
					res.close();
					statement.close();
					return false;
				}
				JsonArray saves = JsonParser.parseString(res.getString("SAVES")).getAsJsonArray();
				res.close();
				statement.close();
				for (JsonElement ele : saves) {
					JsonObject saveObj = ele.getAsJsonObject();
					if (saveObj.get("id").getAsString().equals(getSaveID())) {
						// Update
						saveObj.remove("username");
						saveObj.addProperty("username", name);

						// Save to db
						statement = conn.prepareStatement("UPDATE SAVEMAP_V2 SET SAVES = ? WHERE ACCID = ?");
						conn.setDataObject(1, saves.toString(), statement);
						statement.setString(2, getAccountID());
						statement.execute();
						statement.close();
						break;
					}
				}

				// Update user save map
				statement = conn.prepareStatement("UPDATE SAVEUSERNAMEMAP_V2 SET USERNAME = ? WHERE ID = ?");
				statement.setString(1, name);
				statement.setString(2, getSaveID());
				statement.execute();
				statement.close();
				return true;
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			getLogger().error("Failed to execute database query request while trying to update username of save '"
					+ getSaveID() + "' for ID '" + getAccountID() + "'", e);
			return false;
		}
	}

	@Override
	protected AccountDataContainer retrieveSaveData() {
		return new DatabaseSaveDataContainer(getAccount(), this, manager);
	}

	@Override
	public void doDeleteSave() {
		try {
			DatabaseRequest conn = manager.createRequest();
			try {
				// Delete name lock
				var statement = conn.prepareStatement("DELETE FROM SAVEUSERNAMEMAP_V2 WHERE ID = ?");
				statement.setString(1, getSaveID());
				statement.execute();
				statement.close();

				// Delete save data
				getSaveData().deleteContainer();

				// Delete from save list
				statement = conn.prepareStatement("SELECT SAVES FROM SAVEMAP_V2 WHERE ACCID = ?");
				statement.setString(1, getAccountID());
				ResultSet res = statement.executeQuery();
				if (!res.next()) {
					statement.close();
					res.close();
					return;
				}
				JsonArray saves = JsonParser.parseString(res.getString("SAVES")).getAsJsonArray();
				statement.close();
				res.close();
				for (JsonElement ele : saves) {
					JsonObject saveObj = ele.getAsJsonObject();
					if (saveObj.get("id").getAsString().equals(getSaveID())) {
						// Remove from list
						saves.remove(saveObj);

						// Save to db
						statement = conn.prepareStatement("UPDATE SAVEMAP_V2 SET SAVES = ? WHERE ACCID = ?");
						conn.setDataObject(1, saves.toString(), statement);
						statement.setString(2, getAccountID());
						statement.execute();
						statement.close();
						break;
					}
				}
			} finally {
				conn.close();
			}
		} catch (SQLException | IOException e) {
			getLogger().error("Failed to execute database query request while trying to delete save '" + getSaveID()
					+ "' of ID '" + getAccountID() + "'", e);
		}
	}

}
