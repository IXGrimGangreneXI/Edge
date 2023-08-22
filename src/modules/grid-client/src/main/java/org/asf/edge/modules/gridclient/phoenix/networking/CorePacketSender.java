package org.asf.edge.modules.gridclient.phoenix.networking;

import java.io.IOException;

import org.asf.edge.modules.gridclient.phoenix.networking.channels.AbstractPacketChannel;
import org.asf.edge.modules.gridclient.phoenix.networking.packets.IPhoenixPacket;

public interface CorePacketSender {

	public void send(AbstractPacketChannel channel, IPhoenixPacket packet) throws IOException;

}
