package org.asf.edge.modules.gridclient.grid.channels;

import org.asf.edge.modules.gridclient.grid.channels.packets.textfilter.TextFilterRequestPacket;
import org.asf.edge.modules.gridclient.grid.channels.packets.textfilter.TextFilterResultPacket;
import org.asf.edge.modules.gridclient.phoenix.networking.channels.AbstractPacketChannel;

public class TextFilterChannel extends AbstractPacketChannel {

	@Override
	protected void registerPackets() {
		registerPacket(new TextFilterRequestPacket());
		registerPacket(new TextFilterResultPacket());
	}

	@Override
	protected void registerPacketHandlers() {
	}

}
