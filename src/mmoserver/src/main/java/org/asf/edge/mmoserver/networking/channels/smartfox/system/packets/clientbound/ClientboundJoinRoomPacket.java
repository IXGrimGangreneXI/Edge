package org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound;

import org.asf.edge.mmoserver.io.SequenceWriter;
import org.asf.edge.mmoserver.entities.smartfox.RoomInfo;
import org.asf.edge.mmoserver.networking.packets.ISmartfoxPacket;
import org.asf.edge.mmoserver.networking.sfs.SfsErrorCode;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPacketData;

public class ClientboundJoinRoomPacket implements ISmartfoxPacket {

	public RoomInfo room;

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
		// TODO
		wr = wr;
	}

}
