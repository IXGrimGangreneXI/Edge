package org.asf.edge.mmoserver.networking.packets;

import java.io.IOException;

/**
 * 
 * Smartfox extension message handler
 * 
 * @author Sky Swimmer
 *
 * @param <T> Packet type
 */
public interface IExtensionMessageHandler<T extends ISmartfoxExtensionMessage> {

	/**
	 * Defines the message class
	 * 
	 * @return Message class instance
	 */
	public Class<T> messageClass();

	/**
	 * Checks if a message can be handled
	 * 
	 * @param message Message to handle
	 * @return True if the packet can be handled, false otherwise
	 */
	public default boolean canHandle(T message) {
		return true;
	}

	/**
	 * Called to handle messages
	 * 
	 * @param message Message to handle
	 * @param channel Extension message channel
	 * @return True if handled, false otherwise
	 */
	public boolean handle(T message, ExtensionMessageChannel channel) throws IOException;

}
