package org.asf.edge.modules.gridclient;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import javax.swing.JOptionPane;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.common.entities.messages.defaultmessages.WsGenericMessage;
import org.asf.edge.common.events.accounts.AccountAuthenticatedEvent;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.config.ConfigProviderService;
import org.asf.edge.common.services.messages.PlayerMessenger;
import org.asf.edge.common.services.messages.WsMessageService;
import org.asf.edge.modules.IEdgeModule;
import org.asf.edge.modules.eventbus.EventBus;
import org.asf.edge.modules.eventbus.EventListener;
import org.asf.edge.modules.gridclient.grid.events.GridClientAuthenticationDeferredEvent;
import org.asf.edge.modules.gridclient.grid.events.GridClientAuthenticationFailureEvent;
import org.asf.edge.modules.gridclient.grid.events.GridClientAuthenticationSuccessEvent;
import org.asf.edge.modules.gridclient.gui.LoginWindow;
import org.asf.edge.modules.gridclient.gui.LoginWindow.LoginResult;
import org.asf.edge.modules.gridclient.gui.RegistrationWindow.RegistrationResult;
import org.asf.edge.modules.gridclient.phoenix.PhoenixEnvironment;
import org.asf.edge.modules.gridclient.phoenix.auth.LoginManager;
import org.asf.edge.modules.gridclient.phoenix.auth.PhoenixSession;
import org.asf.edge.modules.gridclient.phoenix.events.PhoenixGameInvalidatedEvent;
import org.asf.edge.modules.gridclient.phoenix.events.SessionRefreshFailureEvent;
import org.asf.edge.modules.gridclient.phoenix.serverlist.ServerInstance;
import org.asf.edge.modules.gridclient.eventhandlers.ConnectionEventHandlers;
import org.asf.edge.modules.gridclient.grid.GridClient;
import org.asf.edge.modules.gridclient.grid.SessionLockStatus;

import com.google.gson.JsonObject;

public class GridClientModule implements IEdgeModule {
	public static final String GRID_API_VERSION = "1.0.0.A1";
	public static final String GRID_SOFTWARE_ID = "sod-phoenix-grid";

	private Scanner sc = new Scanner(System.in);
	private Logger logger = LogManager.getLogger("grid-client");
	public static String loginSystemMessage;

	@Override
	public String moduleID() {
		return "edge-grid";
	}

	@Override
	public String version() {
		return "1.0.0.A1";
	}

	@Override
	public void init() {
		// Read config
		JsonObject defaultConfig = new JsonObject();
		defaultConfig.addProperty("enabled", true);
		defaultConfig.addProperty("gridApiServer", "https://grid.sentinel.projectedge.net:16718/");
		JsonObject config;
		try {
			logger.info("Loading configuration...");
			config = ConfigProviderService.getInstance().loadConfig("moduleconfigs", "gridclient", defaultConfig);
		} catch (IOException e) {
			logger.error("Failed to load Grid client configuration!", e);
			return;
		}
		PhoenixEnvironment.defaultAPIServer = config.get("gridApiServer").getAsString();

		// Check
		if (!config.get("enabled").getAsBoolean()) {
			// Not enabled, lets not start
			return;
		}

		// Read session
		String lastSessionRefreshToken = null;
		String lastUsername = null;
		if (ConfigProviderService.getInstance().configExists("moduleconfigs", "gridsession")) {
			// Read session
			try {
				logger.info("Loading last session...");
				JsonObject session = ConfigProviderService.getInstance().loadConfig("moduleconfigs", "gridsession");
				lastSessionRefreshToken = session.get("refreshToken").getAsString();
				lastUsername = session.get("lastUsername").getAsString();
			} catch (IOException e) {
				logger.error("Failed to load previous Grid session from configuration!", e);
				lastSessionRefreshToken = null;
				lastUsername = null;
			}
		}

		// Attach event handlers
		EventBus.getInstance().addAllEventsFromReceiver(new ConnectionEventHandlers());

		// Contact grid server
		gridStartup(config, lastSessionRefreshToken, lastUsername);
	}

