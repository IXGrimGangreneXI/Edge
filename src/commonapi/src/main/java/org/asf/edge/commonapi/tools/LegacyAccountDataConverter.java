package org.asf.edge.commonapi.tools;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.common.EdgeServerEnvironment;
import org.asf.edge.common.services.ServiceImplementationPriorityLevels;
import org.asf.edge.common.services.ServiceManager;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.impl.BasicAccountManager;
import org.asf.edge.common.services.accounts.impl.BasicAccountObject;
import org.asf.edge.common.services.accounts.impl.BasicAccountSaveContainer;
import org.asf.edge.common.services.commondata.CommonDataManager;
import org.asf.edge.common.services.items.ItemManager;
import org.asf.edge.common.services.items.impl.ItemManagerImpl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

public class LegacyAccountDataConverter {

	public static void main(String[] args) throws ClassNotFoundException {
		// Usage:
		// <old database url> ["<key>" "<value>"...]
		if (args.length < 1) {
			System.err.println("Usage: \"<old database URL>\" [\"<key>\" \"<value>\"...]");
			System.exit(1);
			return;
		}
		ArrayList<String> a = new ArrayList<String>(List.of(args));
		a.remove(0);

		// Check arguments
		// Make sure properties are in pairs of two
		if (a.size() % 2 != 0) {
			System.err.println("Usage: \"<old database URL>\" [\"<key>\" \"<value>\"...]");
			System.exit(1);
			return;
		}

		// Setup
		EdgeServerEnvironment.initAll();
		Class.forName("com.mysql.cj.jdbc.Driver");
		Class.forName("org.mariadb.jdbc.Driver");
		Class.forName("org.asf.edge.common.jdbc.LoggingProxyDriver");
		Class.forName("org.asf.edge.common.jdbc.LockingDriver");

		// Logger
		Logger logger = LogManager.getLogger("CONVERTER");
		logger.info("Preparing to start...");

		// Load properties
		logger.info("Loading properties...");
		Properties props = new Properties();
		for (int i = 0; i < a.size(); i += 2) {
			props.setProperty(a.get(i), a.get(i + 1));
		}

		// Prepare services
		logger.info("Loading managers...");
		logger.debug("Loading account manager implementations...");
		AccountManager.initAccountManagerServices(ServiceImplementationPriorityLevels.DEFAULT,
				ServiceImplementationPriorityLevels.NORMAL, -5);
		logger.debug("Selecting account manager implementation...");
		ServiceManager.selectServiceImplementation(AccountManager.class);
		logger.debug("Loading account manager...");
		AccountManager.getInstance().loadManager();
		logger.debug("Loading common data manager implementations...");
		CommonDataManager.initCommonDataManagerServices(ServiceImplementationPriorityLevels.DEFAULT,
				ServiceImplementationPriorityLevels.NORMAL, -5);
		logger.debug("Selecting common data manager implementation...");
		ServiceManager.selectServiceImplementation(CommonDataManager.class);
		logger.debug("Loading common data manager...");
		CommonDataManager.getInstance().loadManager();
		logger.debug("Setting up item manager...");
		ServiceManager.registerServiceImplementation(ItemManager.class, new ItemManagerImpl(),
				ServiceImplementationPriorityLevels.DEFAULT);
		ServiceManager.selectServiceImplementation(ItemManager.class);

		// Check implementation
		BasicAccountManager mgr = null;
		if (AccountManager.getInstance() instanceof BasicAccountManager)
			mgr = (BasicAccountManager) AccountManager.getInstance();
		if (mgr == null) {
			System.err.println(
					"Incompatible account managers implementation, requiring a implementation based on the BasicAccountManager class.");
			System.exit(1);
			return;
		}

		// Connect to old database
		logger.info("Connecting to database...");
		try {
			DriverManager.getConnection(args[0], props).close();
		} catch (SQLException e) {
			logger.error("Connection failure!", e);
			System.exit(1);
			return;
		}

		// Index
		long accountCount = 0;
		try {
			Connection conn = DriverManager.getConnection(args[0], props);
			try {
				var statement = conn.prepareStatement("SELECT ID, USERNAME FROM USERMAP");
				ResultSet res = statement.executeQuery();
				while (res.next())
					accountCount++;
				res.close();
				statement.close();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Indexing failure!", e);
			System.exit(1);
			return;
		}
		logger.info("Beginning migration of " + accountCount + " accounts...");

		// Migrate
		try {
			Connection conn = DriverManager.getConnection(args[0], props);
			try {
				long i = 0;
				var statement = conn.prepareStatement("SELECT ID, USERNAME, CREDS FROM USERMAP");
				ResultSet res = statement.executeQuery();
				while (res.next()) {
					i++;

					// Pull
					String id = res.getString("ID");
					String name = res.getString("USERNAME");
					byte[] cred = res.getBytes("CREDS");

					// Pull email
					var st2 = conn.prepareStatement("SELECT EMAIL FROM EMAILMAP WHERE ID = ?");
					st2.setString(1, id);
					ResultSet rs2 = st2.executeQuery();
					String email = null;
					if (rs2.next())
						email = rs2.getString("EMAIL");
					rs2.close();
					st2.close();

					// Register
					logger.info("[" + i + " / " + accountCount + "] Converting " + name + "...");
					BasicAccountObject account;
					if (!name.startsWith("g/"))
						account = mgr.registerAccount(id, email, name, cred);
					else
						account = mgr.registerGuest(id, name.substring(2));
					String step = "STEP 1/3";
					logger.info(
							"[" + i + " / " + accountCount + "] [" + step + "] Converting saves of " + name + "...");

					// Find saves
					st2 = conn.prepareStatement("SELECT SAVES FROM SAVEMAP WHERE ACCID = ?");
					st2.setString(1, id);
					rs2 = st2.executeQuery();
					String savesD = null;
					if (rs2.next())
						savesD = rs2.getString("SAVES");
					rs2.close();
					st2.close();
					if (savesD == null) {
						logger.warn("Save list corrupted of " + id + ", unable to convert save data!");
					} else {
						// Convert saves
						JsonArray saveData = JsonParser.parseString(savesD).getAsJsonArray();
						int i2 = 1;
						for (JsonElement ele : saveData) {
							JsonObject save = ele.getAsJsonObject();
							String svID = save.get("id").getAsString();
							String svName = save.get("username").getAsString();
							logger.info("[" + i + " / " + accountCount + "] [" + step + "] [SAVE " + i2 + " / "
									+ saveData.size() + "] Converting save " + svName + "...");

							// Create save
							BasicAccountSaveContainer sv = account.performCreateSave(svID, svName);
							if (sv == null) {
								logger.error("Migration failure! Save creation failure!");
								System.exit(1);
								return;
							}

							// Migrate data
							logger.info("[" + i + " / " + accountCount + "] [" + step + "] [SAVE " + i2 + " / "
									+ saveData.size() + "] Migrating data of save " + svName + "...");
							migrateDataContainer(svID + "//", "SAVESPECIFICPLAYERDATA", sv.getSaveData(), conn, logger,
									"[" + i + " / " + accountCount + "] [" + step + "] [SAVE " + i2 + " / "
											+ saveData.size() + "] ");

							// Update inventories
							logger.info("[" + i + " / " + accountCount + "] [" + step + "] [SAVE " + i2++ + " / "
									+ saveData.size() + "] Updating inventory format of save " + svName + "...");
							updateInventories(sv.getSaveData(), logger);
						}
					}

					// Convert data
					step = "STEP 2/3";
					logger.info("[" + i + " / " + accountCount + "] [" + step + "] Converting account data of account "
							+ name + "...");
					migrateDataContainer(id + "//", "ACCOUNTWIDEPLAYERDATA", account.getAccountData(), conn, logger,
							"[" + i + " / " + accountCount + "] [" + step + "] ");

					// Update inventories
					step = "STEP 3/3";
					logger.info("[" + i + " / " + accountCount + "] [" + step
							+ "] Updating inventory format of account " + name + "...");
					updateInventories(account.getAccountData(), logger);
				}
				res.close();
				statement.close();
			} finally {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Migration failure!", e);
			System.exit(1);
			return;
		}

		// Finished
		logger.info("Done!");
	}

	private static void updateInventories(AccountDataContainer accountData, Logger logger) {
		// Retrieve old common inventory data
		try {
			AccountDataContainer legacyData = accountData.getChildContainer("legacy-common-inventories");
			for (String container : legacyData.getChildContainers()) {
				int containerID = Integer.parseInt(container);

				// Retrieve new inventory
				AccountDataContainer oldC = legacyData.getChildContainer(container);

				// Migrate data
				String[] k = oldC.getEntryKeys();
				for (String key : k) {
					if (key.startsWith("item-")) {
						// Parse
						int uniqueID = Integer.parseInt(key.substring(5));

						// Read item
						JsonObject itm = oldC.getEntry(key).getAsJsonObject();
						int defID = itm.get("id").getAsInt();
						int quantity = itm.get("quantity").getAsInt();
						int uses = itm.get("uses").getAsInt();

						// Save
						itm = new JsonObject();
						itm.addProperty("quantity", quantity);
						itm.addProperty("uses", uses);
						accountData.getChildContainer("commoninventories").getChildContainer("c-" + containerID)
								.getChildContainer("d-" + defID).setEntry("u-" + uniqueID, itm);

						// Write def ID
						accountData.getChildContainer("commoninventories").getChildContainer("c-" + containerID)
								.setEntry("u-" + uniqueID, new JsonPrimitive(defID));
					}
				}
			}
			legacyData.deleteContainer();
		} catch (IOException e) {
			logger.error("Failed to migrate legacy inventories!", e);
		}
	}

	private static void migrateDataContainer(String root, String table, AccountDataContainer saveData, Connection conn,
			Logger logger, String logPrefix) {
		// Find entries
		try {
			var st2 = conn.prepareStatement("SELECT DATA FROM " + table + " WHERE PATH = ?");
			st2.setString(1, root + "datamap");
			ResultSet rs2 = st2.executeQuery();
			if (rs2.next()) {
				// Read
				logger.info(logPrefix + "Converting data container "
						+ (root.endsWith("//") ? root.substring(0, root.lastIndexOf("//")) : root) + "...");
				JsonArray entries = JsonParser.parseString(rs2.getString("DATA")).getAsJsonArray();
				for (JsonElement ele : entries) {
					// Find all entries
					String key = ele.getAsString();
					String pth = root + key;
					if (pth.endsWith("/")) {
						// Convert child container
						pth = pth.substring(0, pth.lastIndexOf("/"));
						key = key.substring(0, key.lastIndexOf("/"));
						try {
							String rootID = saveData.getAccount().getAccountID();
							if (saveData.getSave() != null)
								rootID = saveData.getSave().getSaveID();
							if (pth.equals(rootID + "//commoninventories")) {
								key = "legacy-common-inventories";
							}
							migrateDataContainer(pth + "/", table, saveData.getChildContainer(key), conn, logger,
									logPrefix);
						} catch (IOException e) {
							logger.error("Failed to migrate data container " + pth + " (" + table + ")", e);
						}
					} else if (!key.equals("datamap")) {
						var st3 = conn.prepareStatement("SELECT DATA FROM " + table + " WHERE PATH = ?");
						st3.setString(1, pth);
						ResultSet rs3 = st3.executeQuery();
						if (rs3.next()) {
							logger.info(logPrefix + "Converting data key " + pth + "...");
							try {
								saveData.setEntry(key, JsonParser.parseString(rs3.getString("DATA")));
							} catch (JsonSyntaxException | IOException e) {
								logger.error("Failed to migrate data key " + pth + " (" + table + ")", e);
							}
						}
						rs3.close();
						st3.close();
					}
				}
			}
			rs2.close();
			st2.close();
		} catch (SQLException e) {
			logger.error("Failed to migrate data container " + root + " (" + table + ")", e);
		}
	}

}
