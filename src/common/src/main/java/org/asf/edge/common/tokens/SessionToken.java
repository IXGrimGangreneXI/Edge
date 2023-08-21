package org.asf.edge.common.tokens;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Stream;

import org.asf.edge.common.services.accounts.AccountDataContainer;
import org.asf.edge.common.services.accounts.AccountManager;
import org.asf.edge.common.services.accounts.AccountObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * 
 * EDGE Session Token Utility
 * 
 * @author Sky Swimmer
 *
 */
public class SessionToken {
	private AccountManager accMgr = AccountManager.getInstance();

	/**
	 * Account ID for the token
	 */
	public String accountID;

	/**
	 * Holds the account object found during parsing
	 */
	public AccountObject account;

	/**
	 * Save ID for the token
	 */
	public String saveID;

	/**
	 * Last login timestamp of the account
	 */
	public long lastLoginTime;

	/**
	 * Capability list
	 */
	public String[] capabilities = new String[0];

	/**
	 * Checks if the token has a specific capability
	 * 
	 * @param cap Capability to check
	 * @return True if present, false otherwise
	 */
	public boolean hasCapability(String cap) {
		return Stream.of(capabilities).anyMatch(t -> t.equals(cap));
	}

	/**
	 * Parses tokens
	 * 
	 * @param token Token to parse
	 * @return TokenParseResult value
	 */
	public TokenParseResult parseToken(String token) {
		try {
			// Parse header
			JsonObject jwtHead = JsonParser
					.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]), "UTF-8"))
					.getAsJsonObject();
			if (!jwtHead.get("typ").getAsString().equalsIgnoreCase("jwt"))
				return TokenParseResult.INVALID_DATA;
			if (!jwtHead.get("alg").getAsString().equalsIgnoreCase("rs256"))
				return TokenParseResult.INVALID_DATA;

			// Verify
			TokenParseResult res = accMgr.verifyToken(token);
			if (res != TokenParseResult.SUCCESS)
				return res;

			// Parse payload
			JsonObject jwtPl = JsonParser
					.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
					.getAsJsonObject();
			if (!jwtPl.has("sub") || !jwtPl.has("uuid") || !jwtPl.has("iat") || !jwtPl.has("jti") || !jwtPl.has("iss")
					|| !jwtPl.has("cl") || !jwtPl.has("llt") || !jwtPl.get("iss").getAsString().equals("EDGE"))
				return TokenParseResult.INVALID_DATA;

			// Read payload
			String accID = jwtPl.get("uuid").getAsString();
			long lastLoginTime = jwtPl.get("llt").getAsLong();
			String saveID = null;
			if (jwtPl.has("save"))
				saveID = jwtPl.get("save").getAsString();
			JsonArray capLs = jwtPl.get("cl").getAsJsonArray();

			// Verify account
			AccountObject acc = accMgr.getAccount(accID);
			if (acc == null)
				return TokenParseResult.INVALID_DATA;

			// Check time
			if (acc.getLastLoginTime() != lastLoginTime && saveID != null)
				return TokenParseResult.LOGGED_IN_ELSEWHERE;

			// Verify expiry
			AccountDataContainer accData = acc.getAccountData().getChildContainer("accountdata");
			JsonElement tknTime = accData.getEntry("sessiontime");
			if (tknTime == null)
				return TokenParseResult.TOKEN_EXPIRED;
			if (tknTime.getAsLong() + (15 * 60 * 1000) <= System.currentTimeMillis()) {
				// Token has expired as its too long ago that it was refreshed
				return TokenParseResult.TOKEN_EXPIRED;
			}

			// Parse into token object
			account = acc;
			accountID = accID;
			this.saveID = saveID;
			this.lastLoginTime = lastLoginTime;
			this.capabilities = new String[capLs.size()];
			int i = 0;
			for (JsonElement cap : capLs)
				this.capabilities[i++] = cap.getAsString();

			// Ping
			acc.ping(false);

			// If needed update session time
			if ((System.currentTimeMillis() - tknTime.getAsLong()) > 60000) {
				// One minute since the last refresh, make sure this token doesnt expire
				updateSessionTime();
			}

			// Success!
			return TokenParseResult.SUCCESS;
		} catch (IOException e) {
			return TokenParseResult.INVALID_DATA;
		}
	}

	/**
	 * Converts the token to a string
	 * 
	 * @return Token string
	 */
	public String toTokenString() throws IOException {
		// Build header
		JsonObject headers = new JsonObject();
		headers.addProperty("alg", "RS256");
		headers.addProperty("typ", "JWT");
		String headerD = Base64.getUrlEncoder().withoutPadding().encodeToString(headers.toString().getBytes("UTF-8"));

		// Build payload
		JsonObject payload = new JsonObject();
		payload.addProperty("iat", System.currentTimeMillis() / 1000);
		payload.addProperty("jti", UUID.randomUUID().toString());
		payload.addProperty("iss", "EDGE");
		payload.addProperty("sub", "EDGE");
		payload.addProperty("uuid", accountID);
		if (saveID != null)
			payload.addProperty("save", saveID);
		payload.addProperty("llt", lastLoginTime);

		// Add capabilities
		JsonArray caps = new JsonArray();
		for (String cap : capabilities)
			caps.add(cap);
		payload.add("cl", caps);

		// Update account
		updateSessionTime();

		// Build
		String payloadD = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toString().getBytes("UTF-8"));

		// Sign
		String token = headerD + "." + payloadD;
		String sig = Base64.getUrlEncoder().withoutPadding().encodeToString(accMgr.signToken(token));
		return token + "." + sig;
	}

	/**
	 * Updates the account session time
	 * 
	 * @throws IOException If updating fails
	 */
	public void updateSessionTime() throws IOException {
		AccountObject acc = accMgr.getAccount(accountID);
		if (acc == null)
			throw new IOException("Account not found");
		acc.getAccountData().getChildContainer("accountdata").setEntry("sessiontime",
				new JsonPrimitive(System.currentTimeMillis()));
	}

	// PEM parser
	public static byte[] pemDecode(String pem) {
		String base64 = pem.replace("\r", "");

		// Strip header
		while (base64.startsWith("-"))
			base64 = base64.substring(1);
		while (!base64.startsWith("-"))
			base64 = base64.substring(1);
		while (base64.startsWith("-"))
			base64 = base64.substring(1);

		// Clean data
		base64 = base64.replace("\n", "");

		// Strip footer
		while (base64.endsWith("-"))
			base64 = base64.substring(0, base64.length() - 1);
		while (!base64.endsWith("-"))
			base64 = base64.substring(0, base64.length() - 1);
		while (base64.endsWith("-"))
			base64 = base64.substring(0, base64.length() - 1);

		// Decode and return
		return Base64.getDecoder().decode(base64);
	}

	// PEM emitter
	public static String pemEncode(byte[] key, String type) {
		// Generate header
		String PEM = "-----BEGIN " + type + " KEY-----";

		// Generate payload
		String base64 = new String(Base64.getEncoder().encode(key));

		// Generate PEM
		while (true) {
			PEM += "\n";
			boolean done = false;
			for (int i = 0; i < 64; i++) {
				if (base64.isEmpty()) {
					done = true;
					break;
				}
				PEM += base64.substring(0, 1);
				base64 = base64.substring(1);
			}
			if (base64.isEmpty())
				break;
			if (done)
				break;
		}

		// Append footer
		PEM += "\n";
		PEM += "-----END " + type + " KEY-----";

		// Return PEM data
		return PEM;
	}

	// Signature generator
	public static byte[] sign(byte[] data, PrivateKey privateKey) {
		try {
			Signature sig = Signature.getInstance("Sha256WithRSA");
			sig.initSign(privateKey);
			sig.update(data);
			return sig.sign();
		} catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException e) {
			throw new RuntimeException(e);
		}
	}

	// Signature verification
	public static boolean verify(byte[] data, byte[] signature, PublicKey publicKey) {
		try {
			Signature sig = Signature.getInstance("Sha256WithRSA");
			sig.initVerify(publicKey);
			sig.update(data);
			return sig.verify(signature);
		} catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException e) {
			return false;
		}
	}

}
