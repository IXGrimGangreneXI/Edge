package org.asf.edge.common.services;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * EDGE Service Manager
 * 
 * @author Sky Swimmer
 *
 */
public class ServiceManager {

	private static HashMap<String, AbstractService> services = new HashMap<String, AbstractService>();
	private static HashMap<String, ArrayList<RegistrationInfo>> serviceImplementations = new HashMap<String, ArrayList<RegistrationInfo>>();

	private static class RegistrationInfo {
		public int priority;
		public AbstractService implementation;
	}

	/**
	 * Registers service implementations using the default priority level
	 * 
	 * @param <T>     Service type
	 * @param service Service class
	 * @param impl    Implementation instance
	 */
	public static <T extends AbstractService> void registerServiceImplementation(Class<T> service, T impl) {
		registerServiceImplementation(service, impl, ServiceImplementationPriorityLevels.NORMAL);
	}

	/**
	 * Registers service implementations
	 * 
	 * @param <T>      Service type
	 * @param service  Service class
	 * @param impl     Implementation instance
	 * @param priority Priority level, the higher, the more likely its going to be
	 *                 selected, recommended to use a value from
	 *                 {@link ServiceImplementationPriorityLevels}
	 */
	public static <T extends AbstractService> void registerServiceImplementation(Class<T> service, T impl,
			int priority) {
		synchronized (serviceImplementations) {
			// Add type if needed
			if (!serviceImplementations.containsKey(service.getTypeName()))
				serviceImplementations.put(service.getTypeName(), new ArrayList<RegistrationInfo>());

			// Create info
			RegistrationInfo inf = new RegistrationInfo();
			inf.implementation = impl;
			inf.priority = priority;

			// Register
			serviceImplementations.get(service.getTypeName()).add(inf);
		}
	}

	// TODO: service retrieval, setup and events

}
