package org.asf.edge.common.webservices;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;
import org.asf.edge.common.IEdgeBaseServer;
import org.asf.edge.common.webservices.processors.AccountInventoryParamAnnoProcessor;
import org.asf.edge.common.webservices.processors.InventoryParamProcessor;
import org.asf.edge.common.webservices.processors.SaveInventoryParamAnnoProcessor;
import org.asf.edge.common.webservices.processors.SessionObjectsParamProcessor;
import org.asf.edge.common.webservices.processors.SodRequestInfoParamProcessor;
import org.asf.edge.common.webservices.processors.SodTokenSecuredAnnoProcessor;
import org.asf.nexus.webservices.AbstractWebService;
import org.asf.nexus.webservices.WebServiceContext;
import org.bouncycastle.util.encoders.Base32;

/**
 * 
 * Edge Web Service Abstract
 * 
 * @author Sky Swimmer
 *
 * @param <T> Server type
 * 
 */
public abstract class EdgeWebService<T extends IEdgeBaseServer> extends AbstractWebService<T> {

	/**
	 * Defines API encryption secrets by API key, default will use the windows key
	 * of version 3.31.0, keys MUST be upper-case
	 */
	public static HashMap<String, String> API_SECRET_MAP = new HashMap<String, String>();

	// Add secrets
	static {
		// WIN 3.31.0
		API_SECRET_MAP.put("B99F695C-7C6E-4E9B-B0F7-22034D799013", "56BB211B-CF06-48E1-9C1D-E40B5173D759");

		// OSX 3.31.0
		API_SECRET_MAP.put("44C9E925-2D98-41E8-BF62-F8E90B4A5EE1", "AE38AF03-65C1-4952-A59A-8103A8D1BA2A");

		// XBOX 3.31.0
		API_SECRET_MAP.put("E323F1BA-FDFF-458A-8F36-8A2D5AE8A9F2", "E1B45D11-19EE-4981-B32E-BC33B2459696");

		// WSA 3.31.0
		API_SECRET_MAP.put("7DA5F99D-997D-429C-B4B0-F95574A250E7", "2C0CF98F-9B36-40D1-B059-98644A30848D");

		// WIN 3.31.0 (Steam)
		API_SECRET_MAP.put("FE543944-3F16-4E02-986C-C68C43C16F07", "F5D573D9-CDB3-4142-9462-F2A5DEF0B7E8");

		// OSX 3.31.0 (Steam)
		API_SECRET_MAP.put("A4BEFEA1-DC93-4967-A9E7-5E34F74D12FD", "702189B9-B4EA-4B4D-9D9B-84D54445DEF1");

		// IOS 3.31.0
		API_SECRET_MAP.put("1E7CCC3A-4ADB-4736-B843-7B3DA5140A43", "8ADB1B50-91F7-4829-BEEF-82152C6DB752");

		// FB 3.31.0
		API_SECRET_MAP.put("4CDC7F80-C2D1-4053-9888-8B65B1A0B504", "5469D6A0-098D-424A-BDE3-B096149B1823");

		// Android 3.31.0 (google)
		API_SECRET_MAP.put("515AF675-BEC7-4C42-BA64-7BFAF198D8EA", "7D175215-56D3-4495-BC3E-F55EA92B06A5");

		// Android 3.31.0 (huawei)
		API_SECRET_MAP.put("654A7A73-3BC2-4DD3-8DD9-FA79A1DE9835", "AF44CDF2-CEC5-4B40-940C-A9ECA7AD7587");

		// Android 3.31.0 (amazon)
		API_SECRET_MAP.put("37C8DE45-EE55-4E41-8264-6F5B435D75F7", "A8EB209C-CD3E-4737-BA69-3DDCE3A87263");
	}

	private Utilities utils;

	public EdgeWebService(WebServiceContext<T> context) {
		super(context);
	}

	/**
	 * Creates a new instance of this webservice
	 * 
	 * @param context Webservice context
	 */
	public abstract EdgeWebService<T> createNewInstance(WebServiceContext<T> context);

	/**
	 * Retrieves the utilities container for this api handler (specifically for
	 * JumpStart client compatibility)
	 * 
	 * @return Utilities instance
	 */
	protected Utilities getUtilities() {
		if (utils == null)
			utils = new Utilities(getRequest(), getResponse());
		return utils;
	}

	public static class Utilities {
		private HttpResponse response;
		private HttpRequest request;

