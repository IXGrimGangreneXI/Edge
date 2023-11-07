package org.asf.edge.common.services.messages;

import java.util.HashMap;
import java.util.Map;

import org.asf.edge.common.entities.messages.WsMessage;
import org.asf.edge.common.entities.messages.defaultmessages.WsGenericMessage;
import org.asf.edge.common.entities.messages.defaultmessages.WsPluginMessage;
import org.asf.edge.common.entities.messages.defaultmessages.WsRankMessage;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.xmls.messages.MessageInfoData;
import org.asf.nexus.common.services.AbstractService;
import org.asf.nexus.common.services.ServiceManager;

/**
 * 
 * Message Service - used to send system messages to the client over HTTP
 * 
 * @author Sky Swimmer
 *
 */
public abstract class WsMessageService extends AbstractService {
	private HashMap<Integer, WsMessage> messages = new HashMap<Integer, WsMessage>(Map.of(

			// Rank message
			4, new WsRankMessage(),

			// Plugin message
			36, new WsPluginMessage()

	));

	/**
	 * Retrieves the active messaging service
	 * 
	 * @return WsMessageService instance
	 */
	public static WsMessageService getInstance() {
		return ServiceManager.getService(WsMessageService.class);
	}

	/**
	 * Registers message types
	 * 
	 * @param typeID  Type ID
	 * @param message WsMessage implementation
	 */
	public void registerMessageType(int typeID, WsMessage message) {
		if (!messages.containsKey(typeID))
			messages.put(typeID, message);
	}

	/**
	 * Deserializes messages
	 * 
	 * @param message Message information object
	 * @return WsMessage instance
	 */
	public WsMessage deserializeMessage(MessageInfoData message) {
		// Find
		if (messages.containsKey(message.typeID)) {
			// Found
			WsMessage msg = messages.get(message.typeID);
			msg.deserialize(message);
			return msg;
		}

		// Return generic
		WsMessage msg = new WsGenericMessage();
		msg.deserialize(message);
		return msg;
	}

	/**
	 * Retrieves the player messenger instance for a specific account
	 * 
	 * @param account Account object
	 * @param save    Account save
	 * @return PlayerMessenger instance or null
	 */
	public abstract PlayerMessenger getMessengerFor(AccountObject account, AccountSaveContainer save);

	/**
	 * Retrieves the player messenger instance for a specific account
	 * 
	 * @param account Account object
	 * @return PlayerMessenger instance or null
	 */
	public abstract PlayerMessenger getMessengerFor(AccountObject account);

}
