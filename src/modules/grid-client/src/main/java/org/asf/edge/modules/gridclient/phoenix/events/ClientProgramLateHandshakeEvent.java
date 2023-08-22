package org.asf.edge.modules.gridclient.phoenix.events;

import org.asf.edge.modules.eventbus.EventPath;
import org.asf.edge.modules.gridclient.phoenix.PhoenixClient;
import org.asf.edge.modules.gridclient.phoenix.networking.DataReader;
import org.asf.edge.modules.gridclient.phoenix.networking.DataWriter;

@EventPath("phoenix.client.programlatehandshake")
public class ClientProgramLateHandshakeEvent extends PhoenixEvent {

	private DataReader reader;
	private DataWriter writer;

	public ClientProgramLateHandshakeEvent(PhoenixClient client, DataReader reader, DataWriter writer) {
		super(client);
		this.reader = reader;
		this.writer = writer;
	}

	/**
	 * Retrieves the program handshake data reader
	 * 
	 * @return DataReader instance
	 */
	public DataReader getReader() {
		return reader;
	}

	/**
	 * Retrieves the program handshake data writer
	 * 
	 * @return DataWriter instance
	 */
	public DataWriter getWriter() {
		return writer;
	}

	@Override
	public String eventPath() {
		return "phoenix.client.programlatehandshake";
	}

}
