package org.asf.edge.common.services.config;

import java.io.IOException;

import org.asf.edge.common.services.AbstractService;
import org.asf.edge.common.services.ServiceManager;

import com.google.gson.JsonObject;

/**
 * 
 * Configuration provider service
 * 
 * @author Sky Swimmer
 *
 */
public abstract class ConfigProviderService extends AbstractService {

	/**
	 * Retrieves the config provider service
	 * 
	 * @return ConfigProviderService instance
	 */
	public static ConfigProviderService getInstance() {
		return ServiceManager.getService(ConfigProviderService.class);
	}

	/**
	 * Checks if configuration file exist
	 * 
	 * @param namespace Config namespace
	 * @param config    Config name
	 * @return True if the config exists, false otherwise
	 */
	public abstract boolean configExists(String namespace, String config);

	/**
	 * Saves configuration files
	 * 
	 * @param namespace  Config namespace
	 * @param config     Config name
	 * @param configData Configuration to save
	 * @throws IOException if the config fails to save
	 */
	public abstract void saveConfig(String namespace, String config, JsonObject configData) throws IOException;

	/**
	 * Retrieves configuration files
	 * 
	 * @param namespace     Config namespace
	 * @param config        Config name
	 * @param defaultConfig Default configuration
	 * @return JsonObject instance
	 * @throws IOException if the config fails to load or save
	 */
	public abstract JsonObject loadConfig(String namespace, String config, JsonObject defaultConfig) throws IOException;

	/**
	 * Retrieves configuration files
	 * 
	 * @param namespace Config namespace
	 * @param config    Config name
	 * @return JsonObject instance or null
	 * @throws IOException if the config fails to load
	 */
	public abstract JsonObject loadConfig(String namespace, String config) throws IOException;

}
