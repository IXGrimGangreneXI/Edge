package org.asf.edge.modules.gridapi.commands.defaultcommands;

import java.util.Date;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.modules.gridapi.commands.CommandContext;
import org.asf.edge.modules.gridapi.commands.IGridServerCommand;
import org.asf.edge.modules.gridapi.commands.TaskBasedCommand;
import org.asf.edge.modules.gridapi.utils.IdentityUtils;

import com.google.gson.JsonPrimitive;

public class AccountCommands extends TaskBasedCommand {

	@Override
	public String id() {
		return "accounts";
	}

	@Override
	public String description(CommandContext ctx) {
		return "Account configuration commands";
	}

	@Override
	public IGridServerCommand[] tasks() {
		return new IGridServerCommand[] {

				// Account register
				new IGridServerCommand() {

					@Override
					public String id() {
						return "register";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "\"<email>\" \"<username>\" \"<password>\" [<true/false (isUnderageUser)>] [<true/false (subscribeEmailNotifications)>]";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Registers accounts";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) throws Exception {
						// Check
						if (args.length < 1) {
							outputWriteLineCallback.accept("Missing argument: email");
							return null;
						} else if (args.length < 2) {
							outputWriteLineCallback.accept("Missing argument: username");
							return null;
						} else if (args.length < 3) {
							outputWriteLineCallback.accept("Missing argument: password");
							return null;
						}

						// Load request into memory
						String email = args[0];
						String username = args[1];
						String password = args[2];
						boolean isUnderageUser = args.length >= 4 ? args[3].equalsIgnoreCase("true") : false;
						boolean subscribeEmail = args.length >= 5 ? args[4].equalsIgnoreCase("true") : false;

						// Find account manager
						AccountManager manager = AccountManager.getInstance();

						// Check username validity
						if (!manager.isValidUsername(username)) {
							// Invalid name
							outputWriteLineCallback.accept("Error: invalid username");
							return null;
						}

						// Check filters
						if (TextFilterService.getInstance().isFiltered(username, true)) {
							// Invalid name
							outputWriteLineCallback.accept("Error: invalid username");
							return null;
						}

						// Check password validity
						if (!manager.isValidPassword(password)) {
							// Invalid password
							outputWriteLineCallback
									.accept("Error: invalid password, must be at least 6 characters long");
							return null;
						}

						// Verify email
						if (!email.toLowerCase().matches(
								"(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")) {
							// Invalid name
							outputWriteLineCallback.accept("Error: invalid email address");
							return null;
						}

						// Check if email is taken
						if (manager.getAccountIDByEmail(email) != null) {
							// Error
							outputWriteLineCallback.accept("Error: email address already taken");
							return null;
						}

						// Check if name is taken
						if (manager.isUsernameTaken(username)) {
							// Error
							outputWriteLineCallback.accept("Error: username already taken");
							return null;
						}

						// Check filters
						if (TextFilterService.getInstance().isFiltered(username, true)) {
							// Error
							outputWriteLineCallback.accept("Error: username may not be used");
							return null;
						}

						// Create account
						AccountObject acc = manager.registerAccount(username, email, password.toCharArray());
						if (acc == null) {
							// Error
							outputWriteLineCallback.accept("Error: a server error occurred");
							return null;
						}

						// Set data
						AccountDataContainer cont = acc.getAccountData().getChildContainer("accountdata");
						cont.setEntry("sendupdates", new JsonPrimitive(subscribeEmail));
						cont.setEntry("isunderage", new JsonPrimitive(isUnderageUser));
						cont.setEntry("last_update", new JsonPrimitive(System.currentTimeMillis()));
						cont.setEntry("significantFieldRandom", new JsonPrimitive(IdentityUtils.rnd.nextInt()));
						cont.setEntry("significantFieldNumber", new JsonPrimitive(System.currentTimeMillis()));
						if (isUnderageUser) {
							acc.setChatEnabled(false);
							acc.setStrictChatFilterEnabled(true);
						}

						// Update login time
						acc.updateLastLoginTime();

						// Return
						return "Account created successfully.";
					}

				},

				// Account show
				new IGridServerCommand() {

					@Override
					public String id() {
						return "show";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "\"<username>\"";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Shows accounts";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) throws Exception {
						// Check arguments
						if (args.length < 1) {
							outputWriteLineCallback.accept("Missing argument: username");
							return null;
						}

						// Load request into memory
						String username = args[0];

						// Find account manager
						AccountManager manager = AccountManager.getInstance();

						// Check
						if (!manager.isUsernameTaken(username)) {
							// Invalid name
							outputWriteLineCallback.accept("Error: account not found");
							return null;
						}

						// Retrieve account
						String id = manager.getAccountID(username);
						if (id == null) {
							// Invalid name
							outputWriteLineCallback.accept("Error: account not found");
							return null;
						}

						// Load object
						AccountObject account = manager.getAccount(id);

						// Show
						String message = "Account properties for: " + account.getUsername() + ":\n";
						message += "\nAccount ID: " + account.getAccountID();
						message += "\nAccount username: " + account.getUsername();
						message += "\nAccount registration time: " + new Date(account.getRegistrationTimestamp());
						message += "\nAccount last login time: " + (account.getLastLoginTime() == -1 ? "never logged in"
								: new Date(account.getLastLoginTime()));
						message += "\n";
						message += "\nMMO enabled: " + account.isMultiplayerEnabled();
						message += "\nChat enabled: " + account.isChatEnabled();
						message += "\nStrict chat filter enabled: " + account.isStrictChatFilterEnabled();

						// Find saves
						message += "\n\n";
						for (String save : account.getSaveIDs()) {
							AccountSaveContainer sv = account.getSave(save);
							message += "Save details for " + sv.getUsername() + ":";
							message += "\nSave ID: " + sv.getSaveID();
							message += "\nSave username: " + sv.getUsername();
							message += "\nSave creation time: " + new Date(sv.getCreationTime());
							message += "\n\n";
						}

						message += "For identity properties of this account, use `identities show "
								+ account.getAccountID() + "`";
						return message;
					}

				},

				// Account list
				new IGridServerCommand() {

					@Override
					public String id() {
						return "list";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return null;
					}

					@Override
					public String description(CommandContext ctx) {
						return "Lists all accounts";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) throws Exception {
						StringBuilder message = new StringBuilder("List of all accounts:");
						AccountManager.getInstance().runForAllAccounts(t -> {
							message.append("\n - " + t.getUsername() + " (" + t.getAccountID() + ")");
							return true;
						});
						return message.toString();
					}

				},

				// Account delete
				new IGridServerCommand() {

					@Override
					public String id() {
						return "delete";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "\"<username>\"";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Deletes accounts";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) throws Exception {
						// Check arguments
						if (args.length < 1) {
							outputWriteLineCallback.accept("Missing argument: username");
							return null;
						}

						// Load request into memory
						String username = args[0];

						// Find account manager
						AccountManager manager = AccountManager.getInstance();

						// Check
						if (!manager.isUsernameTaken(username)) {
							// Invalid name
							outputWriteLineCallback.accept("Error: account not found");
							return null;
						}

						// Retrieve account
						String id = manager.getAccountID(username);
						if (id == null) {
							// Invalid name
							outputWriteLineCallback.accept("Error: account not found");
							return null;
						}

						// Delete
						manager.getAccount(id).deleteAccount();
						return "Account deleted successfully";
					}

				},

				// Account updateusername
				new IGridServerCommand() {

					@Override
					public String id() {
						return "updateusername";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "\"<username>\" \"<new-username>\"";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Updates account usernames";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) throws Exception {
						// Check arguments
						if (args.length < 1) {
							outputWriteLineCallback.accept("Missing argument: username");
							return null;
						} else if (args.length < 2) {
							outputWriteLineCallback.accept("Missing argument: new-username");
							return null;
						}

						// Load request into memory
						String username = args[0];
						String newUsername = args[1];

						// Find account manager
						AccountManager manager = AccountManager.getInstance();

						// Check
						if (!manager.isUsernameTaken(username)) {
							// Invalid name
							outputWriteLineCallback.accept("Error: account not found");
							return null;
						}

						// Retrieve account
						String id = manager.getAccountID(username);
						if (id == null) {
							// Invalid name
							outputWriteLineCallback.accept("Error: account not found");
							return null;
						}

						// Check filters
						if (TextFilterService.getInstance().isFiltered(newUsername, true)) {
							// Error
							outputWriteLineCallback.accept("Error: username may not be used");
							return null;
						}

						// Load object
						AccountObject account = manager.getAccount(id);

						// Check if in use
						boolean inUse = false;
						if (!account.getUsername().equalsIgnoreCase(newUsername)
								&& manager.isUsernameTaken(newUsername)) {
							inUse = true;
						} else {
							// Check if in use by any saves
							AccountObject accF = account;
							if (Stream.of(account.getSaveIDs()).map(t -> accF.getSave(t)).anyMatch(t -> {
								return t.getUsername().equalsIgnoreCase(newUsername);
							})) {
								inUse = true;
							}
						}
						if (inUse) {
							// Error
							outputWriteLineCallback.accept("Error: username is already in use");
							return null;
						}

						// Update
						if (account.updateUsername(newUsername)) {
							account.getAccountData().getChildContainer("accountdata").setEntry("last_update",
									new JsonPrimitive(System.currentTimeMillis()));
							return "Account username updated successfully";
						} else
							return "Username update failed";
					}

				},

				// Account updateemail
				new IGridServerCommand() {

					@Override
					public String id() {
						return "updateemail";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "\"<username>\" \"<new-email>\"";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Updates account emails";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) throws Exception {
						// Check arguments
						if (args.length < 1) {
							outputWriteLineCallback.accept("Missing argument: username");
							return null;
						} else if (args.length < 2) {
							outputWriteLineCallback.accept("Missing argument: new-email");
							return null;
						}

						// Load request into memory
						String username = args[0];
						String newEmail = args[1];

						// Find account manager
						AccountManager manager = AccountManager.getInstance();

						// Check
						if (!manager.isUsernameTaken(username)) {
							// Invalid name
							outputWriteLineCallback.accept("Error: account not found");
							return null;
						}

						// Retrieve account
						String id = manager.getAccountID(username);
						if (id == null) {
							// Invalid name
							outputWriteLineCallback.accept("Error: account not found");
							return null;
						}

						// Load object
						AccountObject account = manager.getAccount(id);

						// Verify email
						if (!newEmail.toLowerCase().matches(
								"(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")) {
							// Error
							outputWriteLineCallback.accept("Error: invalid email address");
						}

						// Check if email is taken
						if (manager.getAccountIDByEmail(newEmail) != null) {
							// Error
							outputWriteLineCallback.accept("Error: email address already taken");
						}

						// Update
						if (account.updateEmail(newEmail)) {
							account.getAccountData().getChildContainer("accountdata").setEntry("last_update",
									new JsonPrimitive(System.currentTimeMillis()));
							return "Account email updated successfully";
						} else
							return "Email update failed";
					}

				},

				// Account updatepassword
				new IGridServerCommand() {

					@Override
					public String id() {
						return "updatepassword";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "\"<username>\" \"<new-password>\"";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Updates account passwords";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) throws Exception {
						// Check arguments
						if (args.length < 1) {
							outputWriteLineCallback.accept("Missing argument: username");
							return null;
						} else if (args.length < 2) {
							outputWriteLineCallback.accept("Missing argument: new-password");
							return null;
						}

						// Load request into memory
						String username = args[0];
						String password = args[1];

						// Find account manager
						AccountManager manager = AccountManager.getInstance();

						// Check
						if (!manager.isUsernameTaken(username)) {
							// Invalid name
							outputWriteLineCallback.accept("Error: account not found");
							return null;
						}

						// Retrieve account
						String id = manager.getAccountID(username);
						if (id == null) {
							// Invalid name
							outputWriteLineCallback.accept("Error: account not found");
							return null;
						}

						// Check password validity
						if (!manager.isValidPassword(password)) {
							// Invalid password
							outputWriteLineCallback
									.accept("Error: invalid password, must be at least 6 characters long");
							return null;
						}

						// Update
						AccountObject acc = manager.getAccount(id);
						if (acc.updatePassword(password.toCharArray())) {
							acc.getAccountData().getChildContainer("accountdata").setEntry("last_update",
									new JsonPrimitive(System.currentTimeMillis()));
							return "Account password updated successfully";
						} else
							return "Password update failed";
					}

				},

				// Account fulllogout
				new IGridServerCommand() {

					@Override
					public String id() {
						return "fulllogout";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "\"<username>\"";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Invalidates all sessions of this account";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) throws Exception {
						// Check arguments
						if (args.length < 1) {
							outputWriteLineCallback.accept("Missing argument: username");
							return null;
						}

						// Load request into memory
						String username = args[0];

						// Find account manager
						AccountManager manager = AccountManager.getInstance();

						// Check
						if (!manager.isUsernameTaken(username)) {
							// Invalid name
							outputWriteLineCallback.accept("Error: account not found");
							return null;
						}

						// Retrieve account
						String id = manager.getAccountID(username);
						if (id == null) {
							// Invalid name
							outputWriteLineCallback.accept("Error: account not found");
							return null;
						}

						// Load object
						AccountObject account = manager.getAccount(id);
						account.getAccountData().getChildContainer("accountdata").setEntry("last_update",
								new JsonPrimitive(System.currentTimeMillis()));
						return "Successfully invalidated all tokens for this account, note that Phoenix servers will not kick players as they do not keep track of session changes.";
					}

				},

				// Account setmultiplayer
				new IGridServerCommand() {

					@Override
					public String id() {
						return "setmultiplayer";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "\"<username>\" <multiplayer-enabled:true/false> <chat-enabled:true/false> <strictchat-enabled:true/false>";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Updates account settings";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) throws Exception {
						// Check arguments
						if (args.length < 1) {
							outputWriteLineCallback.accept("Missing argument: username");
							return null;
						} else if (args.length < 2) {
							outputWriteLineCallback.accept("Missing argument: multiplayer-enabled");
							return null;
						} else if (args.length < 3) {
							outputWriteLineCallback.accept("Missing argument: chat-enabled");
							return null;
						} else if (args.length < 4) {
							outputWriteLineCallback.accept("Missing argument: strictchat-enabled");
							return null;
						}

						// Load request into memory
						String username = args[0];
						boolean mmo = args[1].equalsIgnoreCase("true");
						boolean chat = args[2].equalsIgnoreCase("true");
						boolean strictChat = args[3].equalsIgnoreCase("true");

						// Find account manager
						AccountManager manager = AccountManager.getInstance();

						// Check
						if (!manager.isUsernameTaken(username)) {
							// Invalid name
							outputWriteLineCallback.accept("Error: account not found");
							return null;
						}

						// Retrieve account
						String id = manager.getAccountID(username);
						if (id == null) {
							// Invalid name
							outputWriteLineCallback.accept("Error: account not found");
							return null;
						}

						// Load object
						AccountObject account = manager.getAccount(id);
						account.setMultiplayerEnabled(mmo);
						account.setChatEnabled(chat);
						account.setStrictChatFilterEnabled(strictChat);
						return "Account settings updated successfully";
					}

				}

		};
	}

}
