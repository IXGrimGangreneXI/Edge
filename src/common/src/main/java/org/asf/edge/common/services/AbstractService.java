package org.asf.edge.common.services;

import org.asf.edge.modules.eventbus.IEventReceiver;

/**
 * 
 * Abstract Service Class
 * 
 * @author Sky Swimmer
 *
 */
public abstract class AbstractService implements IEventReceiver {

	/**
	 * Called to initialize the service
	 */
	public abstract void initService();

}
