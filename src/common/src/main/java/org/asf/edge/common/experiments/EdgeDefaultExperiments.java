package org.asf.edge.common.experiments;

/**
 * 
 * Default experiment keys used in Edge
 * 
 * @author Sky Swimmer
 * 
 */
public class EdgeDefaultExperiments {

	public static final String LEGACY_INVENTORY_SUPPORT = "EXPERIMENT_LEGACY_INVENTORY_SUPPORT";
	public static final String ACHIEVEMENTSV1_SUPPORT = "EXPERIMENT_ACHIEVEMENTSV1_SUPPORT";
	public static final String MMO_SERVER_SUPPORT = "EXPERIMENT_MMO_SERVER_SUPPORT";
	public static final String DT_ITEM_SUPPORT = "EXPERIMENT_DT_ITEM_SUPPORT";

	/**
	 * Registers experiments
	 * 
	 * @param manager Experiment manager
	 */
	public static void registerExperiments(ExperimentManager manager) {
		// Register experiments
		manager.registerExperiment(LEGACY_INVENTORY_SUPPORT);
		manager.registerExperiment(ACHIEVEMENTSV1_SUPPORT);
		manager.registerExperiment(DT_ITEM_SUPPORT);
		manager.registerExperiment(MMO_SERVER_SUPPORT);

		// Assign names
		manager.setExperimentName(LEGACY_INVENTORY_SUPPORT, "1.x/2.x inventory enhancements");
		manager.setExperimentName(ACHIEVEMENTSV1_SUPPORT, "Support for V1 achievement system (gameplay rewards)");
		manager.setExperimentName(DT_ITEM_SUPPORT, "Support for Dragon Tactics items");
		manager.setExperimentName(MMO_SERVER_SUPPORT, "MMO server support (EXTREMELY WIP, LAN ONLY AT THE MOMENT)");
	}

}
