package org.asf.edge.common.services.accounts.impl;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

import org.asf.edge.common.entities.tables.accounts.AccountPropertiesRow;
import org.asf.edge.common.entities.tables.saves.SaveDetailsRow;
import org.asf.edge.common.services.accounts.AccountDataTableContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.accounts.impl.accounts.db.DatabaseAccountObject;
import org.asf.edge.common.services.accounts.impl.accounts.db.DatabaseDataTableContainer;
import org.asf.edge.common.services.accounts.impl.accounts.db.DatabaseRequest;

public abstract class DatabaseAccountManager extends BasicAccountManager {

	@Override
	public void initService() {
	}

	/**
	 * Called to create database requests
	 * 
	 * @return DatabaseRequest instance
	 */
	public abstract DatabaseRequest createRequest() throws SQLException;

	@Override
	public boolean isLoginNameInUse(String username) {
		try {
			// Create prepared statement
			DatabaseRequest conn = createRequest();
			try {
				var statement = conn.prepareStatement("SELECT COUNT(ID) FROM USERMAP_V3 WHERE USERNAME = ?");
				statement.setString(1, username.toLowerCase());
				ResultSet res = statement.executeQuery();
				if (!res.next()) {
					res.close();
					statement.close();
					return false; // Not found
				}
				boolean r = res.getInt(1) != 0;
				res.close();
				statement.close();
				return r;
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to check if username '" + username
					+ "' is taken", e);
			return false;
		}
	}

	@Override
	public boolean isVikingNameInUse(String username) {
		try {
			// Create prepared statement
			DatabaseRequest conn = createRequest();
			try {
				// Create prepared statement
				var statement = conn.prepareStatement("SELECT COUNT(ID) FROM SAVEUSERNAMEMAP_V3 WHERE USERNAME = ?");
				statement.setString(1, username.toLowerCase());
				ResultSet res = statement.executeQuery();
				if (!res.next()) {
					res.close();
					statement.close();
					return false; // Not found
				}
				boolean r = res.getInt(1) != 0;
				res.close();
				statement.close();
				return r;
			} finally {
				conn.close();
			}
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
			DatabaseRequest conn = createRequest();
			try {
				var statement = conn.prepareStatement("SELECT ID FROM USERMAP_V3 WHERE USERNAME = ?");
				statement.setString(1, username.toLowerCase());
				ResultSet res = statement.executeQuery();
				if (!res.next()) {
					res.close();
					statement.close();
					return null; // Not found
				}
				String r = res.getString("ID");
				res.close();
				statement.close();
				return r;
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to pull user ID of username '"
					+ username + "'", e);
			return null;
		}
	}