		public Utilities(HttpRequest request, HttpResponse response) {
			this.request = request;
			this.response = response;
		}

		/**
		 * Tool to decode URL-encoded forms
		 * 
		 * @param payload Form payload
		 * @return Map containing the form data
		 */
		public Map<String, String> parseForm(String payload) {
			HashMap<String, String> frm = new HashMap<String, String>();
			String key = "";
			String value = "";
			boolean isKey = true;
			for (int i = 0; i < payload.length(); i++) {
				char ch = payload.charAt(i);
				if (ch == '&') {
					if (isKey && !key.isEmpty()) {
						frm.put(key, "");
						key = "";
					} else if (!isKey && !key.isEmpty()) {
						try {
							frm.put(key, URLDecoder.decode(value, "UTF-8"));
						} catch (Exception e) {
							frm.put(key, value);
						}
						isKey = true;
						key = "";
						value = "";
					}
				} else if (ch == '=') {
					isKey = !isKey;
				} else {
					if (isKey) {
						key += ch;
					} else {
						value += ch;
					}
				}
			}
			if (!key.isEmpty() || !value.isEmpty()) {
				try {
					frm.put(key, URLDecoder.decode(value, "UTF-8"));
				} catch (Exception e) {
					frm.put(key, value);
				}
			}
			return frm;
		}

		/**
		 * Retrieves the API key from a request post body, sets HTTP 400 if the content
		 * is invalid
		 * 
		 * @return API key or null if invalid
		 * @throws IOException If parsing fails
		 */
		public String getApiKeyFromRequest() throws IOException {
			if (!request.hasRequestBody() || !request.hasHeader("Content-Type")
					|| !request.getHeaderValue("Content-Type").equals("application/x-www-form-urlencoded")) {
				response.setResponseStatus(400, "Bad request");
				return null;
			}
			String payload = request.getRequestBodyAsString();
			Map<String, String> requestData = parseForm(payload);
			if (!requestData.containsKey("apiKey")) {
				response.setResponseStatus(400, "Bad request");
				return null;
			}
			return requestData.get("apiKey");
		}

		/**
		 * Tool to retrieve secrets by API key
		 * 
		 * @param apiKey API key to use
		 * @param logger Logger to use in case of warnings
		 * @return API secret string
		 */
		public String getSecretFromKey(String apiKey, Logger logger) {
			String secret = "56BB211B-CF06-48E1-9C1D-E40B5173D759";
			if (EdgeWebService.API_SECRET_MAP.containsKey(apiKey.toUpperCase()))
				secret = EdgeWebService.API_SECRET_MAP.get(apiKey.toUpperCase());
			else {
				// Warn
				logger.warn("No secret for API key " + apiKey + ", using default!");
			}
			return secret;
		}

		/**
		 * Encodes secrets in MD5 for SoD's TripleDES interface
		 * 
		 * @param secret   Secret to use
		 * @param encoding Encoding to use (eg. "UTF-16LE")
		 * @return Key bytes
		 */
		public byte[] encodeMD5Key(String secret, String encoding) {
			byte[] key;
			try {
				MessageDigest digest = MessageDigest.getInstance("MD5");
				key = digest.digest(secret.getBytes(encoding));
			} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			return key;
		}

		/**
		 * Decodes a token string from base32 to preserve casing
		 * 
		 * @param token Token to decode
		 * @return Decoded token
		 */
		public String decodeToken(String token) throws IOException {
			return new String(Base32.decode(token), "UTF-8");
		}

		/**
		 * Encodes a token string to base32 to preserve casing
		 * 
		 * @param token Token to encode
		 * @return Encoded token
		 */
		public String encodeToken(String token) throws IOException {
			return Base32.toBase32String(token.getBytes("UTF-8"));
		}
	}

	static {
		// Register processors

		AbstractWebService.registerParameterProcessor(new SodRequestInfoParamProcessor());
		AbstractWebService.registerParameterProcessor(new SessionObjectsParamProcessor());
		AbstractWebService.registerParameterProcessor(new InventoryParamProcessor());

		AbstractWebService.registerAnnotationProcessor(new SodTokenSecuredAnnoProcessor());
		AbstractWebService.registerAnnotationProcessor(new AccountInventoryParamAnnoProcessor());
		AbstractWebService.registerAnnotationProcessor(new SaveInventoryParamAnnoProcessor());
	}
}
