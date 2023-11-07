package org.asf.edge.modules.gridclient.phoenix.events;

import org.asf.edge.modules.gridclient.phoenix.PhoenixClient;
import org.asf.edge.modules.gridclient.phoenix.networking.DataReader;
import org.asf.edge.modules.gridclient.phoenix.networking.DataWriter;
import org.asf.nexus.events.EventPath;

@EventPath("phoenix.client.programhandshake")
public class ClientProgramHandshakeEvent extends PhoenixEvent {

	private DataReader reader;
	private DataWriter writer;
	private boolean failed;

	public boolean hasFailed() {
		return failed;
	}

	public ClientProgramHandshakeEvent(PhoenixClient client, DataReader reader, DataWriter writer) {
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

	/**
	 * Fails the program handshake
	 */
	public void failHandshake() {
		failed = true;
		setHandled();
	}

	@Override
	public String eventPath() {
		return "phoenix.client.programhandshake";
	}

}
