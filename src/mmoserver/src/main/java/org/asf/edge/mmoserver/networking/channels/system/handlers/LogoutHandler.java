package org.asf.edge.mmoserver.networking.channels.system.handlers;

import java.io.IOException;
import org.asf.edge.mmoserver.networking.channels.system.packets.serverbound.ServerboundLogoutPacket;
import org.asf.edge.mmoserver.networking.packets.AbstractPacketChannel;
import org.asf.edge.mmoserver.networking.packets.IPacketHandler;

public class LogoutHandler implements IPacketHandler<ServerboundLogoutPacket> {

	@Override
	public Class<ServerboundLogoutPacket> packetClass() {
		return ServerboundLogoutPacket.class;
	}

	@Override
	public boolean handle(ServerboundLogoutPacket packet, AbstractPacketChannel channel) throws IOException {
		channel.getClient().disconnect();
		return true;
	}

}
