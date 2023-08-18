package org.asf.edge.common.entities.messages;

import org.asf.edge.common.xmls.messages.MessageInfoData;

/**
 * 
 * Abstract message class
 * 
 * @author Sky Swimmer
 *
 */
public abstract class WsMessage {

	/**
	 * Retrieves the message type ID
	 * 
	 * @return Message type ID
	 */
	public abstract int messageTypeID();

	/**
	 * Creates a new message instance
	 */
	public abstract WsMessage createInstance();

	/**
	 * Serializes the message
	 * 
	 * @param messageInfo Message info xml to write to
	 */
	public abstract void serialize(MessageInfoData messageInfo);

	/**
	 * Deserializes the message
	 * 
	 * @param messageInfo Message info xml to read from
	 */
	public abstract void deserialize(MessageInfoData messageInfo);

}
