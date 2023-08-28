package org.asf.edge.modules.gridapi.commands.defaultcommands;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.modules.gridapi.commands.CommandContext;
import org.asf.edge.modules.gridapi.commands.IGridServerCommand;
import org.asf.edge.modules.gridapi.commands.TaskBasedCommand;
import org.asf.edge.modules.gridapi.identities.IdentityDef;
import org.asf.edge.modules.gridapi.identities.PropertyInfo;
import org.asf.edge.modules.gridapi.utils.EncryptionUtils;
import org.asf.edge.modules.gridapi.utils.IdentityUtils;
import org.asf.edge.modules.gridapi.utils.PhoenixToken;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ServerCommands extends TaskBasedCommand {

	@Override
	public String id() {
		return "servers";
	}

	@Override
	public String description(CommandContext ctx) {
		return "Server configuration commands";
	}

	@Override
	public IGridServerCommand[] tasks() {
		return new IGridServerCommand[] {

				// Identity register
				new IGridServerCommand() {

					@Override
					public String id() {
						return "create";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "[\"<owner-id>\"] [addresses...]";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Creates Phoenix server identities";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) throws Exception {
						// Load request into memory
						String owner = args.length >= 1 ? args[0] : null;
						String[] addrs = new String[args.length >= 2 ? args.length - 1 : 0];
						for (int i = 0; i < addrs.length; i++)
							addrs[i] = args[i + 1];
						if (owner != null) {
							if (AccountManager.getInstance().isUsernameTaken(owner))
								owner = AccountManager.getInstance().getAccountID(owner);
							if (owner == null || !IdentityUtils.identityExists(owner)) {
								outputWriteLineCallback
										.accept("Invalid argument: owner: owner identity not recognized");
								return null;
							}
						}

						// Build identity
						String ownerF = owner;
						@SuppressWarnings("serial")
						IdentityDef serverDef = IdentityUtils.createIdentity(new HashMap<String, PropertyInfo>() {
							{
								put("serverHost", new PropertyInfo() {
									{
										isReadonly = true;
										value = "true";
									}
								});
								put("owner", new PropertyInfo() {
									{
										isReadonly = true;
										value = ownerF == null ? new UUID(0, 0).toString() : ownerF;
									}
								});
							}
						});

						// Build certificate
						JsonArray addrsJson = new JsonArray();
						for (String addr : addrs)
							addrsJson.add(addr);
						CertificateDefinition cert = createCertificate(serverDef, addrsJson);

						// Build hosting token
						PhoenixToken tkn = new PhoenixToken();
						tkn.gameID = "nexusgrid";
						tkn.identity = serverDef.identity;
						tkn.lastUpdate = serverDef.lastUpdateTime;
						tkn.tokenExpiryTime = cert.expiry;
						tkn.tokenNotBefore = -1;
						tkn.tokenGenerationTime = System.currentTimeMillis() / 1000;
						tkn.capabilities = new String[] { "host", "idget" };

						// Return
						return "Server created successfully!\n\nID: " + serverDef.identity + "\nToken: "
								+ tkn.toTokenString()
								+ (addrs.length == 0
										? "\n\nPlease note that the server needs to be refreshed before it can be fully usable, there are no addresses assigned"
										: "");
					}

				},

				// Refresh
				new IGridServerCommand() {

					@Override
					public String id() {
						return "refresh";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "\"<id>\" <addresses...>";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Refreshes or reactivates Phoenix server identities";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) throws Exception {
						// Check arguments
						if (args.length < 1) {
							outputWriteLineCallback.accept("Missing argument: id");
							return null;
						}
						if (args.length < 2) {
							outputWriteLineCallback.accept("Missing argument: addresses");
							return null;
						}

						// Load request into memory
						String id = args[0];
						String[] addrs = new String[args.length - 1];
						for (int i = 0; i < addrs.length; i++)
							addrs[i] = args[i + 1];

						// Check identity
						if (!IdentityUtils.identityExists(id) || !IdentityUtils.getIdentity(id).properties
								.containsKey("serverCertificateProperties")) {
							// Invalid name
							outputWriteLineCallback.accept("Error: server not found");
							return null;
						}

						// Check if the owner is not host-banned
						IdentityDef serverDef = IdentityUtils.getIdentity(id);
						if (!serverDef.properties.get("owner").value.equals(new UUID(0, 0).toString())) {
							// Attempt to find account
							AccountObject acc = AccountManager.getInstance()
									.getAccount(serverDef.properties.get("owner").value);
							if (acc != null) {
								AccountDataContainer data = acc.getAccountData().getChildContainer("accountdata");
								if (data.entryExists("hostBanned") && data.getEntry("hostBanned").getAsBoolean()) {
									// Banned from hosting
									outputWriteLineCallback.accept("Error: server owner is host-banned");
									return null;
								}
							} else {
								// Attempt to find identity
								IdentityDef ownerDef = IdentityUtils
										.getIdentity(serverDef.properties.get("owner").value);
								if (ownerDef != null && ownerDef.properties.containsKey("hostBanned")
										&& ownerDef.properties.get("hostBanned").value.equals("true")) {
									// Banned from hosting
									outputWriteLineCallback.accept("Error: server owner is host-banned");
									return null;
								} else if (ownerDef == null) {
									// Owner was deleted
									IdentityUtils.deleteIdentity(serverDef.identity);
									outputWriteLineCallback.accept("Error: server not found");
									return null;
								}
							}
						}

						// Build certificate
						JsonArray addrsJson = new JsonArray();
						for (String addr : addrs)
							addrsJson.add(addr);
						CertificateDefinition cert = createCertificate(serverDef, addrsJson);

						// Build hosting token
						PhoenixToken tkn = new PhoenixToken();
						tkn.gameID = "nexusgrid";
						tkn.identity = serverDef.identity;
						tkn.lastUpdate = serverDef.lastUpdateTime;
						tkn.tokenExpiryTime = cert.expiry;
						tkn.tokenNotBefore = -1;
						tkn.tokenGenerationTime = System.currentTimeMillis() / 1000;
						tkn.capabilities = new String[] { "host", "idget" };

						// Set response
						return "Server refreshed successfully, token: " + tkn.toTokenString();
					}

				},

				// Identity delete
				new IGridServerCommand() {

					@Override
					public String id() {
						return "delete";
					}

					@Override
					public String syntax(CommandContext ctx) {
						return "\"<id>\"";
					}

					@Override
					public String description(CommandContext ctx) {
						return "Deletes Phoenix server identities";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) throws Exception {
						// Check arguments
						if (args.length < 1) {
							outputWriteLineCallback.accept("Missing argument: id");
							return null;
						}

						// Load request into memory
						String id = args[0];

						// Check identity
						if (!IdentityUtils.identityExists(id) || !IdentityUtils.getIdentity(id).properties
								.containsKey("serverCertificateProperties")) {
							// Invalid name
							outputWriteLineCallback.accept("Error: server not found");
							return null;
						}

						// Delete
						IdentityUtils.deleteIdentity(id);
						return "Server identity deleted successfully";
					}

				},

				// Identity list
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
						return "Lists all Phoenix server identities";
					}

					@Override
					public String run(String[] args, CommandContext ctx, Logger logger,
							Consumer<String> outputWriteLineCallback, Map<String, String> dataBlobs) throws Exception {
						StringBuilder message = new StringBuilder("List of all Phoenix server identities:");
						for (IdentityDef id : IdentityUtils.getAll()) {
							if (id.properties.containsKey("serverCertificateProperties")) {
								IdentityDef owner = null;
								if (id.properties.containsKey("owner"))
									owner = IdentityUtils.getIdentity(id.properties.get("owner").value);
								message.append("\n - " + id.identity + " - " + (owner == null ? "UNKNOWN OWNER"
										: owner.identity
												+ (" (display name: " + (owner.properties.containsKey("displayName")
														? owner.properties.get("displayName").value
														: "UNKNOWN") + ")"))
										+ (owner != null && owner.properties.containsKey("hostBanned")
												&& owner.properties.get("hostBanned").value.equalsIgnoreCase("true")
														? " [HOST-BANNED OWNER]"
														: ""));
							}
						}
						return message.toString();
					}

				} };
	}

	// Creates certificate objects
	private CertificateDefinition createCertificate(IdentityDef id, JsonArray addresses)
			throws NoSuchAlgorithmException {
		// Create keys
		KeyPairGenerator fac = KeyPairGenerator.getInstance("RSA");
		KeyPair keys = fac.generateKeyPair();

		// Create certificate
		CertificateDefinition cert = new CertificateDefinition();
		cert.timestamp = System.currentTimeMillis();
		cert.expiry = System.currentTimeMillis() + 2592000000l; // 30 days

		// Create json
		JsonObject obj = new JsonObject();
		obj.addProperty("lastUpdate", cert.timestamp);
		obj.addProperty("expiry", cert.expiry);
		obj.add("addresses", addresses);
		obj.addProperty("publicKey", EncryptionUtils.pemEncode(keys.getPublic().getEncoded(), "PUBLIC"));
		obj.addProperty("privateKey", EncryptionUtils.pemEncode(keys.getPrivate().getEncoded(), "PRIVATE"));
		PropertyInfo prop = new PropertyInfo();
		prop.isReadonly = true;
		prop.value = obj.toString();
		id.properties.put("serverCertificateProperties", prop);

		// Save
		IdentityUtils.updateIdentity(id);
		return cert;
	}

	private static class CertificateDefinition {

		public long timestamp;
		public long expiry;

	}

}
