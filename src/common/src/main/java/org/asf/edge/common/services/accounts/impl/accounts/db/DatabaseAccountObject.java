package org.asf.edge.common.services.accounts.impl.accounts.db;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.asf.edge.common.events.accounts.AccountDeletedEvent;
import org.asf.edge.common.events.accounts.AccountEmailUpdateEvent;
import org.asf.edge.common.services.accounts.AccountDataTableContainer;
import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.services.accounts.impl.BasicAccountObject;
import org.asf.edge.common.services.accounts.impl.BasicAccountSaveContainer;
import org.asf.edge.common.services.accounts.impl.DatabaseAccountManager;
import org.asf.edge.common.services.minigamedata.MinigameDataManager;
import org.asf.edge.common.services.tabledata.TableRow;
import org.asf.edge.modules.eventbus.EventBus;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DatabaseAccountObject extends BasicAccountObject {

	private DatabaseAccountManager manager;

	public DatabaseAccountObject(String id, String username, DatabaseAccountManager manager) {
		super(id, username, manager);
		this.manager = manager;
	}

	@Override
	public String getAccountEmail() {
		try {
			DatabaseRequest conn = manager.createRequest();
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("SELECT EMAIL FROM EMAILMAP_V2 WHERE ID = ?");
				statement.setString(1, getAccountID());
				ResultSet res = statement.executeQuery();
				if (!res.next()) {
					res.close();
					statement.close();
					return null;
				}
				String r = res.getString("EMAIL");
				res.close();
				statement.close();
				return r;
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			getLogger().error("Failed to execute database query request while trying to pull account email of ID '"
					+ getAccountID() + "'", e);
			return null;
		}
	}

	@Override
	public boolean performUpdateUsername(String name) {
		try {
			DatabaseRequest conn = manager.createRequest();
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("UPDATE USERMAP_V2 SET USERNAME = ? WHERE ID = ?");
				statement.setString(1, name);
				statement.setString(2, getAccountID());
				statement.execute();
				statement.close();
				return true;
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			getLogger().error("Failed to execute database query request while trying to update username of ID '"
					+ getAccountID() + "'", e);
			return false;
		}
	}

	@Override
	public boolean performUpdatePassword(byte[] cred) {
		try {
			// Create prepared statement
			DatabaseRequest conn = manager.createRequest();
			try {
				var statement = conn.prepareStatement("UPDATE USERMAP_V2 SET CREDS = ? WHERE ID = ?");
				statement.setBytes(1, cred);
				statement.setString(2, getAccountID());
				statement.execute();
				statement.close();
				return true;
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			getLogger().error("Failed to execute database query request while trying to update password of ID '"
					+ getAccountID() + "'", e);
			return false;
		}
	}

	@Override
	public boolean updateEmail(String email) {
		String oldMail = getAccountEmail();

		try {
			if (getAccountEmail() == null) {
				// Insert instead
				DatabaseRequest conn = manager.createRequest();
				try {
					// Create prepared statement
					var statement = conn.prepareStatement("INSERT INTO EMAILMAP_V2 VALUES (?, ?)");
					statement.setString(1, email);
					statement.setString(2, getAccountID());
					statement.execute();
					statement.close();
					return true;
				} finally {
					conn.close();
				}
			}

			// Update
			DatabaseRequest conn = manager.createRequest();
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("UPDATE EMAILMAP_V2 SET EMAIL = ? WHERE ID = ?");
				statement.setString(1, email);
				statement.setString(2, getAccountID());
				statement.execute();
				statement.close();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			getLogger().error("Failed to execute database query request while trying to update email of ID '"
					+ getAccountID() + "'", e);
			return false;
		}

		// Dispatch event
		if (oldMail != null)
			EventBus.getInstance().dispatchEvent(new AccountEmailUpdateEvent(oldMail, email, this, manager));

		// Log
		getLogger().info("Updated email of " + getUsername() + " (ID " + getAccountID() + ")");

		// Return
		return true;
	}

	@Override
	public String[] retrieveSaveIDs() {
		try {
			DatabaseRequest conn = manager.createRequest();
			try {
				// Pull list
				var statement = conn.prepareStatement("SELECT SAVES FROM SAVEMAP_V2 WHERE ACCID = ?");
				statement.setString(1, getAccountID());
				ResultSet res = statement.executeQuery();
				if (!res.next()) {
					statement.close();
					res.close();
					return new String[0];
				}
				JsonArray saves = JsonParser.parseString(res.getString("SAVES")).getAsJsonArray();
				statement.close();
				res.close();
				String[] ids = new String[saves.size()];
				int i = 0;
				for (JsonElement ele : saves) {
					JsonObject saveObj = ele.getAsJsonObject();
					ids[i++] = saveObj.get("id").getAsString();
				}
				return ids;
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			getLogger().error("Failed to execute database query request while trying to pull save list of ID '"
					+ getAccountID() + "'", e);
			return new String[0];
		}
	}

	@Override
	public BasicAccountSaveContainer performCreateSave(String saveID, String username) {
		try {
			// Create object
			JsonObject saveObj = new JsonObject();
			saveObj.addProperty("id", saveID);
			saveObj.addProperty("username", username);
			saveObj.addProperty("creationTime", System.currentTimeMillis());
			DatabaseRequest conn = manager.createRequest();
			try {
				// Pull list
				var statement = conn.prepareStatement("SELECT SAVES FROM SAVEMAP_V2 WHERE ACCID = ?");
				statement.setString(1, getAccountID());
				ResultSet res = statement.executeQuery();
				if (!res.next()) {
					res.close();
					statement.close();
					return null;
				}
				JsonArray saves = JsonParser.parseString(res.getString("SAVES")).getAsJsonArray();
				res.close();
				statement.close();
				for (JsonElement ele : saves) {
					JsonObject saveObj2 = ele.getAsJsonObject();
					if (saveObj2.get("username").getAsString().equals(username))
						return null;
				}

				// Add object
				saves.add(saveObj);

				// Write to db
				statement = conn.prepareStatement("UPDATE SAVEMAP_V2 SET SAVES = ? WHERE ACCID = ?");
				conn.setDataObject(1, saves.toString(), statement);
				statement.setString(2, getAccountID());
				statement.execute();
				statement.close();

				// Write username to db
				statement = conn.prepareStatement("INSERT INTO SAVEUSERNAMEMAP_V2 VALUES (?, ?)");
				statement.setString(1, username);
				statement.setString(2, saveID);
				statement.execute();
				statement.close();
			} finally {
				conn.close();
			}

			// Return
			return new DatabaseSaveContainer(saveID, saveObj.get("creationTime").getAsLong(), username, getAccountID(),
					manager, this);
		} catch (SQLException e) {
			getLogger().error("Failed to execute database query request while trying to create save '" + saveID
					+ "' for ID '" + getAccountID() + "'", e);
			return null;
		}
	}

	@Override
	public BasicAccountSaveContainer findSave(String saveID) {
		// Add
		try {
			JsonArray saves;
			DatabaseRequest conn = manager.createRequest();
			try {
				// Pull list
				var statement = conn.prepareStatement("SELECT SAVES FROM SAVEMAP_V2 WHERE ACCID = ?");
				statement.setString(1, getAccountID());
				ResultSet res = statement.executeQuery();
				if (!res.next()) {
					res.close();
					statement.close();
					return null;
				}
				saves = JsonParser.parseString(res.getString("SAVES")).getAsJsonArray();
				res.close();
				statement.close();
			} finally {
				conn.close();
			}
			for (JsonElement ele : saves) {
				JsonObject saveObj = ele.getAsJsonObject();
				if (saveObj.get("id").getAsString().equals(saveID)) {
					// Found it
					String username = saveObj.get("username").getAsString();
					BasicAccountSaveContainer save = new DatabaseSaveContainer(saveID,
							saveObj.get("creationTime").getAsLong(), username, getAccountID(), manager, this);
					return save;
				}
			}
			return null;
		} catch (SQLException e) {
			getLogger().error("Failed to execute database query request while trying to pull save '" + saveID
					+ "' of ID '" + getAccountID() + "'", e);
			return null;
		}
	}

	@Override
	public void deleteAccount() throws IOException {
		// Dispatch event
		EventBus.getInstance().dispatchEvent(new AccountDeletedEvent(this, manager));

		// Delete account
		try {
			DatabaseRequest conn = manager.createRequest();
			JsonArray saves = null;
			try {
				// Delete data
				// Fetch all table names
				try {
					// Create prepared statement
					var statement = conn.prepareStatement("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS");
					ResultSet res = statement.executeQuery();

					// Find results
					while (res.next()) {
						// Check name
						// ACCOUNTWIDEPLAYERDATA_V2 = key/value data container named 'LEGACY'
						// UDC2_<NAME> = key/value data container
						// UTC2_<NAME> = table-based data container
						String cont = res.getString("TABLE_NAME");
						if (cont.equalsIgnoreCase("ACCOUNTWIDEPLAYERDATA_V2") || cont.toLowerCase().startsWith("udc2_")
								|| cont.toLowerCase().startsWith("utc2_")) {
							// This is a data container
							// Remove all player data from it
							var st2 = conn.prepareStatement("DELETE FROM " + cont + " WHERE "
									+ (cont.toLowerCase().startsWith("utc2_") ? "CID" : "ACCID") + " = ?");
							st2.setString(1, getAccountID());
							st2.execute();
							st2.close();
						}
					}
					res.close();
					statement.close();
				} catch (SQLException e) {
					getLogger().error("Failed to execute database query request while trying to delete account '"
							+ getAccountID() + "'", e);
					throw new IOException("SQL error", e);
				}

				// Delete from user map
				try {
					// Create prepared statement
					var statement = conn.prepareStatement("DELETE FROM USERMAP_V2 WHERE ID = ?");
					statement.setString(1, getAccountID());
					statement.execute();
					statement.close();
				} catch (SQLException e) {
					getLogger().error("Failed to execute database query request while trying to delete account '"
							+ getAccountID() + "'", e);
					throw new IOException("SQL error", e);
				}

				// Delete from email map
				try {
					// Create prepared statement
					var statement = conn.prepareStatement("DELETE FROM EMAILMAP_V2 WHERE ID = ?");
					statement.setString(1, getAccountID());
					statement.execute();
					statement.close();
				} catch (SQLException e) {
					getLogger().error("Failed to execute database query request while trying to delete account '"
							+ getAccountID() + "'", e);
					throw new IOException("SQL error", e);
				}

				// Delete save list
				try {
					// Pull list
					var statement = conn.prepareStatement("SELECT SAVES FROM SAVEMAP_V2 WHERE ACCID = ?");
					statement.setString(1, getAccountID());
					ResultSet res = statement.executeQuery();
					if (!res.next()) {
						statement.close();
						res.close();
						return;
					}
					saves = JsonParser.parseString(res.getString("SAVES")).getAsJsonArray();
					statement.close();
					res.close();

					// Delete each save
					for (JsonElement saveEle : saves) {
						JsonObject saveObj = saveEle.getAsJsonObject();

						// Delete name lock
						statement = conn.prepareStatement("DELETE FROM SAVEUSERNAMEMAP_V2 WHERE ID = ?");
						statement.setString(1, saveObj.get("id").getAsString());
						statement.execute();
						statement.close();
					}

					// Delete save list
					statement = conn.prepareStatement("DELETE FROM SAVEMAP_V2 WHERE ACCID = ?");
					statement.setString(1, getAccountID());
					statement.execute();
					statement.close();
				} catch (SQLException e) {
					getLogger().error("Failed to execute database query request while trying to delete account '"
							+ getAccountID() + "'", e);
					throw new IOException("SQL error", e);
				}
			} finally {
				conn.close();
			}

			// Delete each save
			for (JsonElement saveEle : saves) {
				JsonObject saveObj = saveEle.getAsJsonObject();

				// Delete save
				((DatabaseSaveContainer) getSave(saveObj.get("id").getAsString())).doDeleteSave();
				MinigameDataManager.getInstance().deleteDataFor(saveObj.get("id").getAsString());
			}

			// Log
			getLogger().info("Deleted account " + getUsername() + " (ID " + getAccountID() + ")");
		} catch (SQLException e) {
			getLogger().error(
					"Failed to execute database query request while trying to delete account '" + getAccountID() + "'",
					e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected AccountKvDataContainer getKeyValueContainerInternal(String rootNodeName) {
		// Compute table name
		String table;
		rootNodeName = rootNodeName.toUpperCase();
		if (rootNodeName.equalsIgnoreCase("LEGACY"))
			table = "ACCOUNTWIDEPLAYERDATA_V2";
		else
			table = "UDC2_" + rootNodeName;
		return new DatabaseAccountKvDataContainer(table, this, getAccountID(), manager);
	}

	@Override
	protected void setupKeyValueContainer(String rootNodeName) {
		manager.prepareAccountKvDataContainer(rootNodeName);
	}

	@Override
	protected <T extends TableRow> AccountDataTableContainer<T> getDataTableContainerInternal(String tableName,
			Class<T> cls) {
		return new DatabaseDataTableContainer<T>("UTC2_" + tableName, getAccountID(), this, null, manager, cls);
	}

	@Override
	protected void setupDataTableContainer(String tableName, AccountDataTableContainer<?> cont) {
		manager.prepareAccountDataTableContainer(tableName, cont);
	}

}
