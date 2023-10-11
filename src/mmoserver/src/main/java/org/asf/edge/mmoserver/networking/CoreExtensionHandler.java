package org.asf.edge.mmoserver.networking;

import java.io.IOException;

import org.asf.edge.mmoserver.networking.channels.smartfox.extension.packets.serverbound.ServerboundExtensionMessage;

public interface CoreExtensionHandler {

	public boolean handle(ServerboundExtensionMessage data) throws IOException;

}
