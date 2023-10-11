package org.asf.edge.mmoserver.networking.packets;

import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

/**
 * 
 * Smartfox Extension Message Interface
 * 
 * @author Sky Swimmer
 * 
 */
public interface ISmartfoxExtensionMessage {

	/**
	 * Called to create a new instance of the message type
	 * 
	 * @return IExtensionMessage instance
	 */
	public ISmartfoxExtensionMessage createInstance();

	/**
	 * Checks if the message type is valid for the given data
	 * 
	 * @param payload Message payload
	 * @return True if valid, false otherwise
	 */
	public default boolean matches(SmartfoxPayload payload) {
		return true;
	}

	/**
	 * Defines the message ID
	 * 
	 * @return Message ID
	 */
	public String messageID();

	/**
	 * Called to parse the message
	 * 
	 * @param payload Message payload
	 */
	public void parse(SmartfoxPayload payload);

	/**
	 * Called to build the message
	 * 
	 * @param payload Message payload
	 */
	public void build(SmartfoxPayload payload);

}
