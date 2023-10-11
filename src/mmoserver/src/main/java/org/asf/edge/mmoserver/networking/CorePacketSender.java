package org.asf.edge.mmoserver.networking;

import java.io.IOException;

import org.asf.edge.mmoserver.networking.packets.PacketChannel;
import org.asf.edge.mmoserver.networking.packets.ISmartfoxPacket;

public interface CorePacketSender {

	public void send(PacketChannel channel, ISmartfoxPacket packet) throws IOException;

}
