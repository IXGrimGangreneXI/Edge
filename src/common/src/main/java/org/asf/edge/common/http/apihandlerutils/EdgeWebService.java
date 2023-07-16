package org.asf.edge.common.http.apihandlerutils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.IBaseServer;
import org.asf.edge.common.http.apihandlerutils.functions.LegacyFunction;
import org.asf.edge.common.http.apihandlerutils.functions.LegacyFunctionInfo;
import org.asf.edge.common.http.apihandlerutils.functions.Function;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionInfo;
import org.asf.edge.common.http.apihandlerutils.functions.FunctionResult;
import org.asf.edge.common.http.cookies.CookieContext;
import org.asf.edge.common.http.cookies.CookieManager;
import org.asf.edge.common.util.TripleDesUtil;
import org.bouncycastle.util.encoders.Base32;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

/**
 * 
 * Edge Web Service Abstract
 * 
 * @author Sky Swimmer
 *
 * @param <T> Server type
 * 
 */
@SuppressWarnings("deprecation")
public abstract class EdgeWebService<T extends IBaseServer> extends HttpPushProcessor {
	private static XmlMapper mapper = new XmlMapper();
	static {
		mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
	}

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

	private Utilities utils = new Utilities();
	private HashMap<String, Method> functions = new HashMap<String, Method>();
	private HashMap<String, Method> legacyFunctions = new HashMap<String, Method>();
	private CookieContext cookies;
	private T server;

	public EdgeWebService(T server) {
		this.server = server;
		initialize();
	}

	private void initialize() {
		// Find all functions
		for (Method meth : getClass().getMethods()) {
			if (!Modifier.isAbstract(meth.getModifiers()) && !Modifier.isStatic(meth.getModifiers())) {
				// Check parameters
				if (meth.getParameterCount() == 1
						&& LegacyFunctionInfo.class.isAssignableFrom(meth.getParameterTypes()[0])) {
					// Check annotation
					if (meth.isAnnotationPresent(LegacyFunction.class)) {
						LegacyFunction funcAnno = meth.getAnnotation(LegacyFunction.class);
						String name = meth.getName();
						if (!funcAnno.value().equals("<auto>"))
							name = funcAnno.value();

						// Make accessible
						meth.setAccessible(true);

						// Register
						legacyFunctions.put(name.toLowerCase(), meth);
					}
				} else if (meth.getParameterCount() == 1
						&& FunctionInfo.class.isAssignableFrom(meth.getParameterTypes()[0])
						&& FunctionResult.class.isAssignableFrom(meth.getReturnType())) {
					// Check annotation
					if (meth.isAnnotationPresent(Function.class)) {
						Function funcAnno = meth.getAnnotation(Function.class);
						String name = meth.getName();
						if (!funcAnno.value().equals("<auto>"))
							name = funcAnno.value();

						// Make accessible
						meth.setAccessible(true);

						// Register
						functions.put(name.toLowerCase(), meth);
					}
				}
			}
		}
	}

	/**
	 * Retrieves the server instance
	 */
	protected T getServerInstance() {
		return server;
	}

	/**
	 * Retrieves the cookie context of this request
	 * 
	 * @return CookieContext instance
	 */
	protected CookieContext getCookies() {
		if (cookies == null)
			cookies = CookieManager.getCookies(getRequest(), getResponse());
		return cookies;
	}

