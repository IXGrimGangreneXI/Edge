package org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound;

import org.asf.edge.mmoserver.io.SequenceWriter;
import org.asf.edge.mmoserver.entities.smartfox.RoomInfo;
import org.asf.edge.mmoserver.networking.packets.ISmartfoxPacket;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPacketData;

public class ClientboundRoomCreatePacket implements ISmartfoxPacket {

	public RoomInfo room;

	@Override
	public ISmartfoxPacket createInstance() {
		return new ClientboundRoomCreatePacket();
	}

	@Override
	public short packetID() {
		return 6;
	}

	@Override
	public void parse(SmartfoxPacketData packet) {
	}

	@Override
	public void build(SmartfoxPacketData packet) {
		SequenceWriter wr = new SequenceWriter();
		room.writeTo(wr, false);
		packet.payload.setObjectArray("r", wr.toArray());
	}

}
