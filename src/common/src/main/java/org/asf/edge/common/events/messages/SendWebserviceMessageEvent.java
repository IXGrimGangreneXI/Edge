package org.asf.edge.common.events.messages;

import org.asf.edge.common.entities.messages.WsMessage;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.messages.PlayerMessenger;
import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Webservice message send event - called when messages are sent from the
 * WsMessageService
 * 
 * @author Sky Swimmer
 * 
 */
@EventPath("wsmessages.send")
public class SendWebserviceMessageEvent extends EventObject {

	private WsMessage message;
	private PlayerMessenger messenger;
	private AccountObject account;
	private AccountSaveContainer save;

	public SendWebserviceMessageEvent(WsMessage message, PlayerMessenger messenger, AccountObject account,
			AccountSaveContainer save) {
		this.message = message;
		this.messenger = messenger;
		this.account = account;
		this.save = save;
	}

	@Override
	public String eventPath() {
		return "wsmessages.send";
	}

	/**
	 * Retrieves the message instance
	 * 
	 * @return WsMessage instance
	 */
	public WsMessage getMessage() {
		return message;
	}

	/**
	 * Retrieves the messenger instance
	 * 
	 * @return PlayerMessenger instance
	 */
	public PlayerMessenger getMessenger() {
		return messenger;
	}

	/**
	 * Retrieves the account instance
	 * 
	 * @return AccountObject instance
	 */
	public AccountObject getAccount() {
		return account;
	}

	/**
	 * Retrieves the save instance (MAY RETURN NULL)
	 * 
	 * @return AccountSaveContainer instance or null
	 */
	public AccountSaveContainer getSave() {
		return save;
	}

}
