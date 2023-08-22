package org.asf.edge.modules.gridclient.phoenix.serverlist;

/**
 * 
 * Server list filter type
 * 
 * @author Sky Swimmer
 *
 */
public enum ServerListFilterType {
	
	/**
	 * Default filter type (uses a loose-like filtering system)
	 */
	DEFAULT,
	
	/**
	 * Exact values only
	 */
	STRICT,
	
	/**
	 * Checks if the value contains the filter string
	 */
	LOOSE,
	
	/**
	 * Only if the entry does not have the exact filter string value
	 */
	REVERSE_STRICT,
	
	/**
	 * Checks if the value does not contains the filter string
	 */
	REVERSE_LOOSE

}
