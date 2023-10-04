package org.asf.edge.mmoserver.networking.channels.extension.packets.clientbound;

import org.asf.edge.mmoserver.networking.packets.ISmartfoxPacket;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPacketData;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

public class ClientboundExtensionMessage implements ISmartfoxPacket {

	public String command;
	public SmartfoxPayload payload = new SmartfoxPayload();

	@Override
	public ISmartfoxPacket createInstance() {
		return new ClientboundExtensionMessage();
	}

	@Override
	public short packetID() {
		return 13;
	}

	@Override
	public void parse(SmartfoxPacketData packet) {
		command = packet.payload.getString("c");
		payload = packet.payload.getObject("p");
	}

	@Override
	public void build(SmartfoxPacketData packet) {
		packet.payload.setString("c", command);
		packet.payload.setObject("p", payload);
	}

}
