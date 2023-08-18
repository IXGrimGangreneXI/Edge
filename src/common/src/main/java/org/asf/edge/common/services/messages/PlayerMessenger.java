package org.asf.edge.common.services.messages;

import java.io.IOException;

import org.asf.edge.common.entities.messages.WsMessage;

/**
 * 
 * Player messenger - used to send and retrieve messages
 * 
 * @author Sky Swimmer
 *
 */
public abstract class PlayerMessenger {

	/**
	 * Retrieves the message queue
	 * 
	 * @param unreadOnly True to only list unread messages, false to list all
	 * @return Array of WsMessage instances
	 * @throws IOException if loading the queue fails
	 */
	public abstract WsMessage[] getQueuedMessages(boolean unreadOnly) throws IOException;

	/**
	 * Delete messages
	 * 
	 * @param messageID Message ID
	 * @throws IOException If deleting fails
	 */
	public abstract void deleteMessage(int messageID) throws IOException;

	/**
	 * Marks messages as read
	 * 
	 * @param messageID Message ID
	 * @throws IOException If marking the message as read fails
	 */
	public abstract void markMessageRead(int messageID) throws IOException;

	/**
	 * Sends messages that save until deletion
	 * 
	 * @param message Message to send
	 * @throws IOException If sending the message fails
	 */
	public abstract void sendPersistentMessage(WsMessage message) throws IOException;

	/**
	 * Sends messages that are only persistent for the current session and that
	 * delete when they are received on the client
	 * 
	 * @param message Message to send
	 * @throws IOException If sending the message fails
	 */
	public abstract void sendSessionMessage(WsMessage message) throws IOException;

}