	@Override
	public void process(String path, String method, RemoteClient client, String contentType) throws IOException {
		// Compute subpath
		path = sanitizePath(path.substring(this.path().length()));
		if (path.startsWith("/"))
			path = path.substring(1);
		if (path.isEmpty()) {
			setResponseStatus(404, "Not found");
			return;
		}

		// Make sure its not attempting to access a resource outside of the scope
		if (path.startsWith("..") || path.endsWith("..") || path.contains("/..") || path.contains("../")) {
			setResponseStatus(403, "Forbidden");
			return;
		}

		// Find function
		if (functions.containsKey(path.toLowerCase())) {
			// Get function
			Method mth = functions.get(path.toLowerCase());
			Function anno = mth.getAnnotation(Function.class);

			// Check method
			boolean allowed = false;
			for (String meth : anno.allowedMethods()) {
				if (meth.equalsIgnoreCase(method)) {
					allowed = true;
					break;
				}
			}
			if (!allowed) {
				setResponseStatus(405, "Method not allowed");
				return;
			}

			// Run function
			try {
				FunctionResult res = (FunctionResult) mth.invoke(this, new FunctionInfo(path, getRequest(),
						getResponse(), getServer(), method, client, contentType, getCookies()));

				// Set response
				setResponseStatus(res.getStatusCode(), res.getStatusMessage());
				if (res.hasResponseBody()) {
					// Check response modes
					if (res.getContentLength() != -1) {
						// With length
						if (res.getResponseMediaType() != null)
							setResponseContent(res.getResponseMediaType(), res.getResponseBodyStream(),
									res.getContentLength());
						else
							setResponseContent(res.getResponseBodyStream(), res.getContentLength());
					} else {
						// Without length
						if (res.getResponseMediaType() != null)
							setResponseContent(res.getResponseMediaType(), res.getResponseBodyStream());
						else
							setResponseContent(res.getResponseBodyStream());
					}
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
			return;
		}
		if (legacyFunctions.containsKey(path.toLowerCase())) {
			// Get function
			Method mth = legacyFunctions.get(path.toLowerCase());
			LegacyFunction anno = mth.getAnnotation(LegacyFunction.class);

			// Check method
			boolean allowed = false;
			for (String meth : anno.allowedMethods()) {
				if (meth.equalsIgnoreCase(method)) {
					allowed = true;
					break;
				}
			}
			if (!allowed) {
				setResponseStatus(405, "Method not allowed");
				return;
			}

			// Run function
			try {
				mth.invoke(this, new LegacyFunctionInfo(path, getRequest(), getResponse(), getServer(), method, client,
						contentType, getCookies()));
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
			return;
		}

		// Run processor
		setResponseStatus(404, "Not found");
		fallbackRequestProcessor(path, method, client, contentType);
	}

	/**
	 * Called to process the request, called if no function applies
	 * 
	 * @param path        Path string
	 * @param method      Request method
	 * @param client      Remote client
	 * @param contentType Body content type
	 * @throws IOException If processing fails
	 */
	protected void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType)
			throws IOException {
	}

	/**
	 * Cleans a given path
	 * 
	 * @param path Path to clean
	 * @return Cleaned path
	 */
	protected String sanitizePath(String path) {
		while (path.startsWith("/"))
			path = path.substring(1);
		while (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		while (path.contains("//"))
			path = path.replace("//", "/");
		if (path.contains("\\"))
			path = path.replace("\\", "/");
		if (!path.startsWith("/"))
			path = "/" + path;
		return path;
	}

	@Override
	public boolean supportsNonPush() {
		return true;
	}

	@Override
	public boolean supportsChildPaths() {
		return true;
	}

	/**
	 * Retrieves the utilities container for this api handler (specifically for
	 * JumpStart client compatibility)
	 * 
	 * @return Utilities instance
	 */
	protected Utilities getUtilities() {
		return utils;
	}

	protected class ServiceRequestInfo {
		public String apiKey;
		public String apiSecret;
		public byte[] desKey;

		public Map<String, String> payload;

		/**
		 * Decodes an XML value
		 * 
		 * @param <T2>  Value type
		 * @param value Value to decode
		 * @param cls   Object class
		 * @return Object instance
		 * @throws IOException If parsing fails
		 */
		public <T2 extends Object> T2 parseXmlValue(String value, Class<T2> cls) throws IOException {
			try {
				return mapper.readValue(value, cls);
			} catch (JsonProcessingException e) {
				throw new IOException("Invalid data", e);
			}
		}

		/**
		 * Encodes an object to XML
		 * 
		 * @param rootElementName Root element name
		 * @param data            Object to encode
		 * @return XML string
		 * @throws IOException If encoding fails
		 */
		public String generateXmlValue(String rootElementName, Object data) throws IOException {
			try {
				return mapper.writer().withFeatures(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL)
						.withRootName(rootElementName).writeValueAsString(data);
			} catch (JsonProcessingException e) {
				throw new IOException("Encoding failure", e);
			}
		}

		/**
		 * Retrieves encrypted value
		 * 
		 * @param key Request payload key to retrieve the value of
		 * @return Decrypted value string
		 * @throws IOException If decrypting fails
		 */
		public String getEncryptedValue(String key) throws IOException {
			String encrypted = payload.get(key);
			return decryptString(encrypted);
		}

		/**
		 * Decrypts a string
		 * 
		 * @param data String to decrypt
		 * @return Decrypted value string
		 * @throws IOException If decrypting fails
		 */
		public String decryptString(String data) throws IOException {
			byte[] enc = Base64.getDecoder().decode(data);

			// Decrypt
			byte[] dec = TripleDesUtil.decrypt(enc, desKey);

			// Encode back to a string
			String val = new String(dec, "UTF-16LE");
			return val;
		}

		/**
		 * Encrypts data to the SoD-readable encrypted service response format
		 * 
		 * @param data Data to encrypt
		 * @return Encrypted string
		 * @throws IOException If encrypting fails
		 */
		public String generateEncryptedResponse(String data) throws IOException {
			return generateXmlValue("string", generateEncryptedString(data));
		}

		/**
		 * Generates an encrypted value string
		 * 
		 * @param value String to encrypt
		 * @return Encrypted string
		 * @throws IOException If encrypting fails
		 */
		public String generateEncryptedString(String value) throws IOException {
			// Encrypt
			byte[] enc = TripleDesUtil.encrypt(value.getBytes("UTF_16LE"), desKey);

			// Encode to base64
			return Base64.getEncoder().encodeToString(enc);
		}
	}

	protected class Utilities {

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
			if (!getRequest().hasRequestBody() || !getRequest().hasHeader("Content-Type")
					|| !getRequest().getHeaderValue("Content-Type").equals("application/x-www-form-urlencoded")) {
				getResponse().setResponseStatus(400, "Bad request");
				return null;
			}
			String payload = getRequest().getRequestBodyAsString();
			Map<String, String> requestData = parseForm(payload);
			if (!requestData.containsKey("apiKey")) {
				getResponse().setResponseStatus(400, "Bad request");
				return null;
			}
			return requestData.get("apiKey");
		}

		/**
		 * Retrieves service request information
		 * 
		 * @param logger Logger to use in case of warnings
		 * @return ServiceRequestInfo instance or null if invalid
		 * @throws IOException If parsing fails
		 */
		public ServiceRequestInfo getServiceRequestPayload(Logger logger) throws IOException {
			// Check request
			if (!getRequest().hasRequestBody() || !getRequest().hasHeader("Content-Type")
					|| !getRequest().getHeaderValue("Content-Type").equals("application/x-www-form-urlencoded")) {
				getResponse().setResponseStatus(400, "Bad request");
				return null;
			}

			// Parse payload
			String payload = getRequest().getRequestBodyAsString();
			Map<String, String> requestData = parseForm(payload);
			if (!requestData.containsKey("apiKey")) {
				getResponse().setResponseStatus(400, "Bad request");
				return null;
			}

			// Compute key
			String apiKey = requestData.get("apiKey");
			String secret = getUtilities().getSecretFromKey(apiKey, logger);
			byte[] key = getUtilities().encodeMD5Key(secret, "UTF-16LE");

			// Create request info
			ServiceRequestInfo info = new ServiceRequestInfo();
			info.apiKey = apiKey;
			info.apiSecret = secret;
			info.desKey = key;
			info.payload = requestData;

			// Return payload
			return info;
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
			if (API_SECRET_MAP.containsKey(apiKey.toUpperCase()))
				secret = API_SECRET_MAP.get(apiKey.toUpperCase());
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

	/**
	 * Creates a function result object with no response body (errors will use
	 * default error page)
	 */
	public FunctionResult ok() {
		return new FunctionResult(200, "OK");
	}

	/**
	 * Creates a function result object
	 * 
	 * @param mediaType    Response media type
	 * @param responseBody Response body
	 */
	public FunctionResult ok(String mediaType, InputStream responseBody) {
		return new FunctionResult(200, "OK", mediaType, responseBody);
	}

	/**
	 * Creates a function result object
	 * 
	 * @param responseBody Response body
	 */
	public FunctionResult ok(InputStream responseBody) {
		return new FunctionResult(200, "OK", responseBody);
	}

	/**
	 * Creates a function result object
	 * 
	 * @param mediaType     Response media type
	 * @param contentLength Response body length
	 * @param responseBody  Response body
	 */
	public FunctionResult ok(String mediaType, long contentLength, InputStream responseBody) {
		return new FunctionResult(200, "OK", mediaType, contentLength, responseBody);
	}

	/**
	 * Creates a function result object
	 * 
	 * @param contentLength Response body length
	 * @param responseBody  Response body
	 */
	public FunctionResult ok(long contentLength, InputStream responseBody) {
		return new FunctionResult(200, "OK", contentLength, responseBody);
	}

	/**
	 * Creates a function result object
	 * 
	 * @param mediaType    Response media type
	 * @param responseBody Response body
	 */
	public FunctionResult ok(String mediaType, byte[] responseBody) {
		return new FunctionResult(200, "OK", mediaType, responseBody);
	}

	/**
	 * Creates a function result object
	 * 
	 * @param responseBody Response body
	 */
	public FunctionResult ok(byte[] responseBody) {
		return new FunctionResult(200, "OK", responseBody);
	}

	/**
	 * Creates a function result object
	 * 
	 * @param mediaType    Response media type
	 * @param responseBody Response body
	 */
	public FunctionResult ok(String mediaType, String responseBody) {
		return new FunctionResult(200, "OK", mediaType, responseBody);

	}

	/**
	 * Creates a function result object
	 * 
	 * @param responseBody Response body
	 */
	public FunctionResult ok(String responseBody) {
		return new FunctionResult(200, "OK", responseBody);
	}

	/**
	 * Creates a function result object with no response body (errors will use
	 * default error page)
	 * 
	 * @param statusCode    Result status code
	 * @param statusMessage Result status message
	 */
	public FunctionResult response(int statusCode, String statusMessage) {
		return new FunctionResult(statusCode, statusMessage);
	}

	/**
	 * Creates a function result object
	 * 
	 * @param statusCode    Result status code
	 * @param statusMessage Result status message
	 * @param mediaType     Response media type
	 * @param responseBody  Response body
	 */
	public FunctionResult response(int statusCode, String statusMessage, String mediaType, InputStream responseBody) {
		return new FunctionResult(statusCode, statusMessage, mediaType, responseBody);
	}

	/**
	 * Creates a function result object
	 * 
	 * @param statusCode    Result status code
	 * @param statusMessage Result status message
	 * @param responseBody  Response body
	 */
	public FunctionResult response(int statusCode, String statusMessage, InputStream responseBody) {
		return new FunctionResult(statusCode, statusMessage, responseBody);
	}

	/**
	 * Creates a function result object
	 * 
	 * @param statusCode    Result status code
	 * @param statusMessage Result status message
	 * @param mediaType     Response media type
	 * @param contentLength Response body length
	 * @param responseBody  Response body
	 */
	public FunctionResult response(int statusCode, String statusMessage, String mediaType, long contentLength,
			InputStream responseBody) {
		return new FunctionResult(statusCode, statusMessage, mediaType, contentLength, responseBody);
	}

	/**
	 * Creates a function result object
	 * 
	 * @param statusCode    Result status code
	 * @param statusMessage Result status message
	 * @param contentLength Response body length
	 * @param responseBody  Response body
	 */
	public FunctionResult response(int statusCode, String statusMessage, long contentLength, InputStream responseBody) {
		return new FunctionResult(statusCode, statusMessage, contentLength, responseBody);
	}

	/**
	 * Creates a function result object
	 * 
	 * @param statusCode    Result status code
	 * @param statusMessage Result status message
	 * @param mediaType     Response media type
	 * @param responseBody  Response body
	 */
	public FunctionResult response(int statusCode, String statusMessage, String mediaType, byte[] responseBody) {
		return new FunctionResult(statusCode, statusMessage, mediaType, responseBody);
	}

	/**
	 * Creates a function result object
	 * 
	 * @param statusCode    Result status code
	 * @param statusMessage Result status message
	 * @param responseBody  Response body
	 */
	public FunctionResult response(int statusCode, String statusMessage, byte[] responseBody) {
		return new FunctionResult(statusCode, statusMessage, responseBody);
	}

	/**
	 * Creates a function result object
	 * 
	 * @param statusCode    Result status code
	 * @param statusMessage Result status message
	 * @param mediaType     Response media type
	 * @param responseBody  Response body
	 */
	public FunctionResult response(int statusCode, String statusMessage, String mediaType, String responseBody) {
		return new FunctionResult(statusCode, statusMessage, mediaType, responseBody);

	}

	/**
	 * Creates a function result object
	 * 
	 * @param statusCode    Result status code
	 * @param statusMessage Result status message
	 * @param responseBody  Response body
	 */
	public FunctionResult response(int statusCode, String statusMessage, String responseBody) {
		return new FunctionResult(statusCode, statusMessage, responseBody);
	}

}
