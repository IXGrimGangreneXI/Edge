package org.asf.edge.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.entities.messages.defaultmessages.WsGenericMessage;
import org.asf.edge.common.events.updates.ServerUpdateCompletionEvent;
import org.asf.edge.common.events.updates.ServerUpdateEvent;
import org.asf.edge.common.events.updates.UpdateCancelEvent;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.messages.WsMessageService;
import org.asf.edge.modules.eventbus.EventBus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 
 * Sentinel update management system
 * 
 * @author Sky Swimmer
 *
 */
public class SentinelUpdateManager {

	private static Logger logger;

	private static String currentVersion = null;

	private static boolean cancelUpdate = false;
	private static boolean updating = false;

	private static String nextVersion = null;
	private static String listURL = null;

	private static long updateRestartTimestamp;

	/**
	 * Checks if the Sentinel Update Manager is enabled
	 * 
	 * @return True if enabled, false otherwise
	 */
	public static boolean isEnabled() {
		return currentVersion != null;
	}

	/**
	 * Retrieves the current Sentinel software version
	 * 
	 * @return Version string or null if not enabled
	 */
	public static String getCurrentSoftwareVersion() {
		return currentVersion;
	}

	/**
	 * Initializes the update system
	 * 
	 * @param listURL        Update list URL
	 * @param currentVersion Current version
	 * @throws IOException If initializing fails
	 */
	public static void init(String listURL, String currentVersion) throws IOException {
		SentinelUpdateManager.listURL = listURL;
		SentinelUpdateManager.currentVersion = currentVersion;
		logger = LogManager.getLogger("Updater");

		// Parse properties
		HashMap<String, String> properties = new HashMap<String, String>();
		if (new File("updater.conf").exists()) {
			for (String line : Files.readAllLines(Path.of("updater.conf"))) {
				if (line.isEmpty() || line.startsWith("#"))
					continue;
				String key = line;
				String value = "";
				if (key.contains("=")) {
					value = key.substring(key.indexOf("=") + 1);
					key = key.substring(0, key.indexOf("="));
				}
				properties.put(key, value);
			}
		}
		// Start the automatic update thread
		Thread updater = new Thread(() -> {
			while (true) {
				// Run every 2 minutes
				try {
					Thread.sleep(120000);
				} catch (InterruptedException e) {
				}

				// Check for updates
				if (shouldUpdate()) {
					runUpdater(60);
					return;
				}
			}
		}, "Automatic update thread");
		updater.setDaemon(true);
		updater.start();

		// Run
		if (shouldUpdate()) {
			// Dispatch event
			EventBus.getInstance().dispatchEvent(new ServerUpdateEvent(nextVersion, -1));

			// Dispatch completion event
			EventBus.getInstance().dispatchEvent(new ServerUpdateCompletionEvent(nextVersion));

			// Exit server
			logger.info("Restarting server via exit code 238 for update...");
			System.exit(238);
		}
	}

	/**
	 * Cancels the update
	 * 
	 * @return True if successful, false otherwise
	 */
	public static boolean cancelUpdate() {
		if (!isEnabled())
			return false;
		if (updating) {
			cancelUpdate = true;
			nextVersion = null;
			updateRestartTimestamp = -1;
			EventBus.getInstance().dispatchEvent(new UpdateCancelEvent());

			// Notify everyone
			AccountManager.getInstance().runForAllAccounts(t -> {
				if (t.isOnline()) {
					// Send message
					WsGenericMessage msg = new WsGenericMessage();
					msg.rawObject.typeID = 3;
					msg.rawObject.messageContentMembers = "The Sentinel update has been cancelled by an administrator.";
					msg.rawObject.messageContentNonMembers = msg.rawObject.messageContentMembers;
					try {
						WsMessageService.getInstance().getMessengerFor(t).sendSessionMessage(msg);
					} catch (IOException e) {
					}
				}
				return true;
			});
			logger.info("Update cancelled.");
			return true;
		} else
			return false;
	}

	/**
	 * Runs the update timer (kicks players after a specified time for server
	 * reboot)
	 * 
	 * @param mins Time given before restart in minutes
	 * @return True if successful, false otherwise
	 */
	public static boolean runUpdater(int mins) {
		// Run timer
		if (!cancelUpdate) {
			updating = true;

			// Dispatch update event
			EventBus.getInstance().dispatchEvent(new ServerUpdateEvent(nextVersion, mins));

			// Run countdown
			updateRestartTimestamp = System.currentTimeMillis() + (mins * 60 * 1000);
			Thread th = new Thread(() -> {
				while (!cancelUpdate) {
					// Update shutdown if countdown reaches zero
					int remainingTime = (int) ((System.currentTimeMillis() - updateRestartTimestamp) / 1000 / 60);
					if (remainingTime <= 0 || AccountManager.getInstance().getOnlinePlayerIDs().length == 0) {
						updateShutdown();
						cancelUpdate = false;
						return;
					}

					// Wait
					if (cancelUpdate)
						break;
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				}
				cancelUpdate = false;
				updating = false;
			});
			th.setName("Update Thread");
			th.start();

			return true;
		}

		return false;
	}

