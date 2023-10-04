package org.asf.edge.common.permissions;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.accounts.AccountObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * 
 * Permission context system
 * 
 * @author Sky Swimmer
 *
 */
public class PermissionContext {

	private AccountObject account;
	private Logger logger = LogManager.getLogger("PERMISSIONS");
	protected static PermissionContextImplementationProvider implementationProvider = (acc) -> new PermissionContext(
			acc);

	public static interface PermissionContextImplementationProvider {
		public PermissionContext getForAccount(AccountObject account);
	}

	public PermissionContext(AccountObject account) {
		this.account = account;
	}

	/**
	 * Retrieves permission context for a account
	 * 
	 * @param account Account to use
	 * @return PermissionContext instance
	 */
	public static PermissionContext getFor(AccountObject account) {
		return implementationProvider.getForAccount(account);
	}

	/**
	 * Retrieves the account object
	 * 
	 * @return AccountObject instance
	 */
	public AccountObject getAccount() {
		return account;
	}

	/**
	 * Checks if the user has the given permission
	 * 
	 * @param node  Permission node to check
	 * @param level Permission level to check if the node is not found
	 * @return True if the user has the permission, false othewise
	 */
	public boolean hasPermission(String node, PermissionLevel level) {
		if (node.endsWith(".*"))
			node = node.substring(0, node.lastIndexOf(".*"));

		// Pull nodes
		PermissionNode[] nodes = getNodes();
		for (PermissionNode nd : nodes) {
			if (nd.getType() == PermissionNodeType.DENY) {
				// Check deny node
				if (nd.getNode().equalsIgnoreCase("*") || node.equalsIgnoreCase(nd.getNode())
						|| node.toLowerCase().startsWith(nd.getNode().toLowerCase() + ".")) {
					return false;
				}
			}
		}
		for (PermissionNode nd : nodes) {
			if (nd.getType() == PermissionNodeType.ALLOW) {
				// Check allow node
				if (nd.getNode().equalsIgnoreCase("*") || node.equalsIgnoreCase(nd.getNode())
						|| node.toLowerCase().startsWith(nd.getNode().toLowerCase() + ".")) {
					return true;
				}
			}
		}

		// Check level instead
		return getPermissionLevel().ordinal() >= level.ordinal();
	}

	/**
	 * Adds permission nodes
	 * 
	 * @param node Permission node to add
	 * @param type Permission node type
	 */
	public void addNode(String node, PermissionNodeType type) {
		addNode(new PermissionNode(node, type));
	}

	/**
	 * Adds permission nodes
	 * 
	 * @param node Permission node to add
	 */
	public void addNode(PermissionNode node) {
		try {
			// Retrieve perms
			JsonObject perms;
			if (account.getAccountData().entryExists("permissions"))
				perms = account.getAccountData().getEntry("permissions").getAsJsonObject();
			else {
				perms = new JsonObject();
				JsonObject nodes = new JsonObject();
				nodes.add("allowed", new JsonArray());
				nodes.add("denied", new JsonArray());
				perms.add("permissionNodes", nodes);
			}
			perms = perms.get("permissionNodes").getAsJsonObject();
			JsonArray allowed = perms.get("allowed").getAsJsonArray();
			JsonArray denied = perms.get("denied").getAsJsonArray();

			// Add
			if (node.getType() == PermissionNodeType.ALLOW) {
				allowed.add(node.getNode());
			} else {
				denied.add(node.getNode());
			}

			// Save
			account.getAccountData().setEntry("permissions", perms);
		} catch (IOException e) {
			// Database down
			logger.warn("Failed to modify permission nodes for " + account.getAccountID() + " due to a database error.",
					e);
		}
	}

