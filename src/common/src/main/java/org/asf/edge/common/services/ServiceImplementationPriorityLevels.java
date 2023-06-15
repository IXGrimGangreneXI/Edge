package org.asf.edge.common.services;

public class ServiceImplementationPriorityLevels {

	/**
	 * Default implementation priority level
	 */
	public static final int DEFAULT = -10;

	/**
	 * Low priority level
	 */
	public static final int LOW = -1;

	/**
	 * Normal priority level
	 */
	public static final int NORMAL = 0;

	/**
	 * High priority level
	 */
	public static final int HIGH = 1;

	/**
	 * Module priority level, meant for use with non-server-specific modules
	 */
	public static final int MODULE = 5;

	/**
	 * User priority level, meant for use with server-specific module, this should
	 * only be used if server operators want to override services
	 */
	public static final int USER = 10;

}
