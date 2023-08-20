package org.asf.edge.common.services.config.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;

import org.asf.edge.common.services.config.ConfigProviderService;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ConfigProviderServiceImpl extends ConfigProviderService {

	private HashMap<String, JsonObject> configs = new HashMap<String, JsonObject>();

	@Override
	public void initService() {
	}

	@Override
	public boolean configExists(String namespace, String config) {
		if (!namespace.matches("^[0-9a-z\\-_]+$"))
			throw new IllegalArgumentException(
					"Config namespace may only contain lowercase alphanumeric characters, hyphens and underscores");
		if (!config.matches("^[0-9a-z\\-_]+$"))
			throw new IllegalArgumentException(
					"Config names may only contain lowercase alphanumeric characters, hyphens and underscores");
		synchronized (configs) {
			if (configs.containsKey(namespace + "/" + config))
				return true;
		}
		File root = namespace.equals("server") ? new File(".") : new File(namespace);
		File configFile = new File(root, config + ".json");
		return configFile.exists();
	}

	@Override
	public void saveConfig(String namespace, String config, JsonObject configData) throws IOException {
		if (!namespace.matches("^[0-9a-z\\-_]+$"))
			throw new IllegalArgumentException(
					"Config namespace may only contain lowercase alphanumeric characters, hyphens and underscores");
		if (!config.matches("^[0-9a-z\\-_]+$"))
			throw new IllegalArgumentException(
					"Config names may only contain lowercase alphanumeric characters, hyphens and underscores");
		File root = namespace.equals("server") ? new File(".") : new File(namespace);
		File configFile = new File(root, config + ".json");

		// Save
		synchronized (configs) {
			root.mkdirs();
			Files.writeString(configFile.toPath(), new GsonBuilder().setPrettyPrinting().create().toJson(configData));
			configs.put(namespace + "/" + config, configData);
		}
	}

	@Override
	public JsonObject loadConfig(String namespace, String config, JsonObject defaultConfig) throws IOException {
		if (!namespace.matches("^[0-9a-z\\-_]+$"))
			throw new IllegalArgumentException(
					"Config namespace may only contain lowercase alphanumeric characters, hyphens and underscores");
		if (!config.matches("^[0-9a-z\\-_]+$"))
			throw new IllegalArgumentException(
					"Config names may only contain lowercase alphanumeric characters, hyphens and underscores");
		synchronized (configs) {
			if (configs.containsKey(namespace + "/" + config))
				return configs.get(namespace + "/" + config);
			File root = namespace.equals("server") ? new File(".") : new File(namespace);
			File configFile = new File(root, config + ".json");
			if (configFile.exists()) {
				configs.put(namespace + "/" + config,
						JsonParser.parseString(Files.readString(configFile.toPath())).getAsJsonObject());
				return configs.get(namespace + "/" + config);
			}
		}
		saveConfig(namespace, config, defaultConfig);
		return defaultConfig;
	}

	@Override
	public JsonObject loadConfig(String namespace, String config) throws IOException {
		if (!namespace.matches("^[0-9a-z\\-_]+$"))
			throw new IllegalArgumentException(
					"Config namespace may only contain lowercase alphanumeric characters, hyphens and underscores");
		if (!config.matches("^[0-9a-z\\-_]+$"))
			throw new IllegalArgumentException(
					"Config names may only contain lowercase alphanumeric characters, hyphens and underscores");
		synchronized (configs) {
			if (configs.containsKey(namespace + "/" + config))
				return configs.get(namespace + "/" + config);
			File root = namespace.equals("server") ? new File(".") : new File(namespace);
			File configFile = new File(root, config + ".json");
			if (configFile.exists()) {
				configs.put(namespace + "/" + config,
						JsonParser.parseString(Files.readString(configFile.toPath())).getAsJsonObject());
				return configs.get(namespace + "/" + config);
			}
		}
		return null;
	}

}
