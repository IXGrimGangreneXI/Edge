package org.asf.edge.modules.gridapi.utils;

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

import org.asf.edge.common.services.accounts.AccountManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 
 * Phoenix Token Utility
 * 
 * @author Sky Swimmer
 *
 */
public class PhoenixToken {
	private AccountManager accMgr = AccountManager.getInstance();

	/**
	 * Phoenix game ID
	 */
	public static final String GAME_ID = "nexusgrid";

	/**
	 * Game ID
	 */
	public String gameID;

	/**
	 * Account ID for the token
	 */
	public String identity;

	/**
	 * Identity last update time
	 */
	public long lastUpdate = -1;

	/**
	 * Token not-before
	 */
	public long tokenNotBefore = -1;

	/**
	 * Token generation time
	 */
	public long tokenGenerationTime = -1;

	/**
	 * Token expiry time
	 */
	public long tokenExpiryTime = -1;

	/**
	 * Capability list
	 */
	public String[] capabilities = new String[0];

	/**
	 * Payload field, optional
	 */
	public JsonElement payload;

	/**
	 * Checks if the token has a specific capability
	 * 
	 * @param cap Capability to check
	 * @return True if present, false otherwise
	 */
	public boolean hasCapability(String cap) {
		return Stream.of(capabilities).anyMatch(t -> t.equalsIgnoreCase(cap))
				|| Stream.of(capabilities).anyMatch(t -> t.equalsIgnoreCase("master"));
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
			org.asf.edge.common.tokens.TokenParseResult res = accMgr.verifyToken(token);
			if (res != org.asf.edge.common.tokens.TokenParseResult.SUCCESS)
				return TokenParseResult.INVALID_DATA;

			// Parse payload
			JsonObject jwtPl = JsonParser
					.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
					.getAsJsonObject();
			if (!jwtPl.has("sub") || !jwtPl.has("cgi") || !jwtPl.has("iat") || !jwtPl.has("lu") || !jwtPl.has("iss")
					|| !jwtPl.has("cl") || !jwtPl.get("iss").getAsString().equals("NEXUSGRID"))
				return TokenParseResult.INVALID_DATA;

			// Read payload
			String id = jwtPl.get("sub").getAsString();
			String commonGameID = jwtPl.get("cgi").getAsString();
			long lu = jwtPl.get("lu").getAsLong();
			long expiry = jwtPl.has("exp") ? jwtPl.get("exp").getAsLong() * 1000 : -1;
			long notBefore = jwtPl.has("nbf") ? jwtPl.get("nbf").getAsLong() * 1000 : -1;
			long issuedAt = jwtPl.get("iat").getAsLong() * 1000;
			JsonArray capLs = jwtPl.get("cl").getAsJsonArray();

			// Verify expiry
			if (expiry != -1 && System.currentTimeMillis() > expiry)
				return TokenParseResult.TOKEN_EXPIRED;

			// Verify not-before
			if (notBefore != -1 && System.currentTimeMillis() < notBefore)
				return TokenParseResult.TOKEN_NOT_YET_VALID;

			// Verify game ID
			if (!commonGameID.equals(GAME_ID))
				return TokenParseResult.INVALID_DATA;

			// Parse into token object
			identity = id;
			gameID = commonGameID;
			lastUpdate = lu;
			tokenExpiryTime = expiry;
			tokenGenerationTime = issuedAt;
			tokenNotBefore = notBefore;
			this.capabilities = new String[capLs.size()];
			payload = jwtPl.get("pl");
			int i = 0;
			for (JsonElement cap : capLs)
				this.capabilities[i++] = cap.getAsString();

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

		// Populate
		tokenGenerationTime = System.currentTimeMillis() / 1000;

		// Build payload
		JsonObject payload = new JsonObject();
		payload.addProperty("cgi", gameID);
		payload.addProperty("sub", identity);
		payload.addProperty("iss", "NEXUSGRID");
		if (tokenNotBefore != -1)
			payload.addProperty("nbf", tokenNotBefore);
		payload.addProperty("iat", tokenGenerationTime);
		if (tokenExpiryTime != -1)
			payload.addProperty("exp", tokenExpiryTime);
		payload.addProperty("jti", UUID.randomUUID().toString());
		payload.addProperty("lu", lastUpdate);
		if (this.payload != null)
			payload.add("pl", this.payload);

		// Add capabilities
		JsonArray caps = new JsonArray();
		for (String cap : capabilities)
			caps.add(cap);
		payload.add("cl", caps);

		// Build
		String payloadD = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toString().getBytes("UTF-8"));

		// Sign
		String token = headerD + "." + payloadD;
		String sig = Base64.getUrlEncoder().withoutPadding().encodeToString(accMgr.signToken(token));
		return token + "." + sig;
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
