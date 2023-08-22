package org.asf.edge.modules.gridapi.identities;

import java.util.HashMap;

public class IdentityDef {

	/**
	 * Identity ID
	 */
	public String identity;

	/**
	 * Last time the identity has been updated
	 */
	public long lastUpdateTime;

	/**
	 * Security field for preventing duplicate logins
	 */
	public long significantFieldNumber;

	/**
	 * Security field for preventing duplicate logins
	 */
	public int significantFieldRandom;

	/**
	 * Other properties
	 */
	public HashMap<String, PropertyInfo> properties = new HashMap<String, PropertyInfo>();

}
