package org.asf.edge.modules.gridclient.utils;

import java.io.IOException;
import java.util.stream.Stream;

import org.asf.edge.common.entities.messages.defaultmessages.WsGenericMessage;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.commondata.CommonDataContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.services.messages.PlayerMessenger;
import org.asf.edge.common.services.messages.WsMessageService;
import org.asf.edge.modules.gridclient.grid.GridClient;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class GridSaveUtil {

	/**
	 * Retrieves the grid save ID
	 * 
	 * @param save EDGE save to retrieve the grid save of
	 * @return Grid save ID or null
	 */
	public static String getGridSaveID(AccountSaveContainer save) {
		// Get data
		try {
			AccountDataContainer data = save.getSaveData();
			if (data.entryExists("gridSaveID"))
				return data.getEntry("gridSaveID").getAsString();
		} catch (IOException e) {
		}
		return updateGridSaveID(save);
	}

	/**
	 * Updates the grid save ID
	 * 
	 * @param save EDGE save to retrieve the grid save of
	 * @return Grid save ID or null
	 */
	public static String updateGridSaveID(AccountSaveContainer save) {
		// Get save ID
		String gridSaveID = null;
		boolean updated = false;
		try {
			AccountDataContainer data = save.getSaveData();
			if (data.entryExists("gridSaveID"))
				gridSaveID = data.getEntry("gridSaveID").getAsString();
		} catch (IOException e) {
		}

		// Verify save
		if (gridSaveID != null && GridClient.getLoginManager().getSession() != null) {
			try {
				// Contact API
				JsonObject payload = new JsonObject();
				payload.addProperty("saveID", gridSaveID);
				JsonObject response = GridClient.sendGridApiRequest("grid/accounts/getSaveDetails",
						GridClient.getLoginManager().getSession().getGameSessionToken(), payload, true);

				// Verify response
				if (response.has("error") && response.get("error").getAsString().equals("save_not_found")) {
					// Save not found
					gridSaveID = null;
				} else {
					// Check username
					String name = response.get("saveUsername").getAsString();
					if (!name.equals(save.getUsername())) {
						// Try update
						updateGridSaveUsername(save, name);
					}
				}
			} catch (IOException e) {
			}
		}

		// Create save if needed
		if (gridSaveID == null) {
			try {
				// Contact API
				JsonObject payload = new JsonObject();
				payload.addProperty("saveUsername", save.getUsername());
				JsonObject response = GridClient.sendGridApiRequest("grid/accounts/createSave",
						GridClient.getLoginManager().getSession().getGameSessionToken(), payload, true);

				// Verify response
				if (response.has("error")) {
					// Handle error
					String error = response.get("error").getAsString();
					boolean invalidUsername = false;
					switch (error) {

					// Username invalid
					case "invalid_username": {
						invalidUsername = true;
						break;
					}

					// Username filtered
					case "inappropriate_username": {
						invalidUsername = true;
						break;
					}

					// Username in use
					case "username_in_use": {
						invalidUsername = true;
						break;
					}

					// Server error
					case "save_creation_failure": {
						break;
					}

					}

					// Check
					if (invalidUsername) {
						// Prepare message
						String message = "Viking '" + save.getUsername()
								+ "' could not be synced to the multiplayer grid as its name is invalid or already in use."
								+ "\n\nPlease change the viking name, when the name of the viking is changed,"
								+ " the grid will automatically resume functionality.";

						// Send message if needed
						PlayerMessenger messenger = WsMessageService.getInstance().getMessengerFor(save.getAccount(),
								save);

						// Check messages
						if (!Stream.of(messenger.getQueuedMessages(false)).anyMatch(t -> {
							// Check message
							if (t instanceof WsGenericMessage) {
								WsGenericMessage genericMsg = (WsGenericMessage) t;
								if (genericMsg.rawObject.typeID == 3
										&& genericMsg.rawObject.messageContentMembers != null) {
									// Check content
									if (genericMsg.rawObject.messageContentMembers.equals(message))
										return true;
								}
							}

							// No match
							return false;
						})) {
							// Send message
							WsGenericMessage msg = new WsGenericMessage();
							msg.rawObject.typeID = 3;
							msg.rawObject.messageContentMembers = message;
							msg.rawObject.messageContentNonMembers = msg.rawObject.messageContentMembers;
							try {
								messenger.sendSessionMessage(msg);
							} catch (IOException e) {
							}
						}
					}
				} else {
					// Success
					gridSaveID = response.get("saveId").getAsString();
					updated = true;
				}
			} catch (IOException e) {
			}
		}

		// Save
		if (gridSaveID != null && updated) {
			try {
				AccountDataContainer data = save.getSaveData();
				data.setEntry("gridSaveID", new JsonPrimitive(gridSaveID));
			} catch (IOException e) {
			}
		}

		// Return ID
		return gridSaveID;
	}

	/**
	 * Deletes grid saves of players
	 * 
	 * @param save Save container to delete from the grid
	 */
	public static void deleteGridSaveOf(AccountSaveContainer save) {
		try {
			AccountDataContainer data = save.getSaveData();
			if (data.entryExists("gridSaveID")) {
				// Delete save
				String gridSaveID = data.getEntry("gridSaveID").getAsString();

				try {
					// Contact API
					JsonObject payload = new JsonObject();
					payload.addProperty("saveID", gridSaveID);
					JsonObject response = GridClient.sendGridApiRequest("grid/accounts/getSaveDetails",
							GridClient.getLoginManager().getSession().getGameSessionToken(), payload, true);

					// Verify response
					if (!response.has("deleted") || !response.get("deleted").getAsBoolean()) {
						throw new IOException();
					}
				} catch (IOException e) {
					// Deletion failure

					// Schedule for future
					// Get common data container
					CommonDataContainer cont = CommonDataManager.getInstance().getContainer("MULTIPLAYERGRID");
					cont = cont.getChildContainer("GRID_SAVES_TO_REMOVE");

					// Add
					cont.setEntry("SV-" + gridSaveID, new JsonPrimitive(true));
				}
			}
		} catch (IOException e) {
		}
	}

	/**
	 * Updates the grid save username
	 * 
	 * @param save        Save container to update in the grid
	 * @param newUsername New username
	 */
	public static void updateGridSaveUsername(AccountSaveContainer save, String newUsername) {
		// Get save ID
		String gridSaveID = getGridSaveID(save);

		// Check ID
		if (gridSaveID != null && GridClient.getLoginManager().getSession() != null) {
			try {
				// Contact API
				JsonObject payload = new JsonObject();
				payload.addProperty("saveID", gridSaveID);
				payload.addProperty("saveUsername", newUsername);
				JsonObject response = GridClient.sendGridApiRequest("grid/accounts/updateSave",
						GridClient.getLoginManager().getSession().getGameSessionToken(), payload, true);

				// Verify response
				if (response.has("error")) {
					// Handle error
					String error = response.get("error").getAsString();
					boolean invalidUsername = false;
					switch (error) {

					// Username invalid
					case "invalid_username": {
						invalidUsername = true;
						break;
					}

					// Username filtered
					case "inappropriate_username": {
						invalidUsername = true;
						break;
					}

					// Username in use
					case "username_in_use": {
						invalidUsername = true;
						break;
					}

					// Server error
					case "server_error": {
						break;
					}

					}

					// Check
					if (invalidUsername) {
						// Prepare message
						String message = "The username of Viking '" + save.getUsername()
								+ "' could not be synced to the multiplayer grid as its name is invalid or already in use."
								+ "\n\nPlease change the viking name, when the name of the viking is changed,"
								+ " the grid will automatically update the username when its possible.";

						// Send message if needed
						PlayerMessenger messenger = WsMessageService.getInstance().getMessengerFor(save.getAccount(),
								save);

						// Check messages
						if (!Stream.of(messenger.getQueuedMessages(false)).anyMatch(t -> {
							// Check message
							if (t instanceof WsGenericMessage) {
								WsGenericMessage genericMsg = (WsGenericMessage) t;
								if (genericMsg.rawObject.typeID == 3
										&& genericMsg.rawObject.messageContentMembers != null) {
									// Check content
									if (genericMsg.rawObject.messageContentMembers.equals(message))
										return true;
								}
							}

							// No match
							return false;
						})) {
							// Send message
							WsGenericMessage msg = new WsGenericMessage();
							msg.rawObject.typeID = 3;
							msg.rawObject.messageContentMembers = message;
							msg.rawObject.messageContentNonMembers = msg.rawObject.messageContentMembers;
							try {
								messenger.sendSessionMessage(msg);
							} catch (IOException e) {
							}
						}
					}
				}
			} catch (IOException e) {
			}
		}
	}

}
