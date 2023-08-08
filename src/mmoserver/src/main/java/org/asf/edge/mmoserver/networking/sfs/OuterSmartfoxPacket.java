package org.asf.edge.mmoserver.networking.sfs;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * Smartfox Packet Object
 * 
 * @author Sky Swimmer
 *
 */
public class OuterSmartfoxPacket {

	public short packetId;
	public byte targetController;

	public Map<String, Object> payload;

	/**
	 * Creates a SmartfoxPacket instance from a SFS object
	 * 
	 * @param obj Smartfox Object
	 * @return SmartfoxPacket instance
	 */
	@SuppressWarnings("unchecked")
	public static OuterSmartfoxPacket fromSfsObject(Map<String, Object> obj) {
		if (!obj.containsKey("c") || !obj.containsKey("a") || !obj.containsKey("p")) {
			throw new IllegalArgumentException("Invalid packet");
		}
		OuterSmartfoxPacket pkt = new OuterSmartfoxPacket();
		pkt.packetId = (short) obj.get("a");
		pkt.targetController = (byte) obj.get("c");
		pkt.payload = (Map<String, Object>) obj.get("p");
		return pkt;
	}

	/**
	 * Creates a SFS object for this smartfox packet
	 * 
	 * @return Smartfox object
	 */
	public Map<String, Object> toSfsObject() {
		LinkedHashMap<String, Object> mp = new LinkedHashMap<String, Object>();
		mp.put("c", targetController);
		mp.put("a", packetId);
		mp.put("p", payload);
		return mp;
	}

}
