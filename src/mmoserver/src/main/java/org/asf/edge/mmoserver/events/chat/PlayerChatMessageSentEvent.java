package org.asf.edge.mmoserver.events.chat;

import org.asf.edge.mmoserver.entities.player.PlayerInfo;
import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;

/**
 * 
 * Player chat message event
 * 
 * @author Sky Swimmer
 * 
 */
@EventPath("players.chat.sendmessage")
public class PlayerChatMessageSentEvent extends EventObject {

	private PlayerInfo player;
	private String message;
	private String clanID;

	private boolean cancelled;

	public PlayerChatMessageSentEvent(PlayerInfo player, String message, String clanID) {
		this.player = player;
		this.message = message;
		this.clanID = clanID;
	}

	@Override
	public String eventPath() {
		return "players.chat.sendmessage";
	}

	/**
	 * Checks if the message is cancelled
	 * 
	 * @return True if cancelled, false otherwise
	 */
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * Cancels the chat message
	 */
	public void cancel() {
		cancelled = true;
		setHandled();
	}

	/**
	 * Retrieves the chat message
	 * 
	 * @return Chat message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Retrieves the clan ID
	 * 
	 * @return Chat clan ID
	 */
	public String getClanID() {
		return clanID;
	}

	/**
	 * Checks if the message is sent in clan chat
	 * 
	 * @return True if in clan chat, false otherwise
	 */
	public boolean isInClanChat() {
		return clanID != null;
	}

	/**
	 * Retrieves the player object
	 * 
	 * @return PlayerInfo instance
	 */
	public PlayerInfo getPlayer() {
		return player;
	}

}
