package org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound;

import org.asf.edge.mmoserver.io.SequenceWriter;
import org.asf.edge.mmoserver.entities.smartfox.RoomInfo;
import org.asf.edge.mmoserver.entities.smartfox.SfsUser;
import org.asf.edge.mmoserver.networking.packets.ISmartfoxPacket;
import org.asf.edge.mmoserver.networking.sfs.SfsErrorCode;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPacketData;

public class ClientboundJoinRoomPacket implements ISmartfoxPacket {

	public RoomInfo room;
	public SfsUser[] users;

	public SfsErrorCode errorCode;
	public String[] errorArgs;

	@Override
	public ISmartfoxPacket createInstance() {
		return new ClientboundJoinRoomPacket();
	}

	@Override
	public short packetID() {
		return 4;
	}

	@Override
	public void parse(SmartfoxPacketData packet) {
	}

	@Override
	public void build(SmartfoxPacketData packet) {
		// Write room
		SequenceWriter wr = new SequenceWriter();
		room.writeTo(wr, false);
		packet.payload.setObjectArray("r", wr.toArray());

		// Write user list
		Object[] userLs = new Object[users.length];
		for (int i = 0; i < users.length; i++) {
			SfsUser user = users[i];
			wr = new SequenceWriter();
			user.writeTo(wr);
			userLs[i] = wr.toArray();
		}
		packet.payload.setObjectArray("ul", userLs);
	}

}
