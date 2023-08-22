package org.asf.edge.modules.gridclient.phoenix.networking.packets;

import java.io.IOException;

import org.asf.edge.modules.gridclient.phoenix.networking.channels.AbstractPacketChannel;

/**
 * 
 * Phoenix packet handler
 * 
 * @author Sky Swimmer
 *
 * @param <T> Packet type
 */
public interface IPacketHandler<T extends IPhoenixPacket> {

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
	 * @return True if the packet can be handled, false otehrwise
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
	public boolean handle(T packet, AbstractPacketChannel channel) throws IOException;

}
