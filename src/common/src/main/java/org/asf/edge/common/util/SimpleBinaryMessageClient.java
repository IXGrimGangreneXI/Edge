package org.asf.edge.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.function.BiFunction;

import org.asf.connective.tasks.AsyncTaskManager;

public class SimpleBinaryMessageClient {

	private BiFunction<Packet, SimpleBinaryMessageClient, Boolean> handler;

	public Object container;

	private InputStream input;
	private OutputStream output;

	private long lastSent;
	private Object packetLock = new Object();
	private boolean connected;

	public static class Packet {
		public byte type;
		public byte[] data;
	}

	public SimpleBinaryMessageClient(BiFunction<Packet, SimpleBinaryMessageClient, Boolean> handler, InputStream input,
			OutputStream output) {
		this.handler = handler;
		this.input = input;
		this.output = output;
	}

	public void send(byte type, byte[] packet) throws IOException {
		if (type < 2)
			throw new IllegalArgumentException("Invalid packet type, must be above or equal to 2");
		sendPacketInternal(type, packet);
	}

	public void send(byte[] packet) throws IOException {
		sendPacketInternal((byte) 2, packet);
	}

	private void sendPacketInternal(byte type, byte[] packet) throws IOException {
		synchronized (packetLock) {
			try {
				// Write type
				output.write(type);
				if (type >= 2) {
					// Write length
					output.write(ByteBuffer.allocate(4).putInt(packet.length).array());
					output.write(packet);
				}
				lastSent = System.currentTimeMillis();
			} catch (IOException e) {
				if (connected)
					stop();
				throw e;
			}
		}
	}

	public void stop() {
		connected = false;
		try {
			// Send disconnect
			sendPacketInternal((byte) 0, new byte[0]);
		} catch (IOException e) {
		}

		// Disconnect streams
		try {
			input.close();
		} catch (IOException e) {
		}
		try {
			output.close();
		} catch (IOException e) {
		}
	}

	public boolean isConnected() {
		return connected;
	}

	public void startAsync() {
		// Start handling
		lastSent = System.currentTimeMillis();
		connected = true;
		AsyncTaskManager.runAsync(() -> start());
	}

	public void start() {
		// Start handling
		lastSent = System.currentTimeMillis();
		connected = true;

		// Start pinger
		AsyncTaskManager.runAsync(() -> {
			while (connected) {
				if ((System.currentTimeMillis() - lastSent) >= 5000) {
					// Ping
					try {
						sendPacketInternal((byte) 1, new byte[0]);
					} catch (IOException e) {
					}
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
		});

		// Start reader
		try {
			while (connected) {
				int type = input.read();
				switch (type) {

				// Disconnect
				case 0: {
					try {
						input.close();
					} catch (IOException e) {
					}
					try {
						output.close();
					} catch (IOException e) {
					}
					connected = false;
					break;
				}

				// Ping
				case 1: {
					break;
				}

				// Payload
				default: {
					// Read
					byte[] l = input.readNBytes(4);
					int length = ByteBuffer.wrap(l).getInt();

					// Handle
					Packet pk = new Packet();
					pk.type = (byte) type;
					pk.data = input.readNBytes(length);
					if (!handler.apply(pk, this))
						stop();
					break;
				}

				}
			}
		} catch (IOException e) {
		}
		connected = false;
	}

}