	@Override
	public String getAccountIdBySaveUsername(String username) {
		try {
			// Create prepared statement
			String id;
			DatabaseRequest conn = createRequest();
			try {
				var statement = conn.prepareStatement("SELECT ID FROM SAVEUSERNAMEMAP_V3 WHERE USERNAME = ?");
				statement.setString(1, username.toLowerCase());
				ResultSet res = statement.executeQuery();
				if (!res.next()) {
					res.close();
					statement.close();
					return null;
				}
				id = res.getString("ID");
				res.close();
				statement.close();
			} finally {
				conn.close();
			}

			// Pull account ID
			AccountSaveContainer save = getSaveByID(id);
			if (save == null)
				return null;
			return save.getAccount().getAccountID();
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to pull user ID of save username '"
					+ username + "'", e);
			return null;
		}
	}

	@Override
	protected byte[] getPasswordCheckData(String id) {
		try {
			// Create prepared statement
			DatabaseRequest conn = createRequest();
			try {
				var statement = conn.prepareStatement("SELECT CREDS FROM USERMAP_V3 WHERE ID = ?");
				statement.setString(1, id);
				ResultSet res = statement.executeQuery();
				if (!res.next()) {
					res.close();
					statement.close();
					return null;
				}
				byte[] data = res.getBytes("CREDS");
				res.close();
				statement.close();

				// Check data
				if (data == null)
					return null;
				if (data.length != 48) {
					logger.error("Detected corrupted credentials for ID '" + id + "' during password verification.");
					return null;
				}

				return data;
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to verify password for ID '" + id + "'",
					e);
			return null;
		}
	}

	@Override
	public void runForAllAccounts(Function<AccountObject, Boolean> func) {
		try {
			// Create prepared statement
			DatabaseRequest conn = createRequest();
			try {
				var statement = conn.prepareStatement("SELECT ID, USERNAME FROM USERMAP_V3");
				ResultSet res = statement.executeQuery();
				while (res.next()) {
					String id = res.getString("ID");
					if (id == null) {
						res.close();
						statement.close();
						return;
					}

					// Pull name
					String oName = res.getString("USERNAME");
					String name = new DatabaseAccountObject(id, oName, this)
							.getAccountDataTable("accountdata", AccountPropertiesRow.class)
							.getFirstRow("accountUsername", String.class);
					if (name == null)
						name = oName;

					// Pull account
					DatabaseAccountObject acc = new DatabaseAccountObject(id, name, this);

					// Run function
					try {
						if (!func.apply(acc))
							break;
					} catch (Exception e) {
						logger.error("Exception occurred while running runForAllAccounts!", e);
						break;
					}
				}
				res.close();
				statement.close();
			} finally {
				conn.close();
			}
		} catch (SQLException | IOException e) {
			logger.error("Failed to execute database query request while trying to run a function for all accounts", e);
		}
	}

	@Override
	public boolean accountExists(String id) {
		try {
			// Create prepared statement
			DatabaseRequest conn = createRequest();
			try {
				var statement = conn.prepareStatement("SELECT COUNT(USERNAME) FROM USERMAP_V3 WHERE ID = ?");
				statement.setString(1, id);
				ResultSet res = statement.executeQuery();
				if (!res.next()) {
					res.close();
					statement.close();
					return false;
				}
				boolean r = res.getInt(1) != 0;
				res.close();
				statement.close();
				return r;
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to check if ID '" + id + "' exists", e);
			return false;
		}
	}

	@Override
	protected AccountObject getAccountByID(String aid, boolean guest) {
		if (!guest) {
			try {
				// Create prepared statement
				DatabaseRequest conn = createRequest();
				try {
					var statement = conn.prepareStatement("SELECT USERNAME FROM USERMAP_V3 WHERE ID = ?");
					statement.setString(1, aid);
					ResultSet res = statement.executeQuery();
					if (!res.next()) {
						res.close();
						statement.close();
						return null;
					}
					String username = res.getString("USERNAME");
					if (username == null) {
						res.close();
						statement.close();
						return null;
					}
					res.close();
					statement.close();

					// Pull name
					String oName = username;
					username = new DatabaseAccountObject(aid, oName, this)
							.getAccountDataTable("accountdata", AccountPropertiesRow.class)
							.getFirstRow("accountUsername", String.class);
					if (username == null)
						username = oName;

					// Return
					AccountObject acc = new DatabaseAccountObject(aid, username, this);
					return acc;
				} finally {
					conn.close();
				}
			} catch (SQLException | IOException e) {
				logger.error("Failed to execute database query request while trying to pull account object of ID '"
						+ aid + "'", e);
				return null;
			}
		} else {
			try {
				// Create prepared statement
				DatabaseRequest conn = createRequest();
				try {
					var statement = conn.prepareStatement("SELECT ID FROM USERMAP_V3 WHERE USERNAME = ?");
					statement.setString(1, "g/" + aid.toLowerCase());
					ResultSet res = statement.executeQuery();
					if (!res.next()) {
						res.close();
						statement.close();
						return null;
					}
					String id = res.getString("ID");
					if (id == null) {
						res.close();
						statement.close();
						return null;
					}
					res.close();
					statement.close();
					DatabaseAccountObject acc = new DatabaseAccountObject(id, "g/" + aid, this);
					return acc;
				} finally {
					conn.close();
				}
			} catch (SQLException e) {
				logger.error(
						"Failed to execute database query request while trying to pull account object of guest ID '"
								+ aid + "'",
						e);
				return null;
			}
		}
	}

	@Override
	public String getAccountIDByEmail(String email) {
		try {
			// Create prepared statement
			DatabaseRequest conn = createRequest();
			try {
				var statement = conn.prepareStatement("SELECT ID FROM EMAILMAP_V3 WHERE EMAIL = ?");
				statement.setString(1, email.toLowerCase());
				ResultSet res = statement.executeQuery();
				if (!res.next()) {
					res.close();
					statement.close();
					return null;
				}
				String r = res.getString("ID");
				res.close();
				statement.close();
				return r;
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error(
					"Failed to execute database query request while trying to pull user ID of email '" + email + "'",
					e);
			return null;
		}
	}

	@Override
	public AccountSaveContainer getSaveByID(String id) {
		try {
			// Pull details
			AccountDataTableContainer<SaveDetailsRow> cont = new DatabaseDataTableContainer<SaveDetailsRow>(
					"STC2_SAVEDETAILS", id, null, null, this, SaveDetailsRow.class);
			SaveDetailsRow details = cont.getFirstRow();
			if (details == null)
				return null;

			// Retrieve account
			AccountObject acc = getAccount(details.accountID);
			if (acc == null)
				return null;

			// Retrieve save
			return acc.getSave(id);
		} catch (IOException e) {
			logger.error("Failed to execute database query request while trying to retrieve save '" + id + "'", e);
			return null;
		}
	}

	@Override
	public BasicAccountObject registerGuest(String id, String guestID) {
		BasicAccountObject obj = null;
		try {
			// Insert information
			DatabaseRequest conn = createRequest();
			try {
				obj = new DatabaseAccountObject(id, "g/" + guestID, this);

				// Insert user
				var statement = conn.prepareStatement("INSERT INTO USERMAP_V3 VALUES(?, ?, ?)");
				statement.setString(1, "g/" + guestID.toLowerCase());
				statement.setString(2, id);
				statement.setBytes(3, new byte[0]);
				statement.execute();
				statement.close();

				// Insert save
				statement = conn.prepareStatement("INSERT INTO SAVEMAP_V2 VALUES(?, ?)");
				statement.setString(1, id);
				conn.setDataObject(2, "[]", statement);
				statement.execute();
				statement.close();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to register account '" + id + "'", e);
			return null;
		}
		return obj;
	}

	@Override
	public BasicAccountObject registerAccount(String accID, String email, String username, byte[] cred) {
		BasicAccountObject obj = null;
		try {
			DatabaseRequest conn = createRequest();
			try {
				// Register account
				obj = new DatabaseAccountObject(accID, username, this);

				// Insert email
				if (email != null) {
					var statement = conn.prepareStatement("INSERT INTO EMAILMAP_V3 VALUES(?, ?)");
					statement.setString(1, email.toLowerCase());
					statement.setString(2, accID);
					statement.execute();
					statement.close();
				}

				// Insert user
				var statement = conn.prepareStatement("INSERT INTO USERMAP_V3 VALUES(?, ?, ?)");
				statement.setString(1, username.toLowerCase());
				statement.setString(2, accID);
				statement.setBytes(3, cred);
				statement.execute();
				statement.close();

				// Insert save
				statement = conn.prepareStatement("INSERT INTO SAVEMAP_V2 VALUES(?, ?)");
				statement.setString(1, accID);
				conn.setDataObject(2, "[]", statement);
				statement.execute();
				statement.close();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to register account '" + accID + "'",
					e);
			return null;
		}
		return obj;
	}

	/**
	 * Called to prepare key/value data containers of accounts
	 * 
	 * @param containerName Container name
	 */
	public abstract void prepareAccountKvDataContainer(String containerName);

	/**
	 * Called to prepare key/value data containers of saves
	 * 
	 * @param containerName Container name
	 */
	public abstract void prepareSaveKvDataContainer(String containerName);

	/**
	 * Called to prepare table containers of accounts
	 * 
	 * @param tableName Container name
	 * @param cont      Container instance
	 */
	public abstract void prepareAccountDataTableContainer(String tableName, AccountDataTableContainer<?> cont);

	/**
	 * Called to prepare table containers of saves
	 * 
	 * @param tableName Container name
	 * @param cont      Container instance
	 */
	public abstract void prepareSaveDataTableContainer(String tableName, AccountDataTableContainer<?> cont);
}
