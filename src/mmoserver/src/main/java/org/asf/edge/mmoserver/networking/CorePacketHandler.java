package org.asf.edge.mmoserver.networking;

import java.io.IOException;

import org.asf.edge.mmoserver.networking.sfs.SmartfoxPacketData;

public interface CorePacketHandler {

	public void handle(int chId, int id, SmartfoxPacketData data) throws IOException;

}
