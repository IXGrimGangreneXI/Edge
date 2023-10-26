package org.asf.edge.modules.gridclient.grid.components.channels;

import org.asf.edge.modules.gridclient.grid.GridClientComponent;
import org.asf.edge.modules.gridclient.grid.channels.TextFilterChannel;
import org.asf.edge.modules.gridclient.phoenix.PhoenixClient;

public class GridNetworkingComponent extends GridClientComponent {

	@Override
	public String componentID() {
		return "gridnetworking";
	}

	@Override
	public void addToClient(PhoenixClient client) {
		// Register channels
		client.registerChannel(new TextFilterChannel());
	}

}
