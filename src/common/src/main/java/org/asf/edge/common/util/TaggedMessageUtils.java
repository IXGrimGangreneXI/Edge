package org.asf.edge.common.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * Utility for parsing and writing tagged messages (for WsMessage)
 * 
 * @author Sky Swimmer
 *
 */
public class TaggedMessageUtils {

	/**
	 * Writes tagged messages
	 * 
	 * @param payload Payload map
	 * @return Tagged string
	 */
	public static String writeTagged(Map<String, String> payload) {
		String msg = "";
		for (String key : payload.keySet()) {
			String value = payload.get(key);
			if (!msg.isEmpty())
				msg += "&&";
			msg += "[[" + key + "]]=[[" + value + "]]";
		}
		return msg;
	}

	/**
	 * Parses tagged messages
	 * 
	 * @param input Input string
	 * @return Payload map
	 */
	public static Map<String, String> parseTagged(String input) {
		if (input == null || input.isEmpty())
			return Map.of();

		// Create map
		String endKeyChars = "]]=[[";
		String nextKeyChars = "]]&&";
		HashMap<String, String> payload = new HashMap<String, String>();
		boolean isKey = true;
		boolean isValue = false;
		int startIndex = 0;
		String keyBuffer = "";
		String valueBuffer = "";
		String buffer = "";
		for (char ch : input.toCharArray()) {
			if (isKey) {
				// Check characters
				if (startIndex < 2) {
					if (ch == '[')
						startIndex++;
					else {
						// Invalid
						return payload;
					}
				} else {
					// Add to buffer
					buffer += ch;

					// Check end
					if (buffer.length() < endKeyChars.length() && !endKeyChars.startsWith(buffer)) {
						keyBuffer += buffer;
						buffer = "";
					} else if (buffer.length() == endKeyChars.length()) {
						// Switch to value reading
						isKey = false;
						startIndex = 0;
						isValue = true;
						buffer = "";
					}
				}
			} else if (isValue) {
				// Add to buffer
				buffer += ch;

				// Check end
				if (buffer.length() < nextKeyChars.length() && !nextKeyChars.startsWith(buffer)) {
					valueBuffer += buffer;
					buffer = "";
				} else if (buffer.length() == nextKeyChars.length()) {
					// Add entry and switch to the move to the next key
					payload.put(keyBuffer, valueBuffer);
					startIndex = 0;
					isKey = true;
					isValue = false;
					keyBuffer = "";
					valueBuffer = "";
					buffer = "";
				}
			}
		}

		// Check remaining
		if (isValue && buffer.equals("]]")) {
			payload.put(keyBuffer, valueBuffer);
		}

		// Return
		return payload;
	}

}
