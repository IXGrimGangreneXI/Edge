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

/**
 * 
 * Common update system
 * 
 * @author Sky Swimmer
 *
 */
public class CommonUpdater {

	private static Logger logger;

	private static String currentVersion = null;
	private static boolean cancelUpdate = false;
	private static boolean updating = false;
	private static String nextVersion = null;
	private static String serverURL = null;
	private static int updateTimeRemaining = -1;

	private static File updaterJar;

	/**
	 * Retrieves the current server version
	 * 
	 * @return Version string
	 */
	public static String getCurrentVersion() {
		return currentVersion;
	}

	/**
	 * Initializes the update system
	 * 
	 * @param serverName     Server name
	 * @param defaultChannel Default updater channel
	 * @param currentVersion Current version
	 * @throws IOException If initializing fials
	 */
	public static void init(String serverName, String defaultChannel, String currentVersion, File updaterJar)
			throws IOException {
		CommonUpdater.currentVersion = currentVersion;
		CommonUpdater.updaterJar = updaterJar;
		logger = LogManager.getLogger("Updater");

		// Defaults
		CommonUpdater.serverURL = "https://projectedge.net/updates/" + serverName;
		String updateChannel = defaultChannel;
		boolean disableUpdater = true;

		// Create config
		if (!new File("updater.conf").exists()) {
			// Write
			Files.writeString(Path.of("updater.conf"), ""//

					+ "# Updater URL\n" //
					+ "url=https://projectedge.net/updates/" + serverName + "\n" //
					+ "\n" //
					+ "# Update channel, valid values are lts, stable, unstable and dev\n" //
					+ "# channel=" + defaultChannel + "\n" //
					+ "\n" //
					+ "# Runtime automatic updater, true to enable updating while the server runs, false otherwise\n"
					+ "runtime-auto-update=true\n" //
					+ "\n" //
					+ "# Update timer length\n" //
					+ "runtime-update-timer-length=120\n" //
					+ "\n" //
					+ "# Disables the updater\n" //
					+ "disable=true\n"

			);
		}

		// Parse properties
		HashMap<String, String> properties = new HashMap<String, String>();
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

		// Load channel
		updateChannel = properties.getOrDefault("channel", updateChannel);

		// Check if disabled
		disableUpdater = Boolean.parseBoolean(properties.getOrDefault("disable", "true"));

		// Load url
		serverURL = properties.getOrDefault("url", serverURL);

		// Check if automatic updating is enabled
		if (Boolean.parseBoolean(properties.getOrDefault("runtime-auto-update", "false"))
				&& (System.getProperty("debugMode") == null)) {
			int mins = Integer.parseInt(properties.getOrDefault("runtime-update-timer-length", "120"));

			// Start the automatic update thread
			final String channel = updateChannel;
			Thread updater = new Thread(() -> {
				while (true) {
					// Run every 2 minutes
					try {
						Thread.sleep(120000);
					} catch (InterruptedException e) {
					}

					// Check for updates
					if (shouldUpdate(channel)) {
						runUpdater(mins);
						return;
					}
				}
			}, "Automatic update thread");
			updater.setDaemon(true);
			updater.start();
		}

		// Updater
		if (!disableUpdater && (System.getProperty("debugMode") == null)) {
			// Check for updates
			if (shouldUpdate(updateChannel)) {
				// Dispatch event
				EventBus.getInstance().dispatchEvent(new ServerUpdateEvent(nextVersion, -1));

				// Dispatch completion event
				EventBus.getInstance().dispatchEvent(new ServerUpdateCompletionEvent(nextVersion));

				// Exit server
				System.exit(0);
			}
		}
	}

	/**
	 * Cancels the update
	 * 
	 * @return True if successful, false otherwise
	 */
	public static boolean cancelUpdate() {
		if (updating) {
			cancelUpdate = true;
			nextVersion = null;
			updateTimeRemaining = -1;
			EventBus.getInstance().dispatchEvent(new UpdateCancelEvent());

			// Notify everyone
			AccountManager.getInstance().runForAllAccounts(t -> {
				if (t.isOnline()) {
					// Send message
					WsGenericMessage msg = new WsGenericMessage();
					msg.rawObject.typeID = 3;
					msg.rawObject.messageContentMembers = "The server update has been cancelled by an administrator.";
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
			final int minutes = mins;
			Thread th = new Thread(() -> {
				int remaining = minutes;
				updateTimeRemaining = remaining;
				while (!cancelUpdate) {
					// Update shutdown if countdown reaches zero
					if (remaining <= 0 || AccountManager.getInstance().getOnlinePlayerIDs().length == 0) {
						updateShutdown();
						cancelUpdate = false;
						return;
					}

					// Wait
					for (int i = 0; i < 60; i++) {
						if (cancelUpdate)
							break;
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
						}
					}

					// Count down
					remaining--;
					updateTimeRemaining = remaining;
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
		logger.info("Restarting server...");
		AccountManager.getInstance().runForAllAccounts(t -> {
			if (t.isOnline()) {
				// Send message
				WsGenericMessage msg = new WsGenericMessage();
				msg.rawObject.typeID = 3;
				msg.rawObject.messageContentMembers = "Edge servers are restarting! They will be offline for a few seconds and may cause errors now!";
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
		if (!new File("update.list").exists())
			System.exit(237);
		else
			System.exit(0);
	}

	private static boolean shouldUpdate(String channel) {
		// Check for updates
		logger.info("Checking for updates...");
		try {
			InputStream updateLog = new URL(serverURL + (serverURL.endsWith("/") ? "" : "/") + channel + "/update.info")
					.openStream();
			String update = new String(updateLog.readAllBytes(), "UTF-8").trim();
			updateLog.close();

			if (!currentVersion.equals(update)) {
				// Download the update list
				logger.info("Update available, new version: " + update);
				logger.info("Preparing to update Edge...");
				InputStream strm = new URL(serverURL + (serverURL.endsWith("/") ? "" : "/") + channel + "/" + update + "/update.list")
						.openStream();
				String fileList = new String(strm.readAllBytes(), "UTF-8").trim();
				strm.close();

				// Parse the file list (newline-separated)
				String downloadList = "";
				for (String file : fileList.split("\n")) {
					if (!file.isEmpty()) {
						downloadList += file + "=" + serverURL + (serverURL.endsWith("/") ? "" : "/") + channel + "/"
								+ update + "/" + file + "\n";
					}
				}

				// Save the file, copy jar and run the shutdown timer
				Files.writeString(Path.of("update.list"), downloadList);
				if (!new File("updater.jar").exists())
					Files.copy(updaterJar.toPath(), Path.of("updater.jar"));

				// Save new version in memory
				nextVersion = update;

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
		if (updating && updateTimeRemaining != -1) {
			// Create message
			SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a", Locale.US);
			fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
			String message;
			switch (updateTimeRemaining) {

			case 10:
			case 5:
			case 3:
			case 1:
				message = "The Edge servers are presently being updated! They will be temporarily offline for a few seconds and may cause errors! Restart is imminent!";
				break;

			case 0:
				message = "Edge servers are restarting! They will be offline for a few seconds and may cause errors now!";
				break;

			default:
				message = "The Edge servers are gonna be updated! They will be temporarily offline and may cause errors soon! They are scheduled to go down at "
						+ fmt.format(new Date(System.currentTimeMillis() + (updateTimeRemaining * 60 * 1000))) + " UTC";
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
