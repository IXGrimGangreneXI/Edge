package org.asf.edge.common.permissions;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.entities.tables.permissions.PermissionsRow;
import org.asf.edge.common.services.accounts.AccountDataTableContainer;
import org.asf.edge.common.services.accounts.AccountObject;

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
			AccountDataTableContainer<PermissionsRow> table = account.getAccountDataTable("PERMISSIONS",
					PermissionsRow.class);
			PermissionsRow perms = table.getFirstRow();
			if (perms == null) {
				// Save default
				perms = new PermissionsRow();
				perms.level = account.isGuestAccount() ? PermissionLevel.GUEST : PermissionLevel.PLAYER;
				table.setRows(perms, true);
			}

			// Add
			if (node.getType() == PermissionNodeType.ALLOW) {
				perms.allowedPermissions.add(node.getNode());
			} else {
				perms.deniedPermissions.add(node.getNode());
			}

			// Save
			table.setRows(perms, true);
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
			// Retrieve perms
			AccountDataTableContainer<PermissionsRow> table = account.getAccountDataTable("PERMISSIONS",
					PermissionsRow.class);
			PermissionsRow perms = table.getFirstRow();
			if (perms != null) {
				// Remove
				if (node.getType() == PermissionNodeType.ALLOW) {
					if (perms.allowedPermissions.contains(node.getNode())) {
						// Remove and save
						perms.allowedPermissions.remove(node.getNode());
						table.setRows(perms, true);
					}
				} else {
					if (perms.deniedPermissions.contains(node.getNode())) {
						// Remove and save
						perms.deniedPermissions.remove(node.getNode());
						table.setRows(perms, true);
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
			// Retrieve perms
			AccountDataTableContainer<PermissionsRow> table = account.getAccountDataTable("PERMISSIONS",
					PermissionsRow.class);
			PermissionsRow perms = table.getFirstRow();
			if (perms != null) {
				// Retrieve nodes
				ArrayList<PermissionNode> nodes = new ArrayList<PermissionNode>();
				for (String node : perms.allowedPermissions)
					nodes.add(new PermissionNode(node, PermissionNodeType.ALLOW));
				for (String node : perms.deniedPermissions)
					nodes.add(new PermissionNode(node, PermissionNodeType.DENY));
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
			// Retrieve perms
			AccountDataTableContainer<PermissionsRow> table = account.getAccountDataTable("PERMISSIONS",
					PermissionsRow.class);
			PermissionsRow perms = table.getFirstRow();
			if (perms == null) {
				// Save default
				perms = new PermissionsRow();
				perms.level = account.isGuestAccount() ? PermissionLevel.GUEST : PermissionLevel.PLAYER;
				table.setRows(perms, true);
			}

			// Update
			perms.level = newLevel;

			// Save
			table.setRows(perms, true);
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
		try {// Retrieve perms
			AccountDataTableContainer<PermissionsRow> table = account.getAccountDataTable("PERMISSIONS",
					PermissionsRow.class);
			PermissionsRow perms = table.getFirstRow();
			if (perms != null) {
				if (perms.level == PermissionLevel.GUEST) {
					// Default
					if (account.isGuestAccount())
						return PermissionLevel.GUEST;
					else
						return PermissionLevel.PLAYER;
				}
				return perms.level;
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
