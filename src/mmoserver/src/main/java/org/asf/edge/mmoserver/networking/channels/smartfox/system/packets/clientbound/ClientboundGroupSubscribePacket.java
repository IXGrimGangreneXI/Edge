package org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound;

import org.asf.edge.mmoserver.entities.smartfox.RoomInfo;
import org.asf.edge.mmoserver.io.SequenceWriter;
import org.asf.edge.mmoserver.networking.packets.ISmartfoxPacket;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPacketData;

public class ClientboundGroupSubscribePacket implements ISmartfoxPacket {

	public String groupID;
	public RoomInfo[] roomList;

	@Override
	public ISmartfoxPacket createInstance() {
		return new ClientboundGroupSubscribePacket();
	}

	@Override
	public short packetID() {
		return 15;
	}

	@Override
	public void parse(SmartfoxPacketData packet) {
	}

	@Override
	public void build(SmartfoxPacketData packet) {
		// Write group ID
		packet.payload.setString("g", groupID);

		// Write rooms
		int i = 0;
		Object[] rooms = new Object[roomList.length];
		for (RoomInfo room : roomList) {
			SequenceWriter wr = new SequenceWriter();
			room.writeTo(wr, false);
			rooms[i++] = wr.toArray();
		}
		packet.payload.setObjectArray("rl", rooms);
	}

}
