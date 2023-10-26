package org.asf.edge.common.http;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import org.asf.edge.common.util.TripleDesUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

/**
 * 
 * Edge service request information
 * 
 * @author Sky swimmer
 * 
 */
public class ServiceRequestInfo {
	public String apiKey;
	public String apiSecret;
	public byte[] desKey;

	public Map<String, String> payload;

	private static XmlMapper mapper = new XmlMapper();
	static {
		mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
		mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
	}

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
			return mapper.writerWithDefaultPrettyPrinter().withFeatures(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL)
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
