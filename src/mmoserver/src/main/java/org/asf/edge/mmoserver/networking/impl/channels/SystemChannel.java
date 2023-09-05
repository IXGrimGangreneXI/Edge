package org.asf.edge.mmoserver.networking.impl.channels;

import org.asf.edge.mmoserver.networking.packets.AbstractPacketChannel;

public class SystemChannel extends AbstractPacketChannel {

	@Override
	public byte channelID() {
		return 0;
	}

	@Override
	public AbstractPacketChannel createInstance() {
		return new SystemChannel();
	}

	@Override
	protected void registerPackets() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void registerPacketHandlers() {
		// TODO Auto-generated method stub

	}

}
