package org.asf.edge.mmoserver.networking;

import java.io.IOException;

import org.asf.edge.mmoserver.networking.packets.AbstractPacketChannel;
import org.asf.edge.mmoserver.networking.packets.ISmartfoxPacket;

public interface CorePacketSender {

	public void send(AbstractPacketChannel channel, ISmartfoxPacket packet) throws IOException;

}
