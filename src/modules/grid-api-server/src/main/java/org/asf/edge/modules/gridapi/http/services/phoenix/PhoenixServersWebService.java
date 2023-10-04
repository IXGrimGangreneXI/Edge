package org.asf.edge.modules.gridapi.http.services.phoenix;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;

import org.asf.connective.processors.HttpPushProcessor;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.http.apihandlerutils.functions.*;
import org.asf.edge.common.io.DataWriter;
import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;
import org.asf.edge.modules.gridapi.EdgeGridApiServer;
import org.asf.edge.modules.gridapi.identities.IdentityDef;
import org.asf.edge.modules.gridapi.identities.PropertyInfo;
import org.asf.edge.modules.gridapi.serverlist.ServerListEntry;
import org.asf.edge.modules.gridapi.serverlist.ServerListManager;
import org.asf.edge.modules.gridapi.utils.EncryptionUtils;
import org.asf.edge.modules.gridapi.utils.IdentityUtils;
import org.asf.edge.modules.gridapi.utils.PhoenixToken;
import org.asf.edge.modules.gridapi.utils.TokenUtils;
import org.asf.edge.modules.gridapi.utils.TokenUtils.AccessContext;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PhoenixServersWebService extends EdgeWebService<EdgeGridApiServer> {

	public PhoenixServersWebService(EdgeGridApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new PhoenixServersWebService(getServerInstance());
	}

	@Override
	public String path() {
		return "/servers";
	}

	@Function(allowedMethods = { "POST" })
	public FunctionResult authenticatePlayer(FunctionInfo func) throws IOException {
		// Load token
		AccessContext ctx = TokenUtils.fromFunction(func, "host");
		if (ctx == null || !ctx.identity.properties.containsKey("serverCertificateProperties")) {
			return response(401, "Unauthorized");
		}

		// Check if the owner is not host-banned
		IdentityDef serverDef = ctx.identity;
		if (!serverDef.properties.get("owner").value.equals(new UUID(0, 0).toString())) {
			// Attempt to find account
			AccountObject acc = AccountManager.getInstance().getAccount(serverDef.properties.get("owner").value);
			if (acc != null) {
				AccountDataContainer data = acc.getAccountData().getChildContainer("accountdata");
				if (data.entryExists("hostBanned") && data.getEntry("hostBanned").getAsBoolean()) {
					// Banned from hosting
					return response(401, "Unauthorized");
				}
			} else {
				// Attempt to find identity
				IdentityDef ownerDef = IdentityUtils.getIdentity(serverDef.properties.get("owner").value);
				if (ownerDef != null && ownerDef.properties.containsKey("hostBanned")
						&& ownerDef.properties.get("hostBanned").value.equals("true")) {
					// Banned from hosting
					return response(401, "Unauthorized");
				} else if (ownerDef == null) {
					// Owner was deleted
					IdentityUtils.deleteIdentity(serverDef.identity);
					return response(401, "Unauthorized");
				}
			}
		}

		// Read payload
		if (!getRequest().hasRequestBody())
			return response(400, "Bad request");
		JsonObject payload = JsonParser.parseString(getRequestBodyAsString()).getAsJsonObject();
		if (!payload.has("secret") || !payload.get("secret").isJsonPrimitive()) {
			return response(400, "Bad request");
		}

		// Find ID
		String playerID = PhoenixAuthWebService.authenticatePlayer(payload.get("secret").getAsString(),
				ctx.identity.identity);
		if (playerID == null) {
			// Failed
			return response(400, "Bad Request", "application/json", "{\"error\":\"invalid_secret\"}");
		}

		// Find account
		AccountObject account = AccountManager.getInstance().getAccount(playerID);
		if (account == null) {
			// Failed
			return response(400, "Bad Request", "application/json", "{\"error\":\"invalid_secret\"}");
		}

		// Generate response
		JsonObject response = new JsonObject();
		response.addProperty("accountID", playerID);
		response.addProperty("displayName", account.getUsername());
		return ok("application/json", response.toString());
	}

	@Function(value = "serverlist/nexusgrid")
	public FunctionResult serverList(FunctionInfo func) throws IOException {
		// Load payload
		JsonObject payload = new JsonObject();
		try {
			if (func.getRequest().hasRequestBody())
				payload = JsonParser.parseString(func.getRequest().getRequestBodyAsString()).getAsJsonObject();
		} catch (Exception e) {
		}

		// Add filters
		HashMap<String, String> filters = new HashMap<String, String>();
		for (String key : payload.keySet()) {
			if (!payload.get(key).isJsonPrimitive()) {
				return response(400, "Bad Request", "text/plain", "Filter properties can only be strings\n");
			}
			filters.put(key, payload.get(key).getAsString());
		}

		// List servers
		String response = "";
		for (ServerListEntry entry : ServerListManager.getServers(filters)) {
			// Create entry
			JsonObject server = new JsonObject();
			server.addProperty("id", entry.serverId);
			server.addProperty("ownerId", entry.ownerId);
			server.addProperty("version", entry.version);
			JsonObject protocol = new JsonObject();
			protocol.addProperty("programVersion", entry.protocolVersion);
			protocol.addProperty("phoenixVersion", entry.phoenixProtocolVersion);
			server.add("protocol", protocol);
			JsonArray addresses = new JsonArray();
			for (String addr : entry.addresses)
				addresses.add(addr);
			server.add("addresses", addresses);
			server.addProperty("port", entry.port);

			// Build info map
			JsonObject info = new JsonObject();
			for (String key : entry.entries.keySet())
				info.addProperty(key, entry.entries.get(key));
			server.add("details", info);

			response += server + "\n";
		}
		return ok("text/plain", response);
	}

	@Function(allowedMethods = { "POST" }, value = "postservertolist")
	public FunctionResult serverListPost(FunctionInfo func) throws IOException {
		// Load token
		TokenUtils.AccessContext ctx = TokenUtils.fromFunction(func, "host");
		if (ctx == null || !ctx.isServer) {
			return response(401, "Unauthorized", "text/plain", "Server token invalid\n");
		}

		// Check if the owner is not host-banned
		IdentityDef serverDef = ctx.identity;
		if (!serverDef.properties.get("owner").value.equals(new UUID(0, 0).toString())) {
			// Attempt to find account
			AccountObject acc = AccountManager.getInstance().getAccount(serverDef.properties.get("owner").value);
			if (acc != null) {
				AccountDataContainer data = acc.getAccountData().getChildContainer("accountdata");
				if (data.entryExists("hostBanned") && data.getEntry("hostBanned").getAsBoolean()) {
					// Banned from hosting
					return response(401, "Unauthorized");
				}
			} else {
				// Attempt to find identity
				IdentityDef ownerDef = IdentityUtils.getIdentity(serverDef.properties.get("owner").value);
				if (ownerDef != null && ownerDef.properties.containsKey("hostBanned")
						&& ownerDef.properties.get("hostBanned").value.equals("true")) {
					// Banned from hosting
					return response(401, "Unauthorized");
				} else if (ownerDef == null) {
					// Owner was deleted
					IdentityUtils.deleteIdentity(serverDef.identity);
					return response(401, "Unauthorized");
				}
			}
		}

		// Check payload
		if (!func.getRequest().hasRequestBody())
			return response(400, "Bad Request", "text/plain",
					"Missing one or more of the following JSON fields: port (integer), version (string), protocol (integer), phoenixProtocol (integer)\n");

		// Load payload
		JsonObject payload;
		try {
			payload = JsonParser.parseString(func.getRequest().getRequestBodyAsString()).getAsJsonObject();
			if (!payload.has("port") || !payload.has("version") || !payload.has("protocol")
					|| !payload.has("phoenixProtocol") || !payload.get("version").isJsonPrimitive())
				throw new IllegalArgumentException("Missing properties");
			payload.get("port").getAsInt();
			payload.get("protocol").getAsInt();
			payload.get("phoenixProtocol").getAsInt();
		} catch (Exception e) {
			return response(400, "Bad Request", "text/plain",
					"Missing one or more of the following JSON fields: port (integer), version (string), protocol (integer), phoenixProtocol (integer)\n");
		}

		// Find certificate for connection details
		JsonObject certificate;
		try {
			// Verify properties
			certificate = JsonParser.parseString(serverDef.properties.get("serverCertificateProperties").value)
					.getAsJsonObject();
			if (!certificate.has("addresses") || !certificate.get("addresses").isJsonArray())
				throw new IllegalArgumentException();
			if (!certificate.has("lastUpdate") || !certificate.get("lastUpdate").isJsonPrimitive())
				throw new IllegalArgumentException();
			else
				certificate.get("lastUpdate").getAsLong(); // Verify timestamp
			if (!certificate.has("expiry") || !certificate.get("expiry").isJsonPrimitive())
				throw new IllegalArgumentException();
			else
				certificate.get("expiry").getAsLong(); // Verify expiry
			if (!certificate.has("privateKey") || !certificate.get("privateKey").isJsonPrimitive())
				throw new IllegalArgumentException();
			if (!certificate.has("publicKey") || !certificate.get("publicKey").isJsonPrimitive())
				throw new IllegalArgumentException();
		} catch (Exception e) {
			return response(400, "Bad Request", "text/plain", "Corrupted certificate on server, please refresh it\n");
		}

		// Allowed addresses
		ArrayList<String> addresses = new ArrayList<String>();
		for (JsonElement ele : certificate.get("addresses").getAsJsonArray())
			addresses.add(ele.getAsString());
		if (addresses.size() == 0) {
			// Lets not
			return response(400, "Bad Request", "text/plain",
					"No addresses set for this server, please add server addresses before using the server list\n");
		}

		// Attempt to connect with the server, and verify protocols
		int port = payload.get("port").getAsInt();
		int protocol = payload.get("protocol").getAsInt();
		int phoenixProtocol = payload.get("phoenixProtocol").getAsInt();
		byte[] hello = ("PHOENIX/HELLO/" + phoenixProtocol + "/").getBytes();
		byte[] helloSrv = ("PHOENIX/HELLO/SERVER/" + phoenixProtocol + "/").getBytes();
		try {
			boolean success = false;
			for (String addr : addresses) {
				Socket sock = new Socket();
				try {
					// Attempt to connect
					sock.connect(new InetSocketAddress(addr, port), 1000);
					success = true;
				} catch (IOException e) {
					continue;
				}

				// Handshake
				sock.getOutputStream().write(hello);
				try {
					for (int i = 0; i < helloSrv.length; i++) {
						int r = sock.getInputStream().read();
						if (r == -1 || (byte) r != helloSrv[i]) {
							// Handshake failure
							sock.close();
							return response(400, "Bad Request", "text/plain", "Server on IP " + addr + " with port "
									+ port + " responded with an invalid Phoenix Early Handshake.\n");
						}
					}
				} catch (Exception e) {
					// Handshake failure
					sock.close();
					return response(400, "Bad Request", "text/plain", "Server on IP " + addr + " with port " + port
							+ " responded with an invalid Phoenix Early Handshake.\n");
				}

				// Write endpoint
				DataWriter wr = new DataWriter(sock.getOutputStream());
				wr.writeString(addr);
				wr.writeInt(port);

				// Set mode to info
				sock.getOutputStream().write(0);

				// Read game ID
				String gameID = new String(
						sock.getInputStream().readNBytes(ByteBuffer.wrap(sock.getInputStream().readNBytes(4)).getInt()),
						"UTF-8");

				// Read server ID
				String serverID = new String(
						sock.getInputStream().readNBytes(ByteBuffer.wrap(sock.getInputStream().readNBytes(4)).getInt()),
						"UTF-8");

				// Read encryption status
				boolean encrypted = sock.getInputStream().read() == 1;
				sock.close();

				// Handle response
				if (!encrypted) {
					// Insecure-mode
					return response(400, "Bad Request", "text/plain",
							"Listing of insecure-mode servers is not allowed.\n");
				}
				if (!gameID.equalsIgnoreCase(PhoenixToken.GAME_ID)) {
					// Invalid ID
					return response(400, "Bad Request", "text/plain", "Game ID does not match.\n");
				}
				if (!serverID.equalsIgnoreCase(serverDef.identity)) {
					// Invalid ID
					return response(400, "Bad Request", "text/plain", "Server ID does not match.\n");
				}
			}
			if (!success)
				throw new IOException("No successful handshake");
		} catch (Exception e) {
			// Handshake failure
			return response(400, "Bad Request", "text/plain",
					"Could not connect to the server from the Phoenix API servers, please make sure your server can be reached. (check port forwarding)\n");
		}

		// Check headers
		if (!getRequest().hasHeader("Upgrade")
				|| !getRequest().getHeaderValue("Upgrade").equals("PHOENIXSERVERLISTCLIENT")) {
			return response(400, "Bad request");
		}

		// Write HTTP success
		func.getResponse().addHeader("X-Response-ID", UUID.randomUUID().toString());
		setResponseHeader("Upgrade", "PHOENIXSERVERLISTCLIENT");

		// Setup
		AsyncTaskManager.runAsync(() -> {
			// Wait for upgrade
			while (func.getClient().isConnected()) {
				// Check
				if (func.getResponse().hasHeader("Upgraded")
						&& func.getResponse().getHeaderValue("Upgraded").equalsIgnoreCase("true"))
					break;

				// Wait
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			}

			// Check
			if ((func.getClient().isConnected())) {
				ServerListManager.onPostServer(serverDef, func.getClient(), port, addresses.toArray(t -> new String[t]),
						payload.get("version").getAsString(), protocol, phoenixProtocol);
			}
		});

		// Send response
		return response(101, "Switching Protocols");
	}

	@Function(allowedMethods = { "POST" }, value = "updateserverstatus")
	public FunctionResult updateServerListStatus(FunctionInfo func) throws IOException {
		// Load token
		TokenUtils.AccessContext ctx = TokenUtils.fromFunction(func, "host");
		if (ctx == null || !ctx.isServer) {
			return response(401, "Unauthorized", "text/plain", "Server token invalid\n");
		}

		// Check if the owner is not host-banned

		// Check if the owner is not host-banned
		IdentityDef serverDef = ctx.identity;
		if (!serverDef.properties.get("owner").value.equals(new UUID(0, 0).toString())) {
			// Attempt to find account
			AccountObject acc = AccountManager.getInstance().getAccount(serverDef.properties.get("owner").value);
			if (acc != null) {
				AccountDataContainer data = acc.getAccountData().getChildContainer("accountdata");
				if (data.entryExists("hostBanned") && data.getEntry("hostBanned").getAsBoolean()) {
					// Banned from hosting
					return response(401, "Unauthorized");
				}
			} else {
				// Attempt to find identity
				IdentityDef ownerDef = IdentityUtils.getIdentity(serverDef.properties.get("owner").value);
				if (ownerDef != null && ownerDef.properties.containsKey("hostBanned")
						&& ownerDef.properties.get("hostBanned").value.equals("true")) {
					// Banned from hosting
					return response(401, "Unauthorized");
				} else if (ownerDef == null) {
					// Owner was deleted
					IdentityUtils.deleteIdentity(serverDef.identity);
					return response(401, "Unauthorized");
				}
			}
		}

		// Load payload
		JsonObject payload;
		try {
			if (!func.getRequest().hasRequestBody())
				return response(400, "Bad Request");
			payload = JsonParser.parseString(func.getRequest().getRequestBodyAsString()).getAsJsonObject();
		} catch (Exception e) {
			return response(400, "Bad Request");
		}

		// Find entry
		ServerListEntry entry = ServerListManager.getServer(serverDef.identity);
		if (entry == null) {
			return response(400, "Bad Request");
		}

		// Add options
		LinkedHashMap<String, String> entries = new LinkedHashMap<String, String>();
		for (String key : payload.keySet()) {
			if (!payload.get(key).isJsonPrimitive()) {
				return response(400, "Bad Request");
			}
			entries.put(key, payload.get(key).getAsString());
		}
		ServerListManager.onPostServerUpdate(serverDef.identity, entries);
		return ok();
	}

	@Function(value = "certificate/nexusgrid", allowSubPaths = true)
	public FunctionResult downloadCertificate(FunctionInfo func) throws IOException {
		// Verify parameters
		if (func.getRequestedPath().split("/").length != 3) {
			// Invalid parameters
			return response(404, "Not found");
		}

		// Find server
		String sid = func.getRequestedPath().split("/")[2];
		IdentityDef serverDef = IdentityUtils.getIdentity(sid);

		// Check if the owner is not host-banned
		if (serverDef == null || !serverDef.properties.containsKey("owner")
				|| !serverDef.properties.containsKey("serverCertificateProperties"))
			return response(404, "Not found");
		if (!serverDef.properties.get("owner").value.equals(new UUID(0, 0).toString())) {
			// Attempt to find account
			AccountObject acc = AccountManager.getInstance().getAccount(serverDef.properties.get("owner").value);
			if (acc != null) {
				AccountDataContainer data = acc.getAccountData().getChildContainer("accountdata");
				if (data.entryExists("hostBanned") && data.getEntry("hostBanned").getAsBoolean()) {
					// Banned from hosting
					return response(401, "Unauthorized");
				}
			} else {
				// Attempt to find identity
				IdentityDef ownerDef = IdentityUtils.getIdentity(serverDef.properties.get("owner").value);
				if (ownerDef != null && ownerDef.properties.containsKey("hostBanned")
						&& ownerDef.properties.get("hostBanned").value.equals("true")) {
					// Banned from hosting
					return response(401, "Unauthorized");
				} else if (ownerDef == null) {
					// Owner was deleted
					IdentityUtils.deleteIdentity(serverDef.identity);
					return response(401, "Unauthorized");
				}
			}
		}

		// Verify server identity configuration
		JsonObject certificate;
		PublicKey pubKey;
		try {
			// Verify properties
			certificate = JsonParser.parseString(serverDef.properties.get("serverCertificateProperties").value)
					.getAsJsonObject();
			if (!certificate.has("addresses") || !certificate.get("addresses").isJsonArray())
				throw new IllegalArgumentException();
			if (!certificate.has("lastUpdate") || !certificate.get("lastUpdate").isJsonPrimitive())
				throw new IllegalArgumentException();
			else
				certificate.get("lastUpdate").getAsLong(); // Verify timestamp
			if (!certificate.has("expiry") || !certificate.get("expiry").isJsonPrimitive())
				throw new IllegalArgumentException();
			else
				certificate.get("expiry").getAsLong(); // Verify expiry
			if (!certificate.has("privateKey") || !certificate.get("privateKey").isJsonPrimitive())
				throw new IllegalArgumentException();
			if (!certificate.has("publicKey") || !certificate.get("publicKey").isJsonPrimitive())
				throw new IllegalArgumentException();

			// Attempt to parse the keys
			KeyFactory fac = KeyFactory.getInstance("RSA");
			pubKey = fac.generatePublic(
					new X509EncodedKeySpec(EncryptionUtils.pemDecode(certificate.get("publicKey").getAsString())));
		} catch (Exception e) {
			// Invalid server id
			return response(404, "Not Found");
		}

		// Create certificate document
		ByteArrayOutputStream strm = new ByteArrayOutputStream();
		// Game id
		byte[] id = "nexusgrid".getBytes("UTF-8");
		strm.write(ByteBuffer.allocate(4).putInt(id.length).array());
		strm.write(id);
		// Server ID
		id = serverDef.identity.getBytes("UTF-8");
		strm.write(ByteBuffer.allocate(4).putInt(id.length).array());
		strm.write(id);
		// Allowed addresses
		ArrayList<String> addresses = new ArrayList<String>();
		for (JsonElement ele : certificate.get("addresses").getAsJsonArray())
			addresses.add(ele.getAsString());
		strm.write(ByteBuffer.allocate(4).putInt(addresses.size()).array());
		for (String addr : addresses) {
			byte[] d = addr.getBytes("UTF-8");
			strm.write(ByteBuffer.allocate(4).putInt(d.length).array());
			strm.write(d);
		}
		// Timestamp
		strm.write(ByteBuffer.allocate(8).putLong(certificate.get("lastUpdate").getAsLong()).array());
		// Expiry
		strm.write(ByteBuffer.allocate(8).putLong(certificate.get("expiry").getAsLong()).array());
		// Public key
		strm.write(EncryptionUtils.pemEncode(pubKey.getEncoded(), "PUBLIC").getBytes("UTF-8"));
		strm.close();

		// Set response
		func.getResponse().addHeader("Content-Disposition",
				"attachment; filename=\"" + serverDef.identity + ".pxcert\"");
		return ok("application/px-public-certificate", new ByteArrayInputStream(strm.toByteArray()));
	}

	@Function(allowedMethods = { "POST" }, value = "refreshserver")
	public FunctionResult refreshServer(FunctionInfo func) throws NoSuchAlgorithmException, IOException {
		// Load token
		TokenUtils.AccessContext ctx = TokenUtils.fromFunction(func, "host");
		if (ctx == null || !ctx.isServer) {
			return response(401, "Unauthorized", "text/plain", "Server token invalid\n");
		}

		// Check if the owner is not host-banned
		IdentityDef serverDef = ctx.identity;
		if (!serverDef.properties.get("owner").value.equals(new UUID(0, 0).toString())) {
			// Attempt to find account
			AccountObject acc = AccountManager.getInstance().getAccount(serverDef.properties.get("owner").value);
			if (acc != null) {
				AccountDataContainer data = acc.getAccountData().getChildContainer("accountdata");
				if (data.entryExists("hostBanned") && data.getEntry("hostBanned").getAsBoolean()) {
					// Banned from hosting
					return response(401, "Unauthorized");
				}
			} else {
				// Attempt to find identity
				IdentityDef ownerDef = IdentityUtils.getIdentity(serverDef.properties.get("owner").value);
				if (ownerDef != null && ownerDef.properties.containsKey("hostBanned")
						&& ownerDef.properties.get("hostBanned").value.equals("true")) {
					// Banned from hosting
					return response(401, "Unauthorized");
				} else if (ownerDef == null) {
					// Owner was deleted
					IdentityUtils.deleteIdentity(serverDef.identity);
					return response(401, "Unauthorized");
				}
			}
		}

		// Load payload
		JsonObject payload;
		try {
			if (!func.getRequest().hasRequestBody())
				return response(400, "Bad Request");
			payload = JsonParser.parseString(func.getRequest().getRequestBodyAsString()).getAsJsonObject();
		} catch (Exception e) {
			return response(400, "Bad Request");
		}

		// Parse payload
		if (!payload.has("addresses") || !payload.get("addresses").isJsonArray()) {
			return response(400, "Bad Request");
		}

		// Verify address list
		for (JsonElement ele : payload.get("addresses").getAsJsonArray()) {
			if (!ele.isJsonPrimitive()) {
				return response(400, "Bad Request");
			}
		}

		// Build certificate
		CertificateDefinition cert = createCertificate(serverDef, payload.get("addresses").getAsJsonArray());

		// Preprare response
		JsonObject resp = new JsonObject();

		// Update token
		PhoenixToken tkn = new PhoenixToken();
		tkn.gameID = "nexusgrid";
		tkn.identity = serverDef.identity;
		tkn.lastUpdate = serverDef.lastUpdateTime;
		tkn.tokenExpiryTime = cert.expiry;
		tkn.tokenNotBefore = -1;
		tkn.tokenGenerationTime = System.currentTimeMillis() / 1000;
		tkn.capabilities = new String[] { "host", "idget" };

		// Set response
		resp.addProperty("identity", serverDef.identity);
		resp.add("certificate", JsonParser.parseString(serverDef.properties.get("serverCertificateProperties").value));
		resp.addProperty("token", tkn.toTokenString());
		return ok("application/json", new Gson().newBuilder().setPrettyPrinting().create().toJson(resp));
	}

	@Function(allowedMethods = { "POST" }, value = "reactivateserver", allowSubPaths = true)
	public FunctionResult reactivateServer(FunctionInfo func) throws NoSuchAlgorithmException, IOException {
		// Load token
		TokenUtils.AccessContext ctx = TokenUtils.fromFunction(func, "serverreactivate");
		if (ctx == null) {
			return response(401, "Unauthorized", "text/plain", "Server token invalid\n");
		}

		// Verify parameters
		if (func.getRequestedPath().split("/").length != 2) {
			// Invalid parameters
			return response(400, "Bad Request");
		}

		// Find server
		String sid = func.getRequestedPath().split("/")[1];
		IdentityDef serverDef = IdentityUtils.getIdentity(sid);

		// Check if the owner is not host-banned
		if (serverDef == null || !serverDef.properties.containsKey("owner")
				|| !serverDef.properties.containsKey("serverCertificateProperties"))
			return response(404, "Not Found");
		if (!serverDef.properties.get("owner").value.equals(new UUID(0, 0).toString())) {
			// Attempt to find account
			AccountObject acc = AccountManager.getInstance().getAccount(serverDef.properties.get("owner").value);
			if (acc != null) {
				AccountDataContainer data = acc.getAccountData().getChildContainer("accountdata");
				if (data.entryExists("hostBanned") && data.getEntry("hostBanned").getAsBoolean()) {
					// Banned from hosting
					return response(401, "Unauthorized");
				}
			} else {
				// Attempt to find identity
				IdentityDef ownerDef = IdentityUtils.getIdentity(serverDef.properties.get("owner").value);
				if (ownerDef != null && ownerDef.properties.containsKey("hostBanned")
						&& ownerDef.properties.get("hostBanned").value.equals("true")) {
					// Banned from hosting
					return response(401, "Unauthorized");
				} else if (ownerDef == null) {
					// Owner was deleted
					IdentityUtils.deleteIdentity(serverDef.identity);
					return response(404, "Not Found");
				}
			}
		}

		// Load payload
		JsonObject payload;
		try {
			if (!func.getRequest().hasRequestBody())
				return response(400, "Bad Request");
			payload = JsonParser.parseString(func.getRequest().getRequestBodyAsString()).getAsJsonObject();
		} catch (Exception e) {
			return response(400, "Bad Request");
		}

		// Parse payload
		if (!payload.has("addresses") || !payload.get("addresses").isJsonArray()) {
			return response(400, "Bad Request");
		}

		// Verify address list
		for (JsonElement ele : payload.get("addresses").getAsJsonArray()) {
			if (!ele.isJsonPrimitive()) {
				return response(400, "Bad Request");
			}
		}

		// Build certificate
		CertificateDefinition cert = createCertificate(serverDef, payload.get("addresses").getAsJsonArray());

		// Preprare response
		JsonObject resp = new JsonObject();

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
		resp.addProperty("identity", serverDef.identity);
		resp.add("certificate", JsonParser.parseString(serverDef.properties.get("serverCertificateProperties").value));
		resp.addProperty("token", tkn.toTokenString());
		return ok("application/json", new Gson().newBuilder().setPrettyPrinting().create().toJson(resp));
	}

	@SuppressWarnings("serial")
	@Function(allowedMethods = { "POST" }, value = "createserver")
	public FunctionResult createServerIdentity(FunctionInfo func) throws NoSuchAlgorithmException, IOException {
		// Load token
		TokenUtils.AccessContext ctx = TokenUtils.fromFunction(func, "makehost");
		if (ctx == null) {
			return response(401, "Unauthorized");
		}

		// Load payload
		JsonObject payload;
		try {
			if (!func.getRequest().hasRequestBody())
				return response(400, "Bad Request");
			payload = JsonParser.parseString(func.getRequest().getRequestBodyAsString()).getAsJsonObject();
		} catch (Exception e) {
			return response(400, "Bad Request");
		}

		// Verify owner
		if (!ctx.isAccount && ctx.identity.properties.containsKey("serverCertificateProperties"))
			return response(400, "Bad Request"); // Lets not allow servers to own servers, that will break hard

		// Parse payload
		if (!payload.has("addresses") || !payload.get("addresses").isJsonArray()) {
			return response(400, "Bad Request");
		}

		// Verify address list
		for (JsonElement ele : payload.get("addresses").getAsJsonArray()) {
			if (!ele.isJsonPrimitive()) {
				return response(400, "Bad Request");
			}
		}
		// Check if the user is not host-banned
		if (!ctx.identity.equals(new UUID(0, 0).toString())) {
			IdentityDef ownerDef = IdentityUtils.getIdentity(ctx.token.identity);
			if (!ownerDef.properties.containsKey("hostBanned")) {
				// Create field
				ownerDef.properties.put("hostBanned", new PropertyInfo() {
					{
						isReadonly = false;
						value = "false";
					}
				});
				IdentityUtils.updateIdentity(ownerDef);
			} else if (ownerDef == null || ownerDef.properties.get("hostBanned").value.equals("true")) {
				// Banned from hosting
				return response(401, "Unauthorized");
			}
		}

		// Build identity
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
						value = ctx.token.identity;
					}
				});
			}
		});

		// Build certificate
		CertificateDefinition cert = createCertificate(serverDef, payload.get("addresses").getAsJsonArray());

		// Preprare response
		JsonObject resp = new JsonObject();

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
		resp.addProperty("identity", serverDef.identity);
		resp.add("certificate", JsonParser.parseString(serverDef.properties.get("serverCertificateProperties").value));
		resp.addProperty("token", tkn.toTokenString());
		return ok("application/json", new Gson().newBuilder().setPrettyPrinting().create().toJson(resp));
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
