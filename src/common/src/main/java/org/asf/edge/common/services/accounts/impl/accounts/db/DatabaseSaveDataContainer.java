package org.asf.edge.common.services.accounts.impl.accounts.db;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.accounts.impl.DatabaseAccountManager;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class DatabaseSaveDataContainer extends AccountDataContainer {

	private AccountSaveContainer save;
	private String id;

	private AccountObject account;
	private Logger logger = LogManager.getLogger("AccountManager");
	private DatabaseAccountManager manager;

	private HashMap<String, JsonElement> dataCache = new HashMap<String, JsonElement>();

	public DatabaseSaveDataContainer(AccountObject account, AccountSaveContainer save, DatabaseAccountManager manager) {
		this.save = save;
		this.id = save.getSaveID();
		this.account = account;
		this.manager = manager;
	}

	@Override
	protected JsonElement get(String key) throws IOException {
		while (true) {
			try {
				if (dataCache.containsKey(key))
					return dataCache.get(key) == null ? null : dataCache.get(key).deepCopy();
				break;
			} catch (ConcurrentModificationException e) {
			}
		}

		// Add if needed
		synchronized (dataCache) {
			if (dataCache.containsKey(key))
				return dataCache.get(key) == null ? null : dataCache.get(key).deepCopy();
			try {
				DatabaseRequest req = manager.createRequest();
				try {
					// Parse key
					String parent = "";
					String parentContainer = "";
					if (key.contains("/")) {
						parent = key.substring(0, key.lastIndexOf("/"));
						key = key.substring(key.lastIndexOf("/") + 1);

						// Check for inner parent
						if (parent.contains("/")) {
							parentContainer = parent.substring(0, parent.lastIndexOf("/"));
							parent = parent.substring(parent.lastIndexOf("/") + 1);
						}
					}

					// Create prepared statement
					var statement = req.prepareStatement(
							"SELECT DATA FROM SAVESPECIFICPLAYERDATA_V2 WHERE DATAKEY = ? AND PARENT = ? AND PARENTCONTAINER = ? AND SVID = ?");
					statement.setString(1, key);
					statement.setString(2, parent);
					statement.setString(3, parentContainer);
					statement.setString(4, id);
					ResultSet res = statement.executeQuery();
					if (!res.next()) {
						res.close();
						statement.close();
						dataCache.put(key, null);
						return null;
					}
					String data = res.getString("DATA");
					if (data == null) {
						res.close();
						statement.close();
						dataCache.put(key, null);
						return null;
					}
					res.close();
					statement.close();
					JsonElement r = JsonParser.parseString(data);
					dataCache.put(key, r);
					return r;
				} finally {
					req.close();
				}
			} catch (SQLException e) {
				logger.error("Failed to execute database query request while trying to retrieve save data entry '" + key
						+ "' of ID '" + id + "'", e);
				throw new IOException("SQL error", e);
			}
		}
	}

	@Override
	protected void set(String key, JsonElement value) throws IOException {
		try {
			DatabaseRequest req = manager.createRequest();
			try {
				// Parse key
				String parent = "";
				String parentContainer = "";
				if (key.contains("/")) {
					parent = key.substring(0, key.lastIndexOf("/"));
					key = key.substring(key.lastIndexOf("/") + 1);

					// Check for inner parent
					if (parent.contains("/")) {
						parentContainer = parent.substring(0, parent.lastIndexOf("/"));
						parent = parent.substring(parent.lastIndexOf("/") + 1);
					}
				}

				// Create prepared statement
				var statement = req.prepareStatement(
						"UPDATE SAVESPECIFICPLAYERDATA_V2 SET DATA = ? WHERE DATAKEY = ? AND PARENT = ? AND PARENTCONTAINER = ? AND SVID = ?");
				req.setDataObject(1, value.toString(), statement);
				statement.setString(2, key);
				statement.setString(3, parent);
				statement.setString(4, parentContainer);
				statement.setString(5, id);
				statement.execute();
				statement.close();
				dataCache.put(key, value);
			} finally {
				req.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to update save data entry '" + key
					+ "' of ID '" + id + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected void create(String key, String root, JsonElement value) throws IOException {
		try {
			DatabaseRequest req = manager.createRequest();
			try {
				// Parse key
				String parent = "";
				String parentContainer = "";
				if (key.contains("/")) {
					parent = key.substring(0, key.lastIndexOf("/"));
					key = key.substring(key.lastIndexOf("/") + 1);

					// Check for inner parent
					if (parent.contains("/")) {
						parentContainer = parent.substring(0, parent.lastIndexOf("/"));
						parent = parent.substring(parent.lastIndexOf("/") + 1);
					}
				}

				// Create prepared statement
				var statement = req.prepareStatement("INSERT INTO SAVESPECIFICPLAYERDATA_V2 VALUES(?, ?, ?, ?, ?)");
				statement.setString(1, id);
				statement.setString(2, key);
				statement.setString(3, parent);
				statement.setString(4, parentContainer);
				req.setDataObject(5, value.toString(), statement);
				statement.execute();
				statement.close();
				dataCache.put(key, value);
			} finally {
				req.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to create save data entry '" + key
					+ "' of ID '" + id + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected boolean exists(String key) throws IOException {
		while (true) {
			try {
				if (dataCache.containsKey(key))
					return dataCache.get(key) != null;
				break;
			} catch (ConcurrentModificationException e) {
			}
		}

		// Check
		try {
			DatabaseRequest req = manager.createRequest();
			try {
				// Parse key
				String parent = "";
				String parentContainer = "";
				if (key.contains("/")) {
					parent = key.substring(0, key.lastIndexOf("/"));
					key = key.substring(key.lastIndexOf("/") + 1);

					// Check for inner parent
					if (parent.contains("/")) {
						parentContainer = parent.substring(0, parent.lastIndexOf("/"));
						parent = parent.substring(parent.lastIndexOf("/") + 1);
					}
				}

				// Create prepared statement
				var statement = req.prepareStatement(
						"SELECT DATA FROM SAVESPECIFICPLAYERDATA_V2 WHERE DATAKEY = ? AND PARENT = ? AND PARENTCONTAINER = ? AND SVID = ?");
				statement.setString(1, key);
				statement.setString(2, parent);
				statement.setString(3, parentContainer);
				statement.setString(4, id);
				ResultSet res = statement.executeQuery();
				boolean r = res.next();
				if (!r)
					dataCache.put(key, null);
				else {
					// Check
					if (res.getString("DATA") == null)
						dataCache.put(key, null);
				}
				res.close();
				statement.close();
				return r;
			} finally {
				req.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to check save data entry '" + key
					+ "' of ID '" + id + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected void delete(String key) throws IOException {
		try {
			DatabaseRequest req = manager.createRequest();
			try {
				// Parse key
				String parent = "";
				String parentContainer = "";
				if (key.contains("/")) {
					parent = key.substring(0, key.lastIndexOf("/"));
					key = key.substring(key.lastIndexOf("/") + 1);

					// Check for inner parent
					if (parent.contains("/")) {
						parentContainer = parent.substring(0, parent.lastIndexOf("/"));
						parent = parent.substring(parent.lastIndexOf("/") + 1);
					}
				}

				// Create prepared statement
				var statement = req.prepareStatement(
						"DELETE FROM SAVESPECIFICPLAYERDATA_V2 WHERE DATAKEY = ? AND PARENT = ? AND PARENTCONTAINER = ? AND SVID = ?");
				statement.setString(1, key);
				statement.setString(2, parent);
				statement.setString(3, parentContainer);
				statement.setString(4, id);
				statement.execute();
				statement.close();
				dataCache.remove(key);
			} finally {
				req.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to delete save data entry '" + key
					+ "' of ID '" + id + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected String[] getEntryKeys(String key) throws IOException {
		// Parse key
		String parent = key;
		String parentContainer = "";

		// Check for inner parent
		if (parent.contains("/")) {
			parentContainer = parent.substring(0, parent.lastIndexOf("/"));
			parent = parent.substring(parent.lastIndexOf("/") + 1);
		}

		// Prepare
		ArrayList<String> keys = new ArrayList<String>();

		// Find all keys
		try {
			DatabaseRequest req = manager.createRequest();
			try {
				// Create prepared statement
				var statement = req.prepareStatement(
						"SELECT DATAKEY FROM SAVESPECIFICPLAYERDATA_V2 WHERE PARENT = ? AND PARENTCONTAINER = ?");
				statement.setString(1, parent);
				statement.setString(2, parentContainer);
				ResultSet res = statement.executeQuery();

				// Find results
				while (res.next()) {
					// Add container
					String cont = res.getString("DATAKEY");
					if (!cont.isEmpty() && !keys.contains(cont))
						keys.add(cont);
				}
				res.close();
				statement.close();
			} finally {
				req.close();
			}
		} catch (SQLException e) {
			logger.error(
					"Failed to execute database query request while trying to retrieve child containers of save data container '"
							+ key + "' of ID '" + id + "'",
					e);
			throw new IOException("SQL error", e);
		}

		return keys.toArray(t -> new String[t]);
	}

	@Override
	protected String[] getChildContainers(String key) throws IOException {
		// Prepare
		ArrayList<String> containers = new ArrayList<String>();

		// Find all containers
		try {
			DatabaseRequest req = manager.createRequest();
			try {
				// Create prepared statement
				var statement = req.prepareStatement(
						"SELECT PARENT FROM SAVESPECIFICPLAYERDATA_V2 WHERE PARENTCONTAINER = ? AND SVID = ?");
				statement.setString(1, key);
				statement.setString(2, id);
				ResultSet res = statement.executeQuery();

				// Find results
				while (res.next()) {
					// Add container
					String cont = res.getString("PARENT");
					if (!cont.isEmpty() && !containers.contains(cont))
						containers.add(cont);
				}
				res.close();
				statement.close();
			} finally {
				req.close();
			}
		} catch (SQLException e) {
			logger.error(
					"Failed to execute database query request while trying to retrieve child containers of save data container '"
							+ key + "' of ID '" + id + "'",
					e);
			throw new IOException("SQL error", e);
		}

		// Return
		return containers.toArray(t -> new String[t]);
	}

	@Override
	protected void deleteContainer(String root) throws IOException {
		// Parse key
		String parent = root;
		String parentContainer = "";

		// Check for inner parent
		if (parent.contains("/")) {
			parentContainer = parent.substring(0, parent.lastIndexOf("/"));
			parent = parent.substring(parent.lastIndexOf("/") + 1);
		}
		try {
			// Delete entries
			for (String ent : getEntryKeys()) {
				String k = (root.isEmpty() ? "" : root + "/") + ent;
				if (dataCache.containsKey(k))
					dataCache.remove(k);
			}

			// Delete child containers
			for (String ch : getChildContainers(root)) {
				deleteContainer((root.isEmpty() ? "" : root + "/") + ch);
			}

			// Delete container
			DatabaseRequest req = manager.createRequest();
			try {
				// Create prepared statement
				var statement = req.prepareStatement(
						"DELETE FROM SAVESPECIFICPLAYERDATA_V2 WHERE PARENT = ? AND PARENTCONTAINER = ? AND SVID = ?");
				statement.setString(1, parent);
				statement.setString(2, parentContainer);
				statement.setString(3, id);
				statement.execute();
				statement.close();
			} finally {
				req.close();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to delete save data container '"
					+ (root.isEmpty() ? "<root>" : root) + "' of ID '" + id + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	public AccountObject getAccount() {
		return account;
	}

	@Override
	public AccountSaveContainer getSave() {
		return save;
	}

}