	@EventListener
	public void accountAuthenticated(AccountAuthenticatedEvent event) {
		if (loginSystemMessage != null) {
			// Send message
			PlayerMessenger messenger = WsMessageService.getInstance().getMessengerFor(event.getAccount());
			WsGenericMessage msg = new WsGenericMessage();
			msg.rawObject.typeID = 3;
			msg.rawObject.messageContentMembers = loginSystemMessage;
			msg.rawObject.messageContentNonMembers = msg.rawObject.messageContentMembers;
			try {
				messenger.sendSessionMessage(msg);
			} catch (IOException e) {
			}
		}
	}

	public static void sendGridErrorMessageToPlayers(String message) {
		// Send to all players
		loginSystemMessage = message;
		try {
			for (AccountObject account : AccountManager.getInstance().getOnlinePlayers()) {
				if (account.isOnline()) {
					// Send message
					PlayerMessenger messenger = WsMessageService.getInstance().getMessengerFor(account);
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
		} catch (IllegalArgumentException e) {
		}
	}

	private void gridStartup(JsonObject config, String lastSessionRefreshToken, String lastUsername) {
		boolean graphical = !GraphicsEnvironment.isHeadless();
		try {
			// Authenticate
			GridClient.authenticateGame(GRID_API_VERSION, GRID_SOFTWARE_ID);
			logger.info("Successfully authenticated the game!");
			authenticateGameSuccess(false, lastSessionRefreshToken, lastUsername);
		} catch (IOException e) {
			// Show message
			String message = "";
			boolean canRetry = false;
			if (e.getMessage() != null) {
				switch (e.getMessage()) {

				case "API version mismatch while authenticating the game": {
					// API mismatch
					message = "Server is running a different version of the Grid protocol, please check for updates.";
					logger.error("Failed to authenticate with the Grid servers!");
					logger.error("Reason: server is running a different version of the Grid protocol");
					break;
				}

				case "Software ID mismatch while authenticating the game": {
					// Software mismatch
					message = "Server is not configured for use with Project Edge clients.";
					logger.error("Failed to authenticate with the Grid servers!");
					logger.error("Reason: server is not configured for use with Project Edge clients");
					break;
				}

				default: {
					// Try to connect to the server
					try {
						// Build URL
						String url = PhoenixEnvironment.defaultAPIServer;
						if (!url.endsWith("/"))
							url += "/";
						url += "testconnection";

						// Contact server
						HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
						int code = conn.getResponseCode();
						if (code != 200 && code != 404)
							throw new IOException(); // Down
					} catch (IOException e2) {
						// Servers are not responding
						canRetry = true;
						message = "Could not connect to the grid API servers, please verify your internet connection.";
						logger.error(
								"Could not connect to the grid API servers, please verify your internet connection");
						break;
					}

					// Generic error
					String stackTrace = "";
					Throwable t = e;
					while (t != null) {
						for (StackTraceElement ele : t.getStackTrace())
							stackTrace += "\n     At: " + ele;
						t = t.getCause();
						if (t != null)
							stackTrace += "\nCaused by: " + t;
					}
					message = "An error occurred while contacting the grid API servers\n\nError details: "
							+ e.toString() + stackTrace;
					logger.error("Failed to authenticate with the Grid servers!");
					logger.error("Reason: an error occurred while contacting the grid API servers", e);
					break;
				}

				}
			}
			if (!graphical) {
				if (canRetry) {
					logger.warn("Edge will launch in offline mode until a connection is re-established,");
//					logger.warn("note that players will NOT be able to use Grid saves until Edge connects to the Grid servers again!");
					logger.warn("note that progress will NOT sync to the Grid servers until Edge connects again!");
					sendGridErrorMessageToPlayers(
							"A connection to the Multiplayer Grid could not be made, non-local multiplayer features are presently unavailable.\n\nEdge will attempt to connect in the background until a connection is made.");
				} else {
					logger.warn("Due to this error, Edge will launch in offline mode.");
//					logger.warn("Please note that players will NOT be able to use Grid saves until the issue is resolved!");
					logger.warn(
							"Please note that player progress will NOT sync to the Grid until the issue is resolved!");
					sendGridErrorMessageToPlayers(
							"A connection to the Multiplayer Grid could not be made, non-local multiplayer features are presently unavailable.");
				}
				logger.warn("Waiting 15 seconds before continuing...");
				try {
					Thread.sleep(15000);
				} catch (InterruptedException e2) {
				}
			} else {
				// Create message
				String msg = "Failed to start the grid client.\n";
				msg += "\n";
				msg += "\n";
				msg += "Error message:\n";
				msg += message;
				msg += "\n";
				msg += "\n";
				if (canRetry) {
					msg += "\nDo you want to try connecting again?";
					msg += "\n";
					msg += "\nIf you select no, Edge will launch in offline mode until a connection is re-established,";
//					msg += "\nnote that you will NOT be able to use Grid saves until Edge connects to the Grid servers again!";
					msg += "\nnote that your progress will NOT sync to the Grid servers until Edge connects again!";
					msg += "\n";
					msg += "\nEdge will try to reconnect in the background until a connection is made.";
				} else {
					msg += "\nDue to this error, Edge will launch in offline mode if you proceed.";
//					msg += "\nPlease note that you will NOT be able to use Grid saves until the issue is resolved!";
					msg += "\nPlease note that your progress will NOT sync to the Grid until the issue is resolved!";
				}

				// Show
				if (canRetry) {
					int res = JOptionPane.showConfirmDialog(null, msg, "Connection failure",
							JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
					if (res == JOptionPane.YES_OPTION) {
						gridStartup(config, lastSessionRefreshToken, lastUsername);
						return;
					} else if (res != JOptionPane.NO_OPTION) {
						logger.info("Exiting server!");
						System.exit(0);
					}
					logger.info("Proceeding in offline mode... Automatic retry enabled...");
					sendGridErrorMessageToPlayers(
							"A connection to the Multiplayer Grid could not be made, non-local multiplayer features are presently unavailable.\n\nEdge will attempt to connect in the background until a connection is made.");
				} else {
					int res = JOptionPane.showConfirmDialog(null, msg, "Connection failure",
							JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
					if (res != JOptionPane.OK_OPTION) {
						logger.info("Exiting server!");
						System.exit(0);
					}
					logger.info("Proceeding in offline mode...");
					sendGridErrorMessageToPlayers(
							"A connection to the Multiplayer Grid could not be made, non-local multiplayer features are presently unavailable.");
				}
			}

			// Start retry
			if (canRetry) {
				AsyncTaskManager.runAsync(() -> {
					while (true) {
						// Authenticate
						try {
							GridClient.authenticateGame(GRID_API_VERSION, GRID_SOFTWARE_ID);
							logger.info("Successfully authenticated the game!");
							authenticateGameSuccess(true, lastSessionRefreshToken, lastUsername);
							break;
						} catch (IOException e2) {
						}
						try {
							Thread.sleep(10000);
						} catch (InterruptedException e1) {
							break;
						}
					}
				});
			} else
				return;
		}
	}

	@EventListener
	public void phoenixTokenInvalidated(PhoenixGameInvalidatedEvent event) {
		try {
			// Authenticate
			GridClient.authenticateGame(GRID_API_VERSION, GRID_SOFTWARE_ID);
			logger.info("Successfully re-authenticated the game.");

			// Read session
			String lastSessionRefreshToken = null;
			String lastUsername = null;
			if (ConfigProviderService.getInstance().configExists("moduleconfigs", "gridsession")) {
				// Read session
				try {
					logger.info("Loading last session...");
					JsonObject session = ConfigProviderService.getInstance().loadConfig("moduleconfigs", "gridsession");
					lastSessionRefreshToken = session.get("refreshToken").getAsString();
					lastUsername = session.get("lastUsername").getAsString();
				} catch (IOException e) {
					logger.error("Failed to load previous Grid session from configuration!", e);
					lastSessionRefreshToken = null;
					lastUsername = null;
				}
			}

			// Log in
			authenticateGameSuccess(true, lastSessionRefreshToken, lastUsername);
		} catch (IOException e) {
			// Retry
			AsyncTaskManager.runAsync(() -> {
				while (true) {
					// Authenticate
					try {
						GridClient.authenticateGame(GRID_API_VERSION, GRID_SOFTWARE_ID);
						break;
					} catch (IOException e2) {
					}
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e1) {
						break;
					}
				}
			});
		}
	}

	@EventListener
	public void phoenixTokenRefreshFailure(SessionRefreshFailureEvent event) {
		// Token invalidated

		// Verify session lock
		if (GridClient.sessionLockStatus() == SessionLockStatus.LOCK_LOST) {
			// Someone else logged in
			logger.warn("Phoenix session has been invalidated! The account was logged into from another location!");

			// Make sure the clients get informed
			sendGridErrorMessageToPlayers("The Grid account used by this server was logged into from another location!"
					+ " The Multiplayer Grid will cease to function for this session, please restart the game to resume non-local multiplayer features."
					+ "\n\nThis message will only be showed once in your session.");
			return;
		}

		// Refresh
		logger.warn("Phoenix session has been invalidated! Attempting re-authentication...");
		if (PhoenixEnvironment.defaultLoginToken != null) {
			// Load refresh token
			String lastSessionRefreshToken = null;
			String lastUsername = null;
			if (ConfigProviderService.getInstance().configExists("moduleconfigs", "gridsession")) {
				// Read session
				try {
					logger.info("Loading last session...");
					JsonObject session = ConfigProviderService.getInstance().loadConfig("moduleconfigs", "gridsession");
					lastSessionRefreshToken = session.get("refreshToken").getAsString();
					lastUsername = session.get("lastUsername").getAsString();
				} catch (IOException e) {
					logger.error("Failed to load previous Grid session from configuration!", e);
					lastSessionRefreshToken = null;
					lastUsername = null;
				}
			}

			// Log in
			authenticateGameSuccess(true, lastSessionRefreshToken, lastUsername);
		} else {
			// Wait
			AsyncTaskManager.runAsync(() -> {
				while (true) {
					if (PhoenixEnvironment.defaultLoginToken != null) {
						// Load refresh token
						String lastSessionRefreshToken = null;
						String lastUsername = null;
						if (ConfigProviderService.getInstance().configExists("moduleconfigs", "gridsession")) {
							// Read session
							try {
								logger.info("Loading last session...");
								JsonObject session = ConfigProviderService.getInstance().loadConfig("moduleconfigs",
										"gridsession");
								lastSessionRefreshToken = session.get("refreshToken").getAsString();
								lastUsername = session.get("lastUsername").getAsString();
							} catch (IOException e) {
								logger.error("Failed to load previous Grid session from configuration!", e);
								lastSessionRefreshToken = null;
								lastUsername = null;
							}
						}

						// Log in
						authenticateGameSuccess(true, lastSessionRefreshToken, lastUsername);
						break;
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				}
			});
		}
	}

	private void authenticateGameSuccess(boolean wasOffline, String lastSessionRefreshToken, String lastUsername) {
		// Prepare login manager
		LoginManager manager = new LoginManager();

		// Log in
		if (lastSessionRefreshToken != null) {
			logger.info("Logging into the Grid with refresh token...");
			JsonObject req = new JsonObject();
			req.addProperty("mode", "refreshtoken");
			req.addProperty("token", lastSessionRefreshToken);
			manager.login(req, t -> {
				// Error
				logger.error("Login failure! Error message: " + t.getErrorMessage());

				// Failed
				GridClientAuthenticationFailureEvent ev = new GridClientAuthenticationFailureEvent(manager, t);
				EventBus.getInstance().dispatchEvent(ev);

				// Check
				if (t.getError().equals("invalid_token")) {
					// Session invalid
					authenticateGameSuccess(wasOffline, null, null);
					return;
				}

				// Show message
				if (!wasOffline && !GraphicsEnvironment.isHeadless()) {
					String msg = "Failed to log into the grid servers!\n";
					msg += "\n";
					msg += "Error details: " + t.getErrorMessage() + "\n";
					msg += "\n";
					msg += "Edge will proceed in offline mode.\n";
//					msg += "Please note that you will NOT be able to use Grid saves.";
					msg += "Please note that your progress will NOT sync to the Grid servers.";
					int res = JOptionPane.showConfirmDialog(null, msg, "Login failure", JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.WARNING_MESSAGE);
					if (res != JOptionPane.OK_OPTION) {
						logger.info("Exiting server!");
						System.exit(0);
					}
				}
				sendGridErrorMessageToPlayers(
						"A connection to the Multiplayer Grid could not be made, non-local multiplayer features are presently unavailable.");
			}, t -> {
				// Deferred
				GridClientAuthenticationDeferredEvent ev = new GridClientAuthenticationDeferredEvent(manager, t);
				EventBus.getInstance().dispatchEvent(ev);
				if (ev.isHandled())
					return;

				// Error
				logger.error("Login failure! Login was deferred and not handled!");
				if (!wasOffline && !GraphicsEnvironment.isHeadless()) {
					String msg = "Failed to log into the grid servers!\n";
					msg += "\n";
					msg += "Error details: login was deferred but not handled by any module.\n";
					msg += "Data request key: " + t.getDataRequestKey() + "\n";
					msg += "\n";
					msg += "Edge will proceed in offline mode.\n";
//					msg += "Please note that you will NOT be able to use Grid saves.";
					msg += "Please note that your progress will NOT sync to the Grid servers.";
					int res = JOptionPane.showConfirmDialog(null, msg, "Login failure", JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.WARNING_MESSAGE);
					if (res != JOptionPane.OK_OPTION) {
						logger.info("Exiting server!");
						System.exit(0);
					}
				}
				sendGridErrorMessageToPlayers(
						"A connection to the Multiplayer Grid could not be made, non-local multiplayer features are presently unavailable.");
			}, t -> {
				// Login success
				logger.info("Login success! Logged in as " + t.getDisplayName() + "!");

				// Get refresh token
				String refreshToken = t.getRawResponse().get("refreshToken").getAsString();
				t.getRawResponse().remove("refreshToken");

				// Dispatch event
				EventBus.getInstance().dispatchEvent(new GridClientAuthenticationSuccessEvent(manager, t.getAccountID(),
						t.getDisplayName(), t.getRawResponse()));

				// Call success
				loginSuccess(t, refreshToken);
			});
			return;
		}

		// Check
		if (!wasOffline) {
			// Show login
			if (!GraphicsEnvironment.isHeadless()) {
				// Graphical login
				LoginWindow window = new LoginWindow();
				LoginResult result = window.show(lastUsername, (username, pass) -> {
					LoginResult r = new LoginResult();
					r.success = false;
					r.errorMessage = "An unknown error occurred";

					// Log in
					logger.info("Logging into the Grid with username and password...");
					JsonObject req = new JsonObject();
					req.addProperty("mode", "usernamepassword");
					req.addProperty("username", username);
					req.addProperty("password", pass);
					manager.login(req, t -> {
						// Error
						logger.error("Login failure! Error message: " + t.getErrorMessage());

						// Failed
						GridClientAuthenticationFailureEvent ev = new GridClientAuthenticationFailureEvent(manager, t);
						EventBus.getInstance().dispatchEvent(ev);

						// Show message
						if (!wasOffline && !GraphicsEnvironment.isHeadless()) {
							String msg = "Failed to log into the grid servers!\n";
							msg += "\n";
							msg += "Error details: " + t.getErrorMessage();
							r.errorMessage = msg;
							r.success = false;
						}
					}, t -> {
						// Deferred
						GridClientAuthenticationDeferredEvent ev = new GridClientAuthenticationDeferredEvent(manager,
								t);
						EventBus.getInstance().dispatchEvent(ev);
						if (ev.isHandled())
							return;

						// Error
						logger.error("Login failure! Login was deferred and not handled!");
						if (!wasOffline && !GraphicsEnvironment.isHeadless()) {
							String msg = "Failed to log into the grid servers!\n";
							msg += "\n";
							msg += "Error details: login was deferred but not handled by any module.\n";
							msg += "Data request key: " + t.getDataRequestKey();
							r.errorMessage = msg;
							r.success = false;
						}
					}, t -> {
						// Login success
						logger.info("Login success! Logged in as " + t.getDisplayName() + "!");

						// Get refresh token
						String refreshToken = t.getRawResponse().get("refreshToken").getAsString();
						t.getRawResponse().remove("refreshToken");
						r.refreshToken = refreshToken;
						r.session = t;
						r.success = true;
						r.errorMessage = null;
					});
					return r;
				}, (username, password) -> {
					// Register
					RegistrationResult r = new RegistrationResult();
					r.success = false;
					r.errorMessage = "An unknown error occurred";

					// Create request
					logger.info("Attempting to register account...");
					JsonObject req = new JsonObject();
					req.addProperty("username", username);
					req.addProperty("password", password);
					req.addProperty("isUnderageUser", false);
					try {
						// Contact API
						JsonObject resp = GridClient.sendGridApiRequest("/grid/accounts/registeraccount",
								PhoenixEnvironment.defaultLoginToken, req, true);
						if (resp.has("error")) {
							// Handle error
							switch (resp.get("error").getAsString()) {

							// Invalid username
							case "invalid_username": {
								logger.error("Username is invalid, please try a different one.");
								r.errorMessage = "Username is invalid, please try a different one.";
								break;
							}

							// Invalid username
							case "inappropriate_username": {
								logger.error(
										"Username is may not be appropriate for all users, please try a different one.");
								r.errorMessage = "Username is may not be appropriate for all users, please try a different one.";
								break;
							}

							// Invalid password
							case "invalid_password": {
								logger.error(
										"Password is invalid, please make sure it is at least 6 characters in length.");
								r.errorMessage = "Password is invalid, please make sure it is at least 6 characters in length.";
								break;
							}

							// Username in use
							case "username_in_use": {
								logger.error("Username is already taken, please try a different one.");
								r.errorMessage = "Username is already taken, please try a different one.";
								break;
							}

							// Default
							default:
								throw new IOException("Server returned error: " + resp.get("error").getAsString());

							}
						} else {
							// Success
							r.success = true;
							r.refreshToken = resp.get("refreshToken").getAsString();
							r.username = resp.get("accountUsername").getAsString();
							logger.info("Account created successfully!");
						}
					} catch (IOException e) {
						String stackTrace = "";
						Throwable t = e;
						while (t != null) {
							for (StackTraceElement ele : t.getStackTrace())
								stackTrace += "\n     At: " + ele;
							t = t.getCause();
							if (t != null)
								stackTrace += "\nCaused by: " + t;
						}
						logger.error("An error occurred while contacting the Grid API! Registration cancelled!", e);
						r.errorMessage = "An error occurred while contacting the Grid API!\n\nError details: " + e
								+ stackTrace;
					}

					// Return
					return r;
				}, (refreshToken, username) -> {
					// Refresh
					LoginResult r = new LoginResult();
					r.success = false;
					r.errorMessage = "An unknown error occurred";

					// Log in
					logger.info("Logging into the Grid with refresh token...");
					JsonObject req = new JsonObject();
					req.addProperty("mode", "refreshtoken");
					req.addProperty("token", refreshToken);
					manager.login(req, t -> {
						// Error
						logger.error("Login failure! Error message: " + t.getErrorMessage());

						// Failed
						GridClientAuthenticationFailureEvent ev = new GridClientAuthenticationFailureEvent(manager, t);
						EventBus.getInstance().dispatchEvent(ev);

						// Show message
						if (!wasOffline && !GraphicsEnvironment.isHeadless()) {
							String msg = "Failed to log into the grid servers!\n";
							msg += "\n";
							msg += "Error details: " + t.getErrorMessage();
							r.errorMessage = msg;
							r.success = false;
						}
					}, t -> {
						// Deferred
						GridClientAuthenticationDeferredEvent ev = new GridClientAuthenticationDeferredEvent(manager,
								t);
						EventBus.getInstance().dispatchEvent(ev);
						if (ev.isHandled())
							return;

						// Error
						logger.error("Login failure! Login was deferred and not handled!");
						if (!wasOffline && !GraphicsEnvironment.isHeadless()) {
							String msg = "Failed to log into the grid servers!\n";
							msg += "\n";
							msg += "Error details: login was deferred but not handled by any module.\n";
							msg += "Data request key: " + t.getDataRequestKey();
							r.errorMessage = msg;
							r.success = false;
						}
					}, t -> {
						// Login success
						logger.info("Login success! Logged in as " + t.getDisplayName() + "!");

						// Get refresh token
						String rfS = t.getRawResponse().get("refreshToken").getAsString();
						t.getRawResponse().remove("refreshToken");
						r.refreshToken = rfS;
						r.session = t;
						r.success = true;
						r.errorMessage = null;
					});
					return r;
				});
				if (result != null && result.success) {
					// Dispatch event
					EventBus.getInstance().dispatchEvent(
							new GridClientAuthenticationSuccessEvent(manager, result.session.getAccountID(),
									result.session.getDisplayName(), result.session.getRawResponse()));

					// Call success
					loginSuccess(result.session, result.refreshToken);
				} else {
					sendGridErrorMessageToPlayers(
							"A connection to the Multiplayer Grid could not be made, non-local multiplayer features are presently unavailable.");
				}
			} else {
				// Console login
				logger.info("Please log into your grid account...");
				while (true) {
					System.out.print("Do you wish to log into an existing account? [Y/n/c] ");

					// Read
					String opt = sc.nextLine();
					if (!opt.equalsIgnoreCase("Y") && !opt.equalsIgnoreCase("N") && !opt.equalsIgnoreCase("C")) {
						System.err.println("Please select either Y (Yes), N (No) or C (Cancel).");
						continue;
					}

					// Check cancel
					if (opt.equalsIgnoreCase("C")) {
						logger.info("Login cancelled.");
						sendGridErrorMessageToPlayers(
								"A connection to the Multiplayer Grid could not be made, non-local multiplayer features are presently unavailable.");
						break;
					}

					// Check
					if (opt.equalsIgnoreCase("Y")) {
						logger.info("Welcome to the Grid account login interface!");
						logger.info("Please enter your account login details below...");

						// Existing
						System.out.print("Account username: ");
						String username = sc.nextLine();
						if (username.isEmpty())
							break;
						System.out.print("Account password: ");
						var con = System.console();
						char[] passwd;
						if (con == null) {
							passwd = sc.nextLine().toCharArray();
						} else
							passwd = con.readPassword();

						// Log in
						logger.info("Logging into the Grid with username and password...");
						JsonObject req = new JsonObject();
						req.addProperty("mode", "usernamepassword");
						req.addProperty("username", username);
						req.addProperty("password", new String(passwd));
						manager.login(req, t -> {
							// Error
							logger.error("Login failure! Error message: " + t.getErrorMessage());

							// Failed
							GridClientAuthenticationFailureEvent ev = new GridClientAuthenticationFailureEvent(manager,
									t);
							EventBus.getInstance().dispatchEvent(ev);

							// Show message
							if (!wasOffline && !GraphicsEnvironment.isHeadless()) {
								String msg = "Failed to log into the grid servers!\n";
								msg += "\n";
								msg += "Error details: " + t.getErrorMessage() + "\n";
								msg += "\n";
								msg += "Edge will proceed in offline mode.\n";
//								msg += "Please note that you will NOT be able to use Grid saves.";
								msg += "Please note that your progress will NOT sync to the Grid servers.";
								int res = JOptionPane.showConfirmDialog(null, msg, "Login failure",
										JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
								if (res != JOptionPane.OK_OPTION) {
									logger.info("Exiting server!");
									System.exit(0);
								}
							}
							sendGridErrorMessageToPlayers(
									"A connection to the Multiplayer Grid could not be made, non-local multiplayer features are presently unavailable.");
						}, t -> {
							// Deferred
							GridClientAuthenticationDeferredEvent ev = new GridClientAuthenticationDeferredEvent(
									manager, t);
							EventBus.getInstance().dispatchEvent(ev);
							if (ev.isHandled())
								return;

							// Error
							logger.error("Login failure! Login was deferred and not handled!");
							if (!wasOffline && !GraphicsEnvironment.isHeadless()) {
								String msg = "Failed to log into the grid servers!\n";
								msg += "\n";
								msg += "Error details: login was deferred but not handled by any module.\n";
								msg += "Data request key: " + t.getDataRequestKey() + "\n";
								msg += "\n";
								msg += "Edge will proceed in offline mode.\n";
//								msg += "Please note that you will NOT be able to use Grid saves.";
								msg += "Please note that your progress will NOT sync to the Grid servers.";
								int res = JOptionPane.showConfirmDialog(null, msg, "Login failure",
										JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
								if (res != JOptionPane.OK_OPTION) {
									logger.info("Exiting server!");
									System.exit(0);
								}
							}
							sendGridErrorMessageToPlayers(
									"A connection to the Multiplayer Grid could not be made, non-local multiplayer features are presently unavailable.");
						}, t -> {
							// Login success
							logger.info("Login success! Logged in as " + t.getDisplayName() + "!");

							// Get refresh token
							String refreshToken = t.getRawResponse().get("refreshToken").getAsString();
							t.getRawResponse().remove("refreshToken");

							// Dispatch event
							EventBus.getInstance().dispatchEvent(new GridClientAuthenticationSuccessEvent(manager,
									t.getAccountID(), t.getDisplayName(), t.getRawResponse()));

							// Call success
							loginSuccess(t, refreshToken);
						});
						return;
					} else {
						// New account
						logger.info("Welcome to the Grid account registration interface!");
						logger.info("Please create your account login details below...");
						boolean created = false;
						while (!created) {
							System.out.print("Create a account username (leave empty to cancel): ");
							String username = sc.nextLine();
							if (username.isEmpty())
								break;
							System.out.print("Create a account password: ");
							var con = System.console();
							char[] passwd;
							if (con == null) {
								passwd = sc.nextLine().toCharArray();
							} else
								passwd = con.readPassword();
							System.out.print("Repeat account password: ");
							char[] passwd2;
							if (con == null) {
								passwd2 = sc.nextLine().toCharArray();
							} else
								passwd2 = con.readPassword();
							if (passwd.length != passwd2.length) {
								System.err.println("Passwords do not match!");
								continue;
							}
							for (int i = 0; i < passwd.length; i++) {
								if (passwd[i] != passwd2[i]) {
									System.err.println("Passwords do not match!");
									continue;
								}
							}

							// Create request
							logger.info("Attempting to register account...");
							JsonObject req = new JsonObject();
							req.addProperty("username", username);
							req.addProperty("password", new String(passwd));
							req.addProperty("isUnderageUser", false);
							try {
								// Contact API
								JsonObject resp = GridClient.sendGridApiRequest("/grid/accounts/registeraccount",
										PhoenixEnvironment.defaultLoginToken, req, true);
								if (resp.has("error")) {
									// Handle error
									switch (resp.get("error").getAsString()) {

									// Invalid username
									case "invalid_username": {
										logger.error("Username is invalid, please try a different one.");
										break;
									}

									// Invalid username
									case "inappropriate_username": {
										logger.error(
												"Username is may not be appropriate for all users, please try a different one.");
										break;
									}

									// Invalid password
									case "invalid_password": {
										logger.error(
												"Password is invalid, please make sure it is at least 6 characters in length.");
										break;
									}

									// Username in use
									case "username_in_use": {
										logger.error("Username is already taken, please try a different one.");
										break;
									}

									// Default
									default:
										throw new IOException(
												"Server returned error: " + resp.get("error").getAsString());

									}
								} else {
									// Success
									created = true;
									logger.info("Account created! Logging in via refresh token...");
									authenticateGameSuccess(wasOffline, resp.get("refreshToken").getAsString(),
											resp.get("accountUsername").getAsString());
									return;
								}
							} catch (IOException e) {
								logger.error("An error occurred while contacting the Grid API! Login cancelled!", e);
								return;
							}
						}
					}
				}
			}
		} else {
			// Error
			logger.error(
					"Unable to log back into the Grid! There is no valid refresh token and the system is past initialization, unable to prompt for input!");
		}
	}

	private void loginSuccess(PhoenixSession session, String refreshToken) {
		// Save session
		JsonObject obj = new JsonObject();
		obj.addProperty("refreshToken", refreshToken);
		obj.addProperty("lastUsername", session.getDisplayName());
		try {
			ConfigProviderService.getInstance().saveConfig("moduleconfigs", "gridsession", obj);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// Start grid client
		logger.info("Initializing Grid client...");
		logger.info("Finding best server...");
		ServerInstance server = GridClient.findBestServer();
		if (server == null) {
			// Failed
			logger.error("Could not find any Phoenix servers! Automatic retry started!");
			if (loginSystemMessage == null)
				sendGridErrorMessageToPlayers(
						"A connection to the Multiplayer Grid could not be made, non-local multiplayer features are presently unavailable.\n\nEdge will attempt to connect in the background until a connection is made.");
			AsyncTaskManager.runAsync(() -> {
				while (true) {
					ServerInstance srv = GridClient.findBestServer();
					if (srv != null) {
						startClient(srv, session);
						break;
					}
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						break;
					}
				}
			});
		} else
			startClient(server, session);
	}

	private void startClient(ServerInstance server, PhoenixSession session) {
		// Found server
		try {
			GridClient.initGridPhoenixClient(server, session.getManager());
		} catch (IOException e) {
			AsyncTaskManager.runAsync(() -> {
				while (true) {
					try {
						ServerInstance srv = GridClient.findBestServer();
						if (srv != null)
							GridClient.initGridPhoenixClient(srv, session.getManager());
						else if (loginSystemMessage == null)
							sendGridErrorMessageToPlayers(
									"A connection to the Multiplayer Grid could not be made, non-local multiplayer features are presently unavailable.\n\nEdge will attempt to connect in the background until a connection is made.");
						break;
					} catch (IOException e2) {
					}
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e2) {
						break;
					}
				}
			});
		}
	}

}
