package org.asf.edge.mmoserver.networking.channels.extensions;

import org.asf.edge.mmoserver.networking.packets.ISmartfoxExtensionMessage;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

/**
 * 
 * Client extension message
 * 
 * @author Sky Swimmer
 * 
 */
public interface ISodClientExtensionMessage extends ISmartfoxExtensionMessage {

	/**
	 * Defines the extension name
	 * 
	 * @return Extension name string
	 */
	public String extensionName();

	@Override
	public default boolean matches(SmartfoxPayload payload) {
		if (!payload.has("en") || !payload.getString("en").equals(extensionName()))
			return false;
		return true;
	}

}
