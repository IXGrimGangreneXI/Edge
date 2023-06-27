package org.asf.edge.common.services.commondata;

import java.util.ConcurrentModificationException;
import java.util.HashMap;

import org.asf.edge.common.services.AbstractService;

/**
 * 
 * Common Data Manager API
 * 
 * @author Sky Swimmer
 *
 */
public abstract class CommonDataManager extends AbstractService {

	private HashMap<String, CommonDataContainer> loadedContainers = new HashMap<String, CommonDataContainer>();

	/**
	 * Called to retrieve containers
	 * 
	 * @param rootNodeName Root node name
	 * @return CommonDataContainer instance
	 */
	protected abstract CommonDataContainer getContainerInternal(String rootNodeName);

	/**
	 * Called to set up containers, called the first time a container is retrieved
	 * 
	 * @param rootNodeName Root node name
	 */
	protected abstract void setupContainer(String rootNodeName);

	/**
	 * Retrieves containers
	 * 
	 * @param rootNodeName Root node name
	 * @return CommonDataContainer instance
	 */
	public CommonDataContainer getContainer(String rootNodeName) {
		if (!rootNodeName.matches("^[A-Za-z0-9]+$"))
			throw new IllegalArgumentException("Root node name can only contain alphanumeric characters");
		rootNodeName = rootNodeName.toUpperCase();
		while (true) {
			try {
				if (loadedContainers.containsKey(rootNodeName))
					return loadedContainers.get(rootNodeName);
				break;
			} catch (ConcurrentModificationException e) {
			}
		}

		// Lock
		synchronized (loadedContainers) {
			if (loadedContainers.containsKey(rootNodeName))
				return loadedContainers.get(rootNodeName); // Seems another thread had added it before we got the lock

			// Add container
			CommonDataContainer cont = getContainerInternal(rootNodeName);
			setupContainer(rootNodeName);
			loadedContainers.put(rootNodeName, cont);
			return cont;
		}
	}

}
