package org.asf.edge.mmoserver.networking.packets;

import org.asf.edge.mmoserver.networking.sfs.SmartfoxPacketData;

/**
 * 
 * Smartfox Packet Interface
 * 
 * @author Sky Swimmer
 * 
 */
public interface ISmartfoxPacket {

	/**
	 * Called to create a new instance of the packet type
	 * 
	 * @return ISmartfoxPacket instance
	 */
	public ISmartfoxPacket createInstance();

	/**
	 * Defines if the packet is synchronized to the reader thread
	 * 
	 * @return True if synchronized, false otherwise
	 */
	public default boolean isSynchronized() {
		return false;
	}

	/**
	 * Checks if the packet type is valid for the given data
	 * 
	 * @param packet Packet data
	 * @return True if valid, false otherwise
	 */
	public default boolean matches(SmartfoxPacketData packet) {
		return true;
	}

	/**
	 * Defines the packet ID
	 * 
	 * @return Packet ID
	 */
	public short packetID();

	/**
	 * Called to parse the packet
	 * 
	 * @param packet Packet data container
	 */
	public void parse(SmartfoxPacketData packet);

	/**
	 * Called to build the packet
	 * 
	 * @param packet Packet data container
	 */
	public void build(SmartfoxPacketData packet);

}
