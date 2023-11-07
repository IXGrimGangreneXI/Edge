package org.asf.edge.common.events.messages;

import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.messages.PlayerMessenger;
import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Webservice message deletion event - called when messages are deleted
 * 
 * @author Sky Swimmer
 * 
 */
@EventPath("wsmessages.delete")
public class DeleteWebserviceMessageEvent extends EventObject {

	private int messageID;
	private PlayerMessenger messenger;
	private AccountObject account;
	private AccountSaveContainer save;

	public DeleteWebserviceMessageEvent(int messageID, PlayerMessenger messenger, AccountObject account,
			AccountSaveContainer save) {
		this.messageID = messageID;
		this.messenger = messenger;
		this.account = account;
		this.save = save;
	}

	@Override
	public String eventPath() {
		return "wsmessages.delete";
	}

	/**
	 * Retrieves the message ID
	 * 
	 * @return Message ID
	 */
	public int getMessageID() {
		return messageID;
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
