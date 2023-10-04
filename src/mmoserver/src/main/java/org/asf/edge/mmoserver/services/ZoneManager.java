package org.asf.edge.mmoserver.services;

import org.asf.edge.common.services.AbstractService;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.mmoserver.entities.smartfox.GameZone;
import org.asf.edge.mmoserver.entities.smartfox.RoomGroup;

/**
 * 
 * Zone Manager Service
 * 
 * @author Sky Swimmer
 * 
 */
public abstract class ZoneManager extends AbstractService {

	/**
	 * Retrieves the active zone manager
	 * 
	 * @return ZoneManager instance
	 */
	public static ZoneManager getInstance() {
		return ServiceManager.getService(ZoneManager.class);
	}

	@Override
	public void initService() {
		// Prepare
		prepareManager();

		// Load zones
		loadZonesFromDisk();

		// Call loaded
		managerLoaded();
	}

	/**
	 * Called when the manager is being prepare
	 */
	protected abstract void prepareManager();

	/**
	 * Called when the manager is loaded
	 */
	protected abstract void managerLoaded();

	/**
	 * Called to load zones from disk
	 */
	protected abstract void loadZonesFromDisk();

	/**
	 * Retrieves all game zones
	 * 
	 * @return Array of GameZone instances
	 */
	public abstract GameZone[] getZones();

	/**
	 * Retrieves all zone names
	 * 
	 * @return Array of zone names
	 */
	public abstract String[] getZoneNames();

	/**
	 * Retrieves zones by name
	 * 
	 * @param name Zone name
	 * @return GameZone instance or null
	 */
	public abstract GameZone getZone(String name);

	/**
	 * Removes zones
	 * 
	 * @param zone Zone to remove
	 */
	public abstract void removeZone(GameZone zone);

	/**
	 * Creates zones
	 * 
	 * @param name Zone name
	 * @return GameZone instance
	 * @throws IllegalArgumentException If the zone already exists
	 */
	public GameZone createZone(String name) throws IllegalArgumentException {
		return createZone(name, true);
	}

	/**
	 * Creates zones
	 * 
	 * @param name     Zone name
	 * @param isActive True if the zone should be active, false otherwise
	 * @return GameZone instance
	 * @throws IllegalArgumentException If the zone already exists
	 */
	public GameZone createZone(String name, boolean isActive) throws IllegalArgumentException {
		return createZone(name, isActive, new RoomGroup[0]);
	}

	/**
	 * Creates zones
	 * 
	 * @param name     Zone name
	 * @param isActive True if the zone should be active, false otherwise
	 * @param groups   Room groups
	 * @return GameZone instance
	 * @throws IllegalArgumentException If the zone already exists
	 */
	public abstract GameZone createZone(String name, boolean isActive, RoomGroup[] groups)
			throws IllegalArgumentException;

}
