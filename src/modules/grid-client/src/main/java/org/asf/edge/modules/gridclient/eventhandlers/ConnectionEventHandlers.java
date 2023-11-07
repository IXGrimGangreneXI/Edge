package org.asf.edge.modules.gridclient.eventhandlers;

import java.io.IOException;

import org.asf.edge.common.entities.messages.defaultmessages.WsGenericMessage;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.messages.PlayerMessenger;
import org.asf.edge.common.services.messages.WsMessageService;
import org.asf.edge.modules.gridclient.GridClientModule;
import org.asf.edge.modules.gridclient.grid.events.GridClientConnectFailedEvent;
import org.asf.edge.modules.gridclient.grid.events.GridClientConnectedEvent;
import org.asf.edge.modules.gridclient.utils.GridSaveUtil;
import org.asf.nexus.events.EventListener;
import org.asf.nexus.events.IEventReceiver;

public class ConnectionEventHandlers implements IEventReceiver {

	@EventListener
	public void connectSuccess(GridClientConnectedEvent event) {
		// If an error is present, send success and unset
		if (GridClientModule.loginSystemMessage != null) {
			// Send to all players
			for (AccountObject account : AccountManager.getInstance().getOnlinePlayers()) {
				if (account.isOnline()) {
					// Send message
					PlayerMessenger messenger = WsMessageService.getInstance().getMessengerFor(account);
					WsGenericMessage msg = new WsGenericMessage();
					msg.rawObject.typeID = 3;
					msg.rawObject.messageContentMembers = "Connection to the Multiplayer Grid was restored! Multiplayer features should begin working again.";
					msg.rawObject.messageContentNonMembers = msg.rawObject.messageContentMembers;
					try {
						messenger.sendSessionMessage(msg);
					} catch (IOException e) {
					}
				}
			}
			GridClientModule.loginSystemMessage = null;
		}

		// Update all user saves
		AccountManager manager = null;
		try {
			manager = AccountManager.getInstance();
		} catch (Exception e) {
		}
		if (manager != null) {
			for (AccountObject account : manager.getOnlinePlayers()) {
				// Get saves
				for (String svID : account.getSaveIDs()) {
					AccountSaveContainer save = account.getSave(svID);
					if (save != null) {
						// Update
						GridSaveUtil.updateGridSaveID(save);
					}
				}
			}
		}
	}

	@EventListener
	public void connectFailure(GridClientConnectFailedEvent event) {
		// Handle failure
		switch (event.getDisconnectReason().getReason()) {

		// No server
		case "connect.error.connectfailure.noservers": {
			// No servers available
			if (GridClientModule.loginSystemMessage == null)
				GridClientModule.sendGridErrorMessageToPlayers(
						"A connection to the Multiplayer Grid could not be made, non-local multiplayer features are presently unavailable.\n\nEdge will attempt to connect in the background until a connection is made.");
			break;
		}

		// Game ID mismatch
		case "connect.error.connectfailure.gamemismatch": {
			// Update required
			GridClientModule.sendGridErrorMessageToPlayers(
					"Unable to connect to the Multiplayer Grid, there was a game ID mismatch, you may need to update the server.");
			break;
		}

		// Version mismatch
		case "connect.error.connectfailure.versionmismatch": {
			// Update required
			GridClientModule.sendGridErrorMessageToPlayers(
					"Unable to connect to the Multiplayer Grid as there was a version mismatch, you may need to update the server.");
			break;
		}

		// Banned for unspecified reasons
		case "disconnect.loginfailure.banned.undefined": {
			// Player is banned
			GridClientModule.sendGridErrorMessageToPlayers(
					"The Grid account used by this server has been permanently banned, you will not be able to use non-local multiplayer features.");
			break;
		}

		// Banned
		case "disconnect.loginfailure.banned": {
			// Player is banned with reason
			GridClientModule.sendGridErrorMessageToPlayers(
					"The Grid account used by this server has been permanently banned, you will not be able to use non-local multiplayer features.\n"
							+ "\n" //
							+ "Ban reason: " + event.getDisconnectReason().getReasonArgs()[0] //
			);
			break;
		}

		// Temporarily banned for unspecified reasons
		case "disconnect.loginfailure.tempbanned.undefined": {
			// Player is banned
			GridClientModule.sendGridErrorMessageToPlayers(
					"The Grid account used by this server has been banned, you will not be able to use non-local multiplayer features.\n"
							+ "\n" //
							+ "You will be able to play online again at: "
							+ event.getDisconnectReason().getReasonArgs()[0] + " "
							+ event.getDisconnectReason().getReasonArgs()[1] + " UTC." //
			);
			break;
		}

		// Temporarily banned
		case "disconnect.loginfailure.tempbanned": {
			// Player is banned with reason
			GridClientModule.sendGridErrorMessageToPlayers(
					"The Grid account used by this server has been banned, you will not be able to use non-local multiplayer features.\n"
							+ "\n" //
							+ "Ban reason: " + event.getDisconnectReason().getReasonArgs()[2] + "\n\n" //
							+ "You will be able to play online again at: "
							+ event.getDisconnectReason().getReasonArgs()[0] + " "
							+ event.getDisconnectReason().getReasonArgs()[1] + " UTC." //
			);
			break;
		}

		}
	}

}
