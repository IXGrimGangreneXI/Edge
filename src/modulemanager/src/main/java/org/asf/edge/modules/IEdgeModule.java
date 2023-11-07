package org.asf.edge.modules;

import java.util.Map;

import org.asf.nexus.events.IEventReceiver;

/**
 * 
 * EDGE server module interface
 * 
 * @author Sky Swimmer
 *
 */
public interface IEdgeModule extends IEventReceiver {

	/**
	 * Defines the module ID
	 * 
	 * @return Module ID string
	 */
	public String moduleID();

	/**
	 * Defines the module version
	 * 
	 * @return Module version string
	 */
	public String version();

	/**
	 * Called to pre-initialize the module
	 */
	public default void preInit() {
	}

	/**
	 * Called to initialize the module
	 */
	public void init();

	/**
	 * Called to post-initialize the module, called after all modules are loaded
	 */
	public default void postInit() {
	}

	/**
	 * Called when the module configuration is loaded
	 * 
	 * @param config Module configuration map
	 */
	public default void onLoadModuleConfig(Map<String, String> config) {
	}

}
