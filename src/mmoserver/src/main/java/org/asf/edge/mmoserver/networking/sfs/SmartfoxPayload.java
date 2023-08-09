package org.asf.edge.mmoserver.networking.sfs;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * Smartfox Network Payload Object
 * 
 * @author Sky Swimmer
 *
 */
public class SmartfoxPayload {
	private Map<String, Object> data = new LinkedHashMap<String, Object>();

	/**
	 * Creates a new empty payload object
	 * 
	 * @return SmartfoxPayload instance
	 */
	public static SmartfoxPayload create() {
		return new SmartfoxPayload();
	}

	/**
	 * Creates a payload object from a smartfox object
	 * 
	 * @param data Smartfox object
	 * @return SmartfoxPayload instance
	 */
	public static SmartfoxPayload fromObject(Map<String, Object> data) {
		SmartfoxPayload payload = create();
		payload.data = data;
		return payload;
	}

	/**
	 * Converts this payload object to a smartfox object
	 * 
	 * @return Smartfox object map
	 */
	public Map<String, Object> toSfsObject() {
		return data;
	}

}
