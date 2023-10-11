package org.asf.edge.mmoserver.networking.channels.extensions.messages.uservars;

import java.util.HashMap;
import java.util.Map;

import org.asf.edge.mmoserver.networking.channels.extensions.ISodClientExtensionMessage;
import org.asf.edge.mmoserver.networking.packets.ISmartfoxExtensionMessage;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

public class ServerboundSetPositionalVarsMessage implements ISodClientExtensionMessage {

	public HashMap<String, Object> vars = new HashMap<String, Object>();

	@Override
	public ISmartfoxExtensionMessage createInstance() {
		return new ServerboundSetPositionalVarsMessage();
	}

	@Override
	public String extensionName() {
		return "we";
	}

	@Override
	public String messageID() {
		return "SPV";
	}

	@Override
	public void parse(SmartfoxPayload payload) {
		Map<String, Object> objs = payload.toSfsObject();
		for (String key : payload.getKeys()) {
			if (key.equals("en"))
				continue;
			vars.put(key, objs.get(key));
		}
	}

	@Override
	public void build(SmartfoxPayload payload) {
		Map<String, Object> objs = payload.toSfsObject();
		for (String key : vars.keySet()) {
			if (key.equals("en"))
				continue;
			objs.put(key, vars.get(key));
		}
	}

}
