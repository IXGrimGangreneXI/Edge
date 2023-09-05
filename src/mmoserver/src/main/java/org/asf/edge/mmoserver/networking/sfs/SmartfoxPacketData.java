package org.asf.edge.mmoserver.networking.sfs;

/**
 * 
 * Smartfox Packet Object
 * 
 * @author Sky Swimmer
 *
 */
public class SmartfoxPacketData {

	public short packetId;
	public byte channelID;

	public boolean hasError;
	public SfsErrorCode errorCode;
	public String[] errorParams;

	public SmartfoxPayload payload;

	/**
	 * Creates a SmartfoxPacket instance from a SFS object
	 * 
	 * @param obj Smartfox Object
	 * @return SmartfoxPacket instance
	 */
	public static SmartfoxPacketData fromSfsObject(SmartfoxPayload obj) {
		if (!obj.has("c") || !obj.has("a") || !obj.has("p")) {
			throw new IllegalArgumentException("Invalid packet");
		}
		SmartfoxPacketData pkt = new SmartfoxPacketData();
		pkt.packetId = obj.getShort("a");
		pkt.channelID = obj.getByte("c");
		pkt.payload = obj.getObject("p");
		if (obj.has("ec")) {
			pkt.hasError = true;
			pkt.errorCode = SfsErrorCode.fromShort(obj.getShort("ec"));
			pkt.errorParams = new String[0];
			if (obj.has("ep"))
				pkt.errorParams = obj.getStringArray("ep");
		}
		return pkt;
	}

	/**
	 * Creates a SFS object for this smartfox packet
	 * 
	 * @return Smartfox object
	 */
	public SmartfoxPayload toSfsObject() {
		SmartfoxPayload mp = SmartfoxPayload.create();
		mp.setByte("c", channelID);
		mp.setShort("a", packetId);
		mp.setObject("p", payload);
		if (hasError) {
			mp.setShort("ec", errorCode.toShort());
			mp.setStringArray("ep", errorParams);
		}
		return mp;
	}

}
