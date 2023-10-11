package org.asf.edge.mmoserver.networking.packets;

import java.io.IOException;

/**
 * 
 * Smartfox packet handler
 * 
 * @author Sky Swimmer
 *
 * @param <T> Packet type
 */
public interface IPacketHandler<T extends ISmartfoxPacket> {

	/**
	 * Defines the packet class
	 * 
	 * @return Packet class instance
	 */
	public Class<T> packetClass();

	/**
	 * Checks if a packet can be handled
	 * 
	 * @param packet Packet to handle
	 * @return True if the packet can be handled, false otherwise
	 */
	public default boolean canHandle(T packet) {
		return true;
	}

	/**
	 * Called to handle packets
	 * 
	 * @param packet  Packet to handle
	 * @param channel Packet channel
	 * @return True if handled, false otherwise
	 */
	public boolean handle(T packet, PacketChannel channel) throws IOException;

}
