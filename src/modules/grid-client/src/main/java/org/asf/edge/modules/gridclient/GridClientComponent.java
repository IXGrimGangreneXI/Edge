package org.asf.edge.modules.gridclient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.modules.eventbus.IEventReceiver;
import org.asf.edge.modules.gridclient.phoenix.DisconnectReason;
import org.asf.edge.modules.gridclient.phoenix.PhoenixClient;
import org.asf.edge.modules.gridclient.phoenix.certificate.PhoenixCertificate;
import org.asf.edge.modules.gridclient.phoenix.events.ClientProgramHandshakeEvent;
import org.asf.edge.modules.gridclient.phoenix.events.ClientProgramLateHandshakeEvent;

/**
 * 
 * Grid Client Component Abstract
 * 
 * @author Sky Swimmer
 *
 */
public abstract class GridClientComponent implements IEventReceiver {

	private Logger logger = LogManager.getLogger(componentID());

	/**
	 * Grid client accessor
	 */
	public static final GridClientAccessor GridClient = new GridClientAccessor();

	public static class GridClientAccessor {

		/**
		 * Retrieves all client component instances
		 * 
		 * @return Array of GridClientComponent instances
		 */
		public GridClientComponent[] getAllComponents() {
			return org.asf.edge.modules.gridclient.GridClient.components.values()
					.toArray(t -> new GridClientComponent[t]);
		}

		/**
		 * Retrieves components by ID
		 * 
		 * @param componentID Component ID string
		 * @return GridClientComponent instance or null
		 */
		public GridClientComponent getComponent(String componentID) {
			for (GridClientComponent comp : org.asf.edge.modules.gridclient.GridClient.components.values())
				if (comp.componentID().equals(componentID))
					return comp;
			return null;
		}

		/**
		 * Checks if components are present
		 * 
		 * @param componentID Component ID string
		 * @return True if present, false otherwise
		 */
		public boolean hasComponent(String componentID) {
			return org.asf.edge.modules.gridclient.GridClient.components.containsKey(componentID);
		}

		/**
		 * Retrieves components
		 * 
		 * @param <T>          Component type
		 * @param componentCls Component class
		 * @return GridClientComponent instance or null
		 */
		@SuppressWarnings("unchecked")
		public <T extends GridClientComponent> T getComponent(Class<T> componentCls) {
			for (GridClientComponent comp : org.asf.edge.modules.gridclient.GridClient.components.values())
				if (componentCls.isAssignableFrom(comp.getClass()))
					return (T) comp;
			return null;
		}

		/**
		 * Retrieves the Phoenix client instance
		 * 
		 * @return PhoenixClient instance
		 */
		public PhoenixClient getClient() {
			return org.asf.edge.modules.gridclient.GridClient.client;
		}

		/**
		 * Checks if the client is connected
		 * 
		 * @return True if connected, false otherwise
		 */
		public boolean isConnected() {
			return org.asf.edge.modules.gridclient.GridClient.client.isConnected();
		}

		/**
		 * Switches Phoenix servers
		 * 
		 * @param host Server host
		 * @param port Server port
		 * @param cert Server certificate
		 */
		public void switchServer(String host, int port, PhoenixCertificate cert) {
			org.asf.edge.modules.gridclient.GridClient.switchServer(host, port, cert);
		}

	}

	/**
	 * Retrieves the component logger
	 * 
	 * @return Logger instance
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Defines the component ID
	 * 
	 * @return Component ID string
	 */
	public abstract String componentID();

	/**
	 * Initializes the component
	 */
	public void init() {
	}

	/**
	 * De-initializes the component
	 */
	public void deInit() {
	}

	/**
	 * Adds component event handlers, packet channels and packet handlers to the
	 * client
	 * 
	 * @param client Phoenix client instance
	 */
	public abstract void addToClient(PhoenixClient client);

	/**
	 * Called on program handshake
	 * 
	 * @param event Event object for program handshake logic
	 */
	public void programHandshake(ClientProgramHandshakeEvent event) {
	}

	/**
	 * Called on late program handshake
	 * 
	 * @param event Event object for program late handshake logic
	 */
	public void programLateHandshake(ClientProgramLateHandshakeEvent event) {
	}

	/**
	 * Called on connect
	 */
	public void connected(PhoenixClient client) {
	}

	/**
	 * Called on connect failures
	 * 
	 * @param reason Disconnect reason object holding the error
	 */
	public void connectFailed(DisconnectReason event) {
	}

	/**
	 * Called on disconnect
	 * 
	 * @param reason Disconnect reason
	 */
	public void disconnect(DisconnectReason reason) {
	}

}
