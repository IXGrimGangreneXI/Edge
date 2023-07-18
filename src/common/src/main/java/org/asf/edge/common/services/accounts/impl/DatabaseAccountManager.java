package org.asf.edge.common.services.accounts.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.accounts.impl.accounts.db.DatabaseAccountObject;
import org.asf.edge.common.services.accounts.impl.accounts.db.DatabaseRequest;

import com.google.gson.JsonParser;

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
				var statement = conn.prepareStatement("SELECT COUNT(ID) FROM USERMAP_V2 WHERE USERNAME = ?");
				statement.setString(1, username);
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
				var statement = conn.prepareStatement("SELECT COUNT(ID) FROM SAVEUSERNAMEMAP_V2 WHERE USERNAME = ?");
				statement.setString(1, username);
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
				var statement = conn.prepareStatement("SELECT ID FROM USERMAP_V2 WHERE USERNAME = ?");
				statement.setString(1, username);
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
				var statement = conn.prepareStatement("SELECT ID FROM SAVEUSERNAMEMAP_V2 WHERE USERNAME = ?");
				statement.setString(1, username);
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
				var statement = conn.prepareStatement("SELECT CREDS FROM USERMAP_V2 WHERE ID = ?");
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
	public boolean accountExists(String id) {
		try {
			// Create prepared statement
			DatabaseRequest conn = createRequest();
			try {
				var statement = conn.prepareStatement("SELECT COUNT(USERNAME) FROM USERMAP_V2 WHERE ID = ?");
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
					var statement = conn.prepareStatement("SELECT USERNAME FROM USERMAP_V2 WHERE ID = ?");
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
					AccountObject acc = new DatabaseAccountObject(aid, username, this);
					return acc;
				} finally {
					conn.close();
				}
			} catch (SQLException e) {
				logger.error("Failed to execute database query request while trying to pull account object of ID '"
						+ aid + "'", e);
				return null;
			}
		} else {
			try {
				// Create prepared statement
				DatabaseRequest conn = createRequest();
				try {
					var statement = conn.prepareStatement("SELECT ID FROM USERMAP_V2 WHERE USERNAME = ?");
					statement.setString(1, "g/" + aid);
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
				var statement = conn.prepareStatement("SELECT ID FROM EMAILMAP_V2 WHERE EMAIL = ?");
				statement.setString(1, email);
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
			DatabaseRequest conn = createRequest();
			try {
				// Create prepared statement
				var statement = conn.prepareStatement(
						"SELECT DATA FROM SAVESPECIFICPLAYERDATA WHERE SVID = ? AND DATAKEY = ? AND PARENT = ? AND PARENTCONTAINER = ?");
				statement.setString(1, id);
				statement.setString(2, "accountid");
				statement.setString(3, "");
				statement.setString(4, "");
				ResultSet res = statement.executeQuery();
				if (!res.next()) {
					res.close();
					statement.close();
					return null;
				}
				String data = res.getString("DATA");
				if (data == null) {
					res.close();
					statement.close();
					return null;
				}

				// Retrieve account
				res.close();
				statement.close();
				String accID = JsonParser.parseString(data).getAsString();
				AccountObject acc = getAccount(accID);
				if (acc == null)
					return null;

				// Retrieve save
				return acc.getSave(id);
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to retrieve save '" + id + "'", e);
			return null;
		}
	}

	@Override
	protected AccountObject registerGuest(String id, String guestID) {
		AccountObject obj = null;
		try {
			// Insert information
			DatabaseRequest conn = createRequest();
			try {
				obj = new DatabaseAccountObject(id, "g/" + guestID, this);

				// Insert user
				var statement = conn.prepareStatement("INSERT INTO USERMAP_V2 VALUES(?, ?, ?)");
				statement.setString(1, "g/" + guestID);
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
	protected AccountObject registerAccount(String accID, String email, String username, byte[] cred) {
		AccountObject obj = null;
		try {
			DatabaseRequest conn = createRequest();
			try {
				// Register account
				obj = new DatabaseAccountObject(accID, username, this);

				// Insert email
				var statement = conn.prepareStatement("INSERT INTO EMAILMAP_V2 VALUES(?, ?)");
				statement.setString(1, email);
				statement.setString(2, accID);
				statement.execute();
				statement.close();

				// Insert user
				statement = conn.prepareStatement("INSERT INTO USERMAP_V2 VALUES(?, ?, ?)");
				statement.setString(1, username);
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

}
