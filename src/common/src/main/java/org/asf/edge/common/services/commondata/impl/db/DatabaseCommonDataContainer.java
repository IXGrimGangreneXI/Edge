package org.asf.edge.common.services.commondata.impl.db;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.commondata.CommonDataContainer;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class DatabaseCommonDataContainer extends CommonDataContainer {

	private String table;
	private DatabaseCommonDataManager mgr;
	private Logger logger = LogManager.getLogger("CommonDataManager");

	public DatabaseCommonDataContainer(DatabaseCommonDataManager mgr, String table) {
		this.mgr = mgr;
		this.table = table;
	}

	@Override
	protected JsonElement get(String key) throws IOException {
		try {
			DatabaseRequest req = mgr.createRequest();
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
				var statement = req.createPreparedStatement(
						"SELECT DATA FROM " + table + " WHERE DATAKEY = ? AND PARENT = ? AND PARENTCONTAINER = ?");
				statement.setString(1, key);
				statement.setString(2, parent);
				statement.setString(3, parentContainer);
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
				res.close();
				statement.close();
				return JsonParser.parseString(data);
			} finally {
				req.finish();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to retrieve common data entry '" + key
					+ "' of table '" + table + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected void set(String key, JsonElement value) throws IOException {
		try {
			DatabaseRequest req = mgr.createRequest();
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
				var statement = req.createPreparedStatement(
						"UPDATE " + table + " SET DATA = ? WHERE DATAKEY = ? AND PARENT = ? AND PARENTCONTAINER = ?");
				req.setDataObject(1, value.toString(), statement);
				statement.setString(2, key);
				statement.setString(3, parent);
				statement.setString(4, parentContainer);
				statement.execute();
				statement.close();
			} finally {
				req.finish();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to update common data entry '" + key
					+ "' of table '" + table + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected void create(String key, String root, JsonElement value) throws IOException {
		try {
			DatabaseRequest req = mgr.createRequest();
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
				var statement = req.createPreparedStatement("INSERT INTO " + table + " VALUES(?, ?, ?, ?)");
				statement.setString(1, key);
				statement.setString(2, parent);
				statement.setString(3, parentContainer);
				req.setDataObject(4, value.toString(), statement);
				statement.execute();
				statement.close();
			} finally {
				req.finish();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to create common data entry '" + key
					+ "' in table '" + table + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected boolean exists(String key) throws IOException {
		try {
			DatabaseRequest req = mgr.createRequest();
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
				var statement = req.createPreparedStatement(
						"SELECT DATA FROM " + table + " WHERE DATAKEY = ? AND PARENT = ? AND PARENTCONTAINER = ?");
				statement.setString(1, key);
				statement.setString(2, parent);
				statement.setString(3, parentContainer);
				ResultSet res = statement.executeQuery();
				boolean r = res.next();
				res.close();
				statement.close();
				return r;
			} finally {
				req.finish();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to check common data entry '" + key
					+ "' in table '" + table + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected void delete(String key) throws IOException {
		try {
			DatabaseRequest req = mgr.createRequest();
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
				var statement = req.createPreparedStatement(
						"DELETE FROM " + table + " WHERE DATAKEY = ? AND PARENT = ? AND PARENTCONTAINER = ?");
				statement.setString(1, key);
				statement.setString(2, parent);
				statement.setString(3, parentContainer);
				statement.execute();
				statement.close();
			} finally {
				req.finish();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to delete common data entry '" + key
					+ "' from table '" + table + "'", e);
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
			DatabaseRequest req = mgr.createRequest();
			try {
				// Create prepared statement
				var statement = req.createPreparedStatement(
						"SELECT DATAKEY FROM " + table + " WHERE PARENT = ? AND PARENTCONTAINER = ?");
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
				req.finish();
			}
		} catch (SQLException e) {
			logger.error(
					"Failed to execute database query request while trying to retrieve child containers of common data container '"
							+ key + "' from table '" + table + "'",
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
			DatabaseRequest req = mgr.createRequest();
			try {
				// Create prepared statement
				var statement = req
						.createPreparedStatement("SELECT PARENT FROM " + table + " WHERE PARENTCONTAINER = ?");
				statement.setString(1, key);
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
				req.finish();
			}
		} catch (SQLException e) {
			logger.error(
					"Failed to execute database query request while trying to retrieve child containers of common data container '"
							+ key + "' from table '" + table + "'",
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
			// Delete child containers
			for (String ch : getChildContainers(root)) {
				deleteContainer((root.isEmpty() ? "" : root + "/") + ch);
			}

			// Delete container
			DatabaseRequest req = mgr.createRequest();
			try {
				// Create prepared statement
				var statement = req
						.createPreparedStatement("DELETE FROM " + table + " WHERE PARENT = ? AND PARENTCONTAINER = ?");
				statement.setString(1, parent);
				statement.setString(2, parentContainer);
				statement.execute();
				statement.close();
			} finally {
				req.finish();
			}
		} catch (SQLException e) {
			logger.error("Failed to execute database query request while trying to delete common data container '"
					+ (root.isEmpty() ? "<root>" : root) + "' from table '" + table + "'", e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected JsonElement find(BiFunction<String, JsonElement, Boolean> function, String root) throws IOException {
		JsonElement resO = null;

		// Parse key
		String parent = root;
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
			DatabaseRequest req = mgr.createRequest();
			try {
				// Create prepared statement
				var statement = req.createPreparedStatement(
						"SELECT * FROM " + table + " WHERE PARENT = ? AND PARENTCONTAINER = ?");
				statement.setString(1, parent);
				statement.setString(2, parentContainer);
				ResultSet res = statement.executeQuery();

				// Find results
				while (res.next()) {
					// Add container
					String cont = res.getString("DATAKEY");
					String data = res.getString("DATA");
					if (!cont.isEmpty() && !keys.contains(cont)) {
						keys.add(cont);

						// Run function
						JsonElement d = JsonParser.parseString(data);
						if (function.apply(cont, d)) {
							resO = d;
							break;
						}
					}
				}
				res.close();
				statement.close();
			} finally {
				req.finish();
			}
		} catch (SQLException e) {
			logger.error(
					"Failed to execute database query request while trying to retrieve child containers of common data container '"
							+ root + "' from table '" + table + "'",
					e);
			throw new IOException("SQL error", e);
		}

		// Return
		return resO;
	}

	@Override
	protected void runFor(BiFunction<String, JsonElement, Boolean> function, String root) throws IOException {
		// Parse key
		String parent = root;
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
			DatabaseRequest req = mgr.createRequest();
			try {
				// Create prepared statement
				var statement = req.createPreparedStatement(
						"SELECT * FROM " + table + " WHERE PARENT = ? AND PARENTCONTAINER = ?");
				statement.setString(1, parent);
				statement.setString(2, parentContainer);
				ResultSet res = statement.executeQuery();

				// Find results
				while (res.next()) {
					// Add container
					String cont = res.getString("DATAKEY");
					String data = res.getString("DATA");
					if (!cont.isEmpty() && !keys.contains(cont)) {
						keys.add(cont);

						// Run function
						JsonElement d = JsonParser.parseString(data);
						if (!function.apply(cont, d))
							break;
					}
				}
				res.close();
				statement.close();
			} finally {
				req.finish();
			}
		} catch (SQLException e) {
			logger.error(
					"Failed to execute database query request while trying to retrieve child containers of common data container '"
							+ root + "' from table '" + table + "'",
					e);
			throw new IOException("SQL error", e);
		}
	}

	@Override
	protected void runForChildren(Function<String, Boolean> function, String root) throws IOException {
		// Prepare
		ArrayList<String> containers = new ArrayList<String>();

		// Find all containers
		try {
			DatabaseRequest req = mgr.createRequest();
			try {
				// Create prepared statement
				var statement = req
						.createPreparedStatement("SELECT PARENT FROM " + table + " WHERE PARENTCONTAINER = ?");
				statement.setString(1, root);
				ResultSet res = statement.executeQuery();

				// Find results
				while (res.next()) {
					// Add container
					String cont = res.getString("PARENT");
					if (!cont.isEmpty() && !containers.contains(cont)) {
						containers.add(cont);

						// Run function
						if (!function.apply(cont))
							break;
					}
				}
				res.close();
				statement.close();
			} finally {
				req.finish();
			}
		} catch (SQLException e) {
			logger.error(
					"Failed to execute database query request while trying to retrieve child containers of common data container '"
							+ root + "' from table '" + table + "'",
					e);
			throw new IOException("SQL error", e);
		}
	}

}
