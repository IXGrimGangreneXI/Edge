package org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound;

import org.asf.edge.mmoserver.entities.smartfox.RoomInfo;
import org.asf.edge.mmoserver.io.SequenceWriter;
import org.asf.edge.mmoserver.networking.packets.ISmartfoxPacket;
import org.asf.edge.mmoserver.networking.sfs.SfsErrorCode;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPacketData;

public class ClientboundLoginResponsePacket implements ISmartfoxPacket {

	public boolean hasError;
	public SfsErrorCode errorCode;
	public String[] errorArgs;

	public String zoneName;
	public RoomInfo[] roomList;

	public String userName;
	public int sessionNumericID;
	public short privilegeID;

	public short reconnectSeconds;

	@Override
	public ISmartfoxPacket createInstance() {
		return new ClientboundLoginResponsePacket();
	}

	@Override
	public short packetID() {
		return 1;
	}

	@Override
	public void parse(SmartfoxPacketData packet) {
	}

	@Override
	public void build(SmartfoxPacketData packet) {
		if (hasError) {
			packet.hasError = true;
			packet.errorCode = errorCode;
			packet.errorParams = errorArgs;
		} else {
			// Write

			// Write rooms
			int i = 0;
			Object[] rooms = new Object[roomList.length];
			for (RoomInfo room : roomList) {
				SequenceWriter wr = new SequenceWriter();
				room.writeTo(wr, false);
				rooms[i++] = wr.toArray();
			}
			packet.payload.setObjectArray("rl", rooms);

			// Write zone
			packet.payload.setString("zn", zoneName);

			// Write settings
			packet.payload.setShort("rs", reconnectSeconds);

			// Write user
			packet.payload.setShort("pi", privilegeID);
			packet.payload.setString("un", userName);
			packet.payload.setInt("id", sessionNumericID);
		}
	}

}
