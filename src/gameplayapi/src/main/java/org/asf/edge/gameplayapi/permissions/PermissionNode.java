package org.asf.edge.gameplayapi.permissions;

/**
 * 
 * Permission Node Object
 * 
 * @author Sky Swimmer
 *
 */
public class PermissionNode {
	private String node;
	private PermissionNodeType type;

	public PermissionNode(String node, PermissionNodeType type) {
		if (node.endsWith(".*"))
			node = node.substring(0, node.lastIndexOf(".*"));
		this.node = node;
		this.type = type;
	}

	/**
	 * Retrieves the permission node
	 * 
	 * @return Permission node string
	 */
	public String getNode() {
		return node;
	}

	/**
	 * Retrieves the permission node type
	 * 
	 * @return PermissionNodeType value
	 */
	public PermissionNodeType getType() {
		return type;
	}
}
