package org.asf.edge.mmoserver.networking.channels.extensions.messages.uservars;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.asf.edge.mmoserver.networking.packets.ISmartfoxExtensionMessage;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

public class ClientboundSetUserVarsMessage implements ISmartfoxExtensionMessage {

	public ArrayList<UserVarUpdate> varUpdates = new ArrayList<UserVarUpdate>();

	public static class UserVarUpdate {
		public int userID;
		public int roomID;
		public HashMap<String, Object> vars = new HashMap<String, Object>();
	}

	@Override
	public ISmartfoxExtensionMessage createInstance() {
		return new ClientboundSetUserVarsMessage();
	}

	@Override
	public String messageID() {
		return "SUV";
	}

	@Override
	public void parse(SmartfoxPayload payload) {
		Object[] vars = payload.getObjectArray("arr");
		for (Object obj : vars) {
			SmartfoxPayload pl = (SmartfoxPayload) obj;
			UserVarUpdate update = new UserVarUpdate();
			update.userID = pl.getInt("MID");
			update.roomID = Integer.parseInt(pl.getString("RID"));
			Map<String, Object> data = pl.toSfsObject();
			data.forEach((key, value) -> {
				if (key.equals("MID") || key.equals("RID"))
					return;
				update.vars.put(key, value);
			});
			varUpdates.add(update);
		}
	}

	@Override
	public void build(SmartfoxPayload payload) {
		ArrayList<SmartfoxPayload> objects = new ArrayList<SmartfoxPayload>();
		for (UserVarUpdate update : varUpdates) {
			SmartfoxPayload u = new SmartfoxPayload();
			u.setInt("MID", update.userID);
			u.setString("RID", Integer.toString(update.roomID));
			for (String key : update.vars.keySet())
				u.toSfsObject().put(key, update.vars.get(key));
			objects.add(u);
		}
		payload.setObjectArray("arr", objects.toArray(t -> new Object[t]));
	}

}
