package org.asf.edge.common.experiments;

import java.io.IOException;

import org.asf.edge.common.services.config.ConfigProviderService;
import org.asf.nexus.common.experiments.ExperimentManager;

import com.google.gson.JsonObject;

public class EdgeExperimentManager extends ExperimentManager {

	public static void bindManager() {
		implementation = new EdgeExperimentManager();
	}

	@Override
	protected void init() {
		EdgeDefaultExperiments.registerExperiments(implementation);
	}

	@Override
	protected void saveExperimentConfig(JsonObject config) throws IOException {
		ConfigProviderService.getInstance().saveConfig("server", "experiments", config);
	}

	@Override
	protected JsonObject loadExperimentConfig() throws IOException {
		return ConfigProviderService.getInstance().loadConfig("server", "experiments", new JsonObject());
	}

	@Override
	protected void saveExperimentCache(JsonObject config) throws IOException {
		ConfigProviderService.getInstance().saveConfig("cache", "experiments", config);
	}

	@Override
	protected JsonObject loadExperimentCache() throws IOException {
		return ConfigProviderService.getInstance().loadConfig("cache", "experiments", new JsonObject());
	}

}
