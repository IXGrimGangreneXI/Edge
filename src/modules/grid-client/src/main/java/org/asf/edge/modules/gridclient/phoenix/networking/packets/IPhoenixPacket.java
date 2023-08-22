package org.asf.edge.modules.gridclient.phoenix.networking.packets;

import java.io.IOException;

import org.asf.edge.modules.gridclient.phoenix.networking.DataReader;
import org.asf.edge.modules.gridclient.phoenix.networking.DataWriter;

/**
 *
 * Packet interface
 * 
 * @author Sky Swimmer
 *
 */
public interface IPhoenixPacket {

	/**
	 * Instantiates the packet
	 * 
	 * @return IPhoenixPacket instance
	 */
	public IPhoenixPacket instantiate();

	/**
	 * Defines if the packet is length-prefixed
	 * 
	 * @return True if length-prefixed, false otherwise
	 */
	public default boolean lengthPrefixed() {
		return true;
	}

	/**
	 * Defines if the packet is synchronized to the reader thread
	 * 
	 * @return True if synchronized, false otherwise
	 */
	public default boolean isSynchronized() {
		return false;
	}

	/**
	 * Parses the Phoenix packet
	 * 
	 * @param reader Data reader
	 */
	public void parse(DataReader reader) throws IOException;

	/**
	 * Builds the Phoenix packet
	 * 
	 * @param writer Data writer
	 * @return Packet content
	 */
	public void build(DataWriter writer) throws IOException;;

}
