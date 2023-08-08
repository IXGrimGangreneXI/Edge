package org.asf.edge.mmoserver.networking.impl;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

import org.asf.edge.mmoserver.networking.SmartfoxClient;
import org.asf.edge.mmoserver.networking.SmartfoxServer;
import org.asf.edge.mmoserver.networking.bitswarm.BitswarmClient;
import org.asf.edge.mmoserver.networking.impl.BitswarmSmartfoxServer.BitswarmClientContainer;

public class BitswarmSmartfoxClient extends SmartfoxClient {

	private BitswarmClient client;
	private SocketAddress addr;
	BitswarmSmartfoxServer server;
	BitswarmClientContainer container;
	Socket socket;

	public BitswarmSmartfoxClient(BitswarmClient client, Socket socket, BitswarmSmartfoxServer server) {
		this.client = client;
		this.socket = socket;
		this.server = server;
		this.addr = socket.getRemoteSocketAddress();
	}

	@Override
	public boolean isConnected() {
		return socket != null;
	}

	@Override
	protected void disconnectClient() {
		server.onClientDisconnect(this);
	}

	@Override
	public String getRemoteAddress() {
		return addr.toString();
	}

	@Override
	public SmartfoxServer getServer() {
		return server;
	}

	void callDisconnectEventsInternal() {
		this.callDisconnectEvents();
	}

	@Override
	protected byte[] readSingleRawPacket() throws IOException {
		return client.readPacket();
	}

	@Override
	protected void writeSingleRawPacket(byte[] packet) throws IOException {
		client.writePacket(packet, false);
	}

}
