package org.asf.edge.mmoserver.networking.channels.extension.packets.serverbound;

import org.asf.edge.mmoserver.networking.packets.ISmartfoxPacket;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPacketData;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

public class ServerboundExtensionMessage implements ISmartfoxPacket {

	public String command;
	public int roomID = -1;
	public SmartfoxPayload payload = new SmartfoxPayload();

	@Override
	public ISmartfoxPacket createInstance() {
		return new ServerboundExtensionMessage();
	}

	@Override
	public short packetID() {
		return 13;
	}

	@Override
	public void parse(SmartfoxPacketData packet) {
		command = packet.payload.getString("c");
		roomID = packet.payload.getInt("r");
		payload = packet.payload.getObject("p");
	}

	@Override
	public void build(SmartfoxPacketData packet) {
		packet.payload.setString("c", command);
		packet.payload.setInt("r", roomID);
		packet.payload.setObject("p", payload);
	}

}