	/**
	 * Shuts down the server with a update message
	 */
	public static void updateShutdown() {
		// Dispatch event if the update was instant
		if (!updating) {
			EventBus.getInstance().dispatchEvent(new ServerUpdateEvent(nextVersion, -1));
		}

		// Warn everyone
		logger.info("Restarting server via exit code 238 for update...");
		AccountManager.getInstance().runForAllAccounts(t -> {
			if (t.isOnline()) {
				// Send message
				WsGenericMessage msg = new WsGenericMessage();
				msg.rawObject.typeID = 3;
				msg.rawObject.messageContentMembers = "The Sentinel Launcher is being restarted to apply the server update! The emulation software will be offline for a few minutes and may cause errors now!";
				msg.rawObject.messageContentNonMembers = msg.rawObject.messageContentMembers;
				try {
					WsMessageService.getInstance().getMessengerFor(t).sendSessionMessage(msg);
				} catch (IOException e) {
				}
			}
			return true;
		});

		// Dispatch completion event
		EventBus.getInstance().dispatchEvent(new ServerUpdateCompletionEvent(nextVersion));

		// Shut server down
		System.exit(238);
	}

	private static boolean shouldUpdate() {
		// Check for updates
		logger.info("Checking for updates...");
		try {
			// Download list
			InputStream updateLog = new URL(listURL).openStream();
			String updateInfo = new String(updateLog.readAllBytes(), "UTF-8").trim();
			JsonObject versionList = JsonParser.parseString(updateInfo).getAsJsonObject();
			updateLog.close();

			// Check version
			if (!currentVersion.equals(versionList.get("latest").getAsString())) {
				// Download the update list
				logger.info(
						"Sentinel software update available, new version: " + versionList.get("latest").getAsString());

				// Save new version in memory
				nextVersion = versionList.get("latest").getAsString();

				// Update available
				return true;
			}
		} catch (IOException e) {
		}

		// No update available
		return false;
	}

	/**
	 * Sends a update warning to players if updating
	 * 
	 * @param account Account to send the warning to
	 */
	public static void warnPlayerIfUpdating(AccountObject account) {
		// Check
		if (updating && updateRestartTimestamp != -1) {
			// Create message
			int remainingTime = (int) ((System.currentTimeMillis() - updateRestartTimestamp) / 1000 / 60);
			SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a", Locale.US);
			fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
			String message;
			switch (remainingTime) {

			case 10:
			case 5:
			case 3:
			case 1:
				message = "The Sentinel Launcher is presently being updated! Game and Launcher restart is imminent!\n\nWhen the launcher restarts, the game will be left open unless there are client mod updates, note that there may be errors until the update finishes and restarts the servers.";
				break;

			case 0:
				message = "The Sentinel Launcher is being restarted to apply the server update! The emulation software will be offline for a few minutes and may cause errors now!";
				break;

			default:
				if (nextVersion != null)
					message = "Project Edge has a update available! Launcher restart is scheduled at "
							+ fmt.format(new Date(updateRestartTimestamp)) + " UTC. Updating to " + nextVersion + "."
							+ "\n\nWhen the launcher restarts, the game will be left open unless there are client mod updates, note that there may be errors until the update finishes and restarts the servers.";
				else
					message = "Project Edge has a update available! Launcher restart is scheduled at "
							+ fmt.format(new Date(updateRestartTimestamp))
							+ " UTC.\n\nWhen the launcher restarts, the game will be left open unless there are client mod updates, note that there may be errors until the update finishes and restarts the servers.";
				break;

			}

			// Send message
			if (message != null) {
				// Warn
				// Send message
				WsGenericMessage msg = new WsGenericMessage();
				msg.rawObject.typeID = 3;
				msg.rawObject.messageContentMembers = message;
				msg.rawObject.messageContentNonMembers = msg.rawObject.messageContentMembers;
				try {
					WsMessageService.getInstance().getMessengerFor(account).sendSessionMessage(msg);
				} catch (IOException e) {
				}

				// Send over MMO server
				// TODO: mmo support
			}
		}
	}
}
