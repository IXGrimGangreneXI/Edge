package org.asf.edge.modules.gridclient.phoenix.networking;

import java.io.IOException;

public interface CorePacketHandler {

	public void handle(int chId, int id, DataReader reader) throws IOException;

}
