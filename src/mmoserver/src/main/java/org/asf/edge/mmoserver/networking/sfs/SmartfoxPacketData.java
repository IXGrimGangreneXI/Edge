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
		if (pkt.payload.has("ec")) {
			pkt.hasError = true;
			pkt.errorCode = SfsErrorCode.fromShort(pkt.payload.getShort("ec"));
			pkt.errorParams = new String[0];
			if (pkt.payload.has("ep"))
				pkt.errorParams = pkt.payload.getStringArray("ep");
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
			mp.getObject("p").setShort("ec", errorCode.toShort());
			mp.getObject("p").setStringArray("ep", errorParams);
		}
		return mp;
	}

}
