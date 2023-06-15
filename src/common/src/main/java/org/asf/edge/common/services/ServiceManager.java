package org.asf.edge.common.services;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.modules.eventbus.EventBus;

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
	private static Logger logger = LogManager.getLogger("ServiceManager");

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
			logger.debug("Service implementation " + impl.getClass().getTypeName() + " registered for service "
					+ service.getTypeName() + " with priority " + priority);
		}
	}

	/**
	 * Retrieves service instances
	 * 
	 * @param <T>     Service type
	 * @param service Service class
	 * @return Service instance
	 */
	@SuppressWarnings("unchecked")
	public static <T extends AbstractService> T getService(Class<T> service) {
		if (services.containsKey(service.getTypeName()))
			return (T) services.get(service.getTypeName());

		// Try to select implementation
		T i;
		synchronized (services) {
			if (services.containsKey(service.getTypeName()))
				return (T) services.get(service.getTypeName()); // Seems another thread had registered the service while
																// we were waiting to get the sync

			// Find implementation
			synchronized (serviceImplementations) {
				i = selectServiceImplementationInternal(service);
			}
		}

		// Init if needed
		if (!i.inited)
			i.initService();
		i.inited = true;
		return i;
	}

	/**
	 * Forces re-selection of service implementations
	 * 
	 * @param <T>     Service type
	 * @param service Service class
	 * @return Service instance
	 */
	public static <T extends AbstractService> T selectServiceImplementation(Class<T> service) {
		T i;
		synchronized (services) {
			synchronized (serviceImplementations) {
				i = selectServiceImplementationInternal(service);
			}
		}

		// Init if needed
		if (!i.inited)
			i.initService();
		i.inited = true;
		return i;
	}

	@SuppressWarnings("unchecked")
	private static <T extends AbstractService> T selectServiceImplementationInternal(Class<T> service) {
		if (!serviceImplementations.containsKey(service.getTypeName()))
			throw new IllegalArgumentException("No service implementations for " + service.getTypeName());

		// Find implementation
		RegistrationInfo[] i = serviceImplementations.get(service.getTypeName()).stream()
				.sorted((t1, t2) -> -Integer.compare(t1.priority, t2.priority)).toArray(t -> new RegistrationInfo[t]);
		if (i.length >= 1) {
			// Attach events, register, initialize and return
			EventBus.getInstance().addEventReceiver(i[0].implementation);
			services.put(service.getTypeName(), i[0].implementation);
			logger.debug("Service implementation selected for " + service.getTypeName() + ": "
					+ i[0].getClass().getTypeName());
			return (T) i[0].implementation;
		}
		throw new IllegalArgumentException("No service implementations for " + service.getTypeName());
	}

}