	/**
	 * Removes permission nodes
	 * 
	 * @param node Permission node to remove
	 */
	public void removeNode(PermissionNode node) {
		try {
			if (account.getAccountData().entryExists("permissions")) {
				// Retrieve perms
				JsonObject perms = account.getAccountData().getEntry("permissions").getAsJsonObject();
				perms = perms.get("permissionNodes").getAsJsonObject();
				JsonArray allowed = perms.get("allowed").getAsJsonArray();
				JsonArray denied = perms.get("denied").getAsJsonArray();

				// Remove
				if (node.getType() == PermissionNodeType.ALLOW) {
					for (JsonElement ele : allowed) {
						if (ele.getAsString().equals(node.getNode())) {
							// Save
							allowed.remove(ele);
							account.getAccountData().setEntry("permissions", perms);
							return;
						}
					}
				} else {
					for (JsonElement ele : denied) {
						if (ele.getAsString().equals(node.getNode())) {
							// Save
							denied.remove(ele);
							account.getAccountData().setEntry("permissions", perms);
							return;
						}
					}
				}
			}
		} catch (IOException e) {
			// Database down
			logger.warn("Failed to modify permission nodes for " + account.getAccountID() + " due to a database error.",
					e);
		}
	}

	/**
	 * Retrieves all permission nodes
	 * 
	 * @return Array of PermissionNode instances
	 */
	public PermissionNode[] getNodes() {
		try {
			if (account.getAccountData().entryExists("permissions")) {
				// Retrieve perms
				JsonObject perms = account.getAccountData().getEntry("permissions").getAsJsonObject();
				perms = perms.get("permissionNodes").getAsJsonObject();
				JsonArray allowed = perms.get("allowed").getAsJsonArray();
				JsonArray denied = perms.get("denied").getAsJsonArray();

				// Retrieve nodes
				ArrayList<PermissionNode> nodes = new ArrayList<PermissionNode>();
				for (JsonElement node : allowed)
					nodes.add(new PermissionNode(node.getAsString(), PermissionNodeType.ALLOW));
				for (JsonElement node : denied)
					nodes.add(new PermissionNode(node.getAsString(), PermissionNodeType.DENY));
				return nodes.toArray(t -> new PermissionNode[t]);
			}
			return new PermissionNode[0];
		} catch (IOException e) {
			// Database down
			logger.warn(
					"Failed to retrieve permission nodes for " + account.getAccountID() + " due to a database error.",
					e);
			return new PermissionNode[0];
		}
	}

	/**
	 * Assigns the permission level
	 * 
	 * @param newLevel New permission level
	 */
	public void setPermissionLevel(PermissionLevel newLevel) {
		try {
			JsonObject perms;
			if (account.getAccountData().entryExists("permissions"))
				perms = account.getAccountData().getEntry("permissions").getAsJsonObject();
			else {
				perms = new JsonObject();
				JsonObject nodes = new JsonObject();
				nodes.add("allowed", new JsonArray());
				nodes.add("denied", new JsonArray());
				perms.add("permissionNodes", nodes);
			}

			// Set level
			perms.addProperty("permissionLevel", newLevel.toString());

			// Save
			account.getAccountData().setEntry("permissions", perms);
		} catch (IOException e) {
			// Database down
			logger.warn("Failed to save permission level for " + account.getAccountID() + " due to a database error.",
					e);
		}
	}

	/**
	 * Retrieves the player permission level
	 * 
	 * @return PermissionLevel value
	 */
	public PermissionLevel getPermissionLevel() {
		// Find level
		JsonElement ele;
		try {
			ele = account.getAccountData().getEntry("permissions");
			if (ele != null) {
				String level = ele.getAsJsonObject().get("permissionLevel").getAsString();
				for (PermissionLevel lv : PermissionLevel.values()) {
					if (lv.toString().equalsIgnoreCase(level)) {
						if (lv == PermissionLevel.GUEST) {
							// Default
							if (account.isGuestAccount())
								return PermissionLevel.GUEST;
							else
								return PermissionLevel.PLAYER;
						}
						return lv;
					}
				}
				logger.warn("Unrecognized permission level: " + level + ", using default level.");
			}
		} catch (IOException e) {
			// Database down
			logger.warn("Failed to retrieve permission level for " + account.getAccountID()
					+ " due to a database error, using default level.", e);
		}

		// Default
		if (account.isGuestAccount())
			return PermissionLevel.GUEST;
		else
			return PermissionLevel.PLAYER;
	}

}
