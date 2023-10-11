package org.asf.edge.mmoserver.networking.channels.smartfox.system.packets.clientbound;

import java.util.HashMap;
import java.util.Map;

import org.asf.edge.mmoserver.entities.smartfox.UserVariable;
import org.asf.edge.mmoserver.networking.packets.ISmartfoxPacket;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPacketData;

public class ClientboundSetUserVariablePacket implements ISmartfoxPacket {

	public int userID;
	public Map<String, UserVariable> variables = new HashMap<String, UserVariable>();

	@Override
	public ISmartfoxPacket createInstance() {
		return new ClientboundSetUserVariablePacket();
	}

	@Override
	public short packetID() {
		return 12;
	}

	@Override
	public void parse(SmartfoxPacketData packet) {
		userID = packet.payload.getInt("u");

		// Parse variables
		variables.clear();
		Object[] vars = packet.payload.getObjectArray("vl");
		for (Object v : vars) {
			Object[] varData = (Object[]) v;
			UserVariable varD = UserVariable.parseObjectArray(varData);
			variables.put(varD.getName(), varD);
		}
	}

	@Override
	public void build(SmartfoxPacketData packet) {
		packet.payload.setInt("u", userID);

		// Build variables
		int i = 0;
		Object[] vars = new Object[variables.size()];
		for (UserVariable var : variables.values()) {
			// Build variable
			vars[i++] = var.toObjectArray();
		}
		packet.payload.setObjectArray("vl", vars);
	}

}
