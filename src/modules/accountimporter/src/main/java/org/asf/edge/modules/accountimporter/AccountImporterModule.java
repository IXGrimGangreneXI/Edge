package org.asf.edge.modules.accountimporter;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.events.accounts.AccountManagerLoadEvent;
import org.asf.edge.common.services.accounts.AccountKvDataContainer;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.common.services.accounts.AccountSaveContainer;
import org.asf.edge.common.services.accounts.impl.BasicAccountManager;
import org.asf.edge.common.services.accounts.impl.BasicAccountObject;
import org.asf.edge.common.services.accounts.impl.BasicAccountSaveContainer;
import org.asf.edge.modules.IEdgeModule;
import org.asf.nexus.common.io.DataReader;
import org.asf.nexus.events.EventListener;

import com.google.gson.JsonParser;

public class AccountImporterModule implements IEdgeModule {
	private Scanner sc = new Scanner(System.in);

	@Override
	public String moduleID() {
		return "accountimporter";
	}

	@Override
	public String version() {
		return "1.0.0.A1";
	}

	@Override
	public void init() {
	}

	@EventListener
	public void accountManagerInited(AccountManagerLoadEvent event) throws IOException {
		// Prepare
		Logger logger = LogManager.getLogger("AccountImporter");
		File importFolder = new File("accountimports");
		logger.info("Checking account imports...");
		importFolder.mkdirs();

		// Check type
		if (!(event.getAccountManager() instanceof BasicAccountManager)) {
			logger.fatal(
					"Unable to run the account importer with account managers that are not based on the BasicAccountManager type!");
			if (!GraphicsEnvironment.isHeadless()) {
				try {
					SwingUtilities.invokeAndWait(() -> {
						JOptionPane.showMessageDialog(null,
								"An error occured while running the importer\n\nUnable to run the account importer with account managers that are not based on the BasicAccountManager type!",
								"Module Error", JOptionPane.ERROR_MESSAGE);
					});
				} catch (InvocationTargetException | InterruptedException e1) {
				}
			}
			System.exit(1);
		}

		// Find accounts
		for (File f : importFolder.listFiles(t -> t.getName().endsWith(".ead") && t.isFile())) {
			logger.info("Loading account file: " + f.getName());

			// Read file
			boolean deleteFile = false;
			FileInputStream strm = new FileInputStream(f);
			try {
				try {
					// Read headers
					String accID = readString(strm);
					String username = readString(strm);

					// Open UI if possible
					if (!GraphicsEnvironment.isHeadless()) {
						ProgressWindow.WindowAppender.showWindow();
					}
					ProgressWindow.WindowAppender.setMax(0);
					ProgressWindow.WindowAppender.setValue(0);
					ProgressWindow.WindowAppender.lockLabel();
					ProgressWindow.WindowAppender.setLabel("Preparing to import '" + username + "'");

					// Read headers
					logger.info("Importing account '" + username + "' please wait...");
					logger.info("Reading headers...");
					String email = null;
					if (readBoolean(strm))
						email = readString(strm);
					readLong(strm);
					readLong(strm);
					boolean isGuest = readBoolean(strm);
					boolean multiplayerEnabled = readBoolean(strm);
					boolean chatEnabled = readBoolean(strm);
					boolean strictChatEnabled = readBoolean(strm);

					// Check account existence
					logger.info("Verifying account name...");
					if (event.getAccountManager().accountExists(accID)) {
						// Warn
						logger.warn("Account '" + username + "' already exists on the server!");
						logger.warn("Each save on this account will be wiped and reimported.");
						if (ProgressWindow.shown) {
							if (JOptionPane.showConfirmDialog(null, "Account '" + username
									+ "' already exists on the server!\nEach save on this account will be wiped and reimported.\n\nPress cancel to cancel this operation if you do not wish the data to be modified.\nCancelling will shut the server down, to prevent this operation happening on next startup,\ndelete the file '"
									+ f.getPath() + "' from the server.", "Importing", JOptionPane.WARNING_MESSAGE,
									JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
								System.exit(0);
							}
						} else {
							logger.warn("Shut down the Edge server now if you do not want this!");
							logger.warn("Waiting 30 seconds before continuing...");
							try {
								Thread.sleep(30000);
							} catch (InterruptedException e) {
							}
						}
					} else if (event.getAccountManager().isUsernameTaken(username)) {
						// Error
						logger.fatal("Unable to import account '" + username + "' (" + f.getPath()
								+ ") as an account with the same name already exists on the server.");
						ProgressWindow.WindowAppender.fatalError("Unable to import account '" + username + "' ("
								+ f.getPath() + ")\nas an account with the same name already exists on the server.");
						System.exit(1);
					}

					// Check saves
					logger.info("Verifying saves...");
					long pos = strm.getChannel().position();

					// Read ahead to the save data
					logger.info("Skipping data...");
					strm.getChannel().position(strm.getChannel().position() + 8);
					skipOverSaveData(strm);

					// Find data
					logger.info("Reading save headers...");
					HashMap<String, String> saves = new HashMap<String, String>();
					int saveCount = readInt(strm);
					for (int i = 0; i < saveCount; i++) {
						// Read headers
						String id = readString(strm);
						String name = readString(strm);
						strm.getChannel().position(strm.getChannel().position() + 8);
						saves.put(id, name);
						skipOverSaveData(strm);
					}

					// Verify
					logger.info("Verifying save names...");
					for (String id : saves.keySet()) {
						String name = saves.get(id);

						// Check ID
						logger.info("Verifying save: " + name);
						String owner = event.getAccountManager().getAccountIdBySaveUsername(name);
						if (owner != null && !owner.equals(accID)) {
							// Error
							logger.fatal("Unable to import account '" + username + "' (" + f.getPath()
									+ ") as the name of save '" + name + "' is already taken.");
							ProgressWindow.WindowAppender.fatalError("Unable to import account '" + username + "' ("
									+ f.getPath() + ")\nas the name of save '" + name + "' is already taken.");
							System.exit(1);
						} else if (owner != null) {
							// Verify save ID
							AccountObject acc = event.getAccountManager().getAccount(accID);
							for (String svID : acc.getSaveIDs()) {
								if (acc.getSave(svID).getUsername().equalsIgnoreCase(name)) {
									// Check ID
									if (!svID.equals(id)) {
										// Error
										logger.fatal("Unable to import account '" + username + "' (" + f.getPath()
												+ ") as the name of save '" + name
												+ "' is in use by another save on the account.");
										ProgressWindow.WindowAppender.fatalError("Unable to import account '" + username
												+ "' (" + f.getPath() + ")\nas the name of save '" + name
												+ "' is in use by another save on the account.");
										System.exit(1);
									}
									break;
								}
							}
						}

						// Check ID
						AccountSaveContainer save = event.getAccountManager().getSaveByID(id);
						if (save != null && !save.getAccount().getAccountID().equalsIgnoreCase(accID)) {
							// Error
							logger.fatal("Unable to import account '" + username + "' (" + f.getPath() + ") as save '"
									+ id + "' is owned by a different user.");
							ProgressWindow.WindowAppender.fatalError("Unable to import account '" + username + "' ("
									+ f.getPath() + ")\nas save '" + id + "' is owned by a different user.");
							System.exit(1);
						}
					}

					// Go back
					strm.getChannel().position(pos);

					// Prepare
					BasicAccountManager manager = (BasicAccountManager) event.getAccountManager();

					// Register account
					BasicAccountObject account = (BasicAccountObject) manager.getAccount(accID);
					if (account == null) {
						if (!isGuest) {
							ProgressWindow.WindowAppender.setLabel("Registering account...");

							// Get password
							logger.info("Requesting password creation...");
							char[] pword = null;
							if (GraphicsEnvironment.isHeadless()) {
								// Console-based
								var con = System.console();
								logger.info("Please create an account passsword for '" + username + "'");
								while (true) {
									// Request
									System.out.print("Enter new account password: ");
									char[] passwd;
									if (con == null) {
										passwd = sc.nextLine().toCharArray();
									} else
										passwd = con.readPassword();

									// Repeat
									System.out.print("Repeat account password: ");
									char[] passwd2;
									if (con == null) {
										passwd2 = sc.nextLine().toCharArray();
									} else
										passwd2 = con.readPassword();

									// Verify
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
									if (!manager.isValidPassword(new String(passwd))) {
										System.err.println(
												"Password is invalid, please make sure it is at least 6 characters long.");
										continue;
									}

									// Assign
									pword = passwd;
									break;
								}
							} else {
								// Show password creation window
								PasswordCreationWindow window = new PasswordCreationWindow();
								String res = window.show(username, null);
								if (res == null)
									System.exit(0);
								pword = res.toCharArray();
							}

							// Register
							logger.info("Registering account...");
							account = manager.registerAccount(accID, email, username,
									manager.createCredBytesWithPassword(pword));
						} else {
							// Register
							logger.info("Registering account...");
							account = manager.registerGuest(accID, username.substring(2));
						}
					} else {
						// Update
						logger.info("Updating account information...");
						if (email != null)
							account.updateEmail(email);
						account.performUpdateUsername(username);
					}

					// Assign data
					ProgressWindow.WindowAppender.setLabel("Configuring account...");
					logger.info("Configuring account...");
					account.setMultiplayerEnabled(multiplayerEnabled);
					account.setChatEnabled(chatEnabled);
					account.setStrictChatFilterEnabled(strictChatEnabled);

					// Read index
					logger.info("Indexing...");
					long index = readLong(strm);
					ProgressWindow.WindowAppender.setMax((int) index);

					// Import data
					logger.info("Importing account data...");
					ProgressWindow.WindowAppender.setLabel("Importing account data...");
					account.getAccountKeyValueContainer().deleteContainer();
					importData(logger, account.getAccountKeyValueContainer(), strm, "/");

					// Import saves
					logger.info("Importing account saves...");
					logger.info("Importing " + saveCount + " save(s)...");
					ProgressWindow.WindowAppender.setLabel("Importing account saves...");
					saveCount = readInt(strm);
					for (int i = 0; i < saveCount; i++) {
						// Read headers
						String id = readString(strm);
						String name = readString(strm);
						readLong(strm);
						logger.info("Importing save: " + name);
						ProgressWindow.WindowAppender.setLabel("Importing save '" + name + "'...");

						// Find save
						BasicAccountSaveContainer save = (BasicAccountSaveContainer) account.getSave(id);
						if (save != null)
							save.performUpdateUsername(name);
						else
							save = account.performCreateSave(id, name);

						// Import data
						logger.info("Importing save data...");
						save.getSaveData().deleteContainer();
						importData(logger, save.getSaveData(), strm, "/");

						// Increase progress
						ProgressWindow.WindowAppender.increaseProgress();
					}
					logger.info("Done!");
					deleteFile = true;
				} catch (Exception e) {
					logger.fatal("Failed to import account data!", e);
					if (ProgressWindow.shown) {
						try {
							SwingUtilities.invokeAndWait(() -> {
								String stackTrace = "";
								for (StackTraceElement ele : e.getStackTrace())
									stackTrace += "\n     At: " + ele;
								JOptionPane.showMessageDialog(null,
										"An error occured while running the importer\n\nError details: " + e
												+ stackTrace,
										"Import Error", JOptionPane.ERROR_MESSAGE);
							});
						} catch (InvocationTargetException | InterruptedException e1) {
						}
					}
					System.exit(1);
				}
			} finally {
				strm.close();
			}
			if (deleteFile)
				f.delete();
		}
		ProgressWindow.WindowAppender.closeWindow();
	}

	private void importData(Logger logger, AccountKvDataContainer cont, InputStream input, String pth)
			throws IOException {
		logger.info("Importing data container: " + pth);

		// Read keys
		int length = readInt(input);
		for (int i = 0; i < length; i++) {
			// Read key
			String key = readString(input);
			logger.info("Importing data key: " + (pth.equals("/") ? "/" + key : pth + "/" + key));

			// Read element
			boolean nonNull = readBoolean(input);
			String data = null;
			if (nonNull)
				data = readString(input);

			// Apply
			cont.setEntry(key, nonNull ? JsonParser.parseString(data) : null);

			// Increase progress
			ProgressWindow.WindowAppender.increaseProgress();
		}

		// Read containers
		length = readInt(input);
		for (int i = 0; i < length; i++) {
			// Read key
			String key = readString(input);

			// Read container
			importData(logger, cont.getChildContainer(key), input, pth.equals("/") ? "/" + key : pth + "/" + key);

			// Increase progress
			ProgressWindow.WindowAppender.increaseProgress();
		}
	}

	private void skipOverSaveData(FileInputStream input) throws IOException {
		// Read keys
		int length = readInt(input);
		for (int i = 0; i < length; i++) {
			// Skip over entry
			int l = readInt(input);
			input.getChannel().position(l + input.getChannel().position());
			if (readBoolean(input)) {
				l = readInt(input);
				input.getChannel().position(l + input.getChannel().position());
			}
		}

		// Read containers
		length = readInt(input);
		for (int i = 0; i < length; i++) {
			int l = readInt(input);
			input.getChannel().position(l + input.getChannel().position());
			skipOverSaveData(input);
		}
	}

	private boolean readBoolean(InputStream strm) throws IOException {
		return strm.read() == 1;
	}

	private String readString(InputStream strm) throws IOException {
		return new String(readByteArray(strm), "UTF-8");
	}

	private int readInt(InputStream strm) throws IOException {
		return ByteBuffer.wrap(readNBytes(strm, 4)).getInt();
	}

	private long readLong(InputStream strm) throws IOException {
		return ByteBuffer.wrap(readNBytes(strm, 8)).getLong();
	}

	private byte[] readByteArray(InputStream strm) throws IOException {
		return readNBytes(strm, readInt(strm));
	}

	private byte[] readNBytes(InputStream input, int num) throws IOException {
		byte[] res = new byte[num];
		int c = 0;
		while (true) {
			try {
				int r = input.read(res, c, num);
				if (r == -1)
					break;
				c += r;
			} catch (Exception e) {
				int b = input.read();
				if (b == -1)
					break;
				res[c++] = (byte) b;
			}
			if (c >= num)
				break;
		}
		return Arrays.copyOfRange(res, 0, c);
	}
}
