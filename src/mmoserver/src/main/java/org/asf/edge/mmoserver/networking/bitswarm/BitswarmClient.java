package org.asf.edge.mmoserver.networking.bitswarm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * 
 * Bitswarm-compatible client implementation
 * 
 * @author Sky Swimmer
 *
 */
public class BitswarmClient {

	private InputStream input;
	private OutputStream output;

	public BitswarmClient(InputStream input, OutputStream output) {
		this.input = input;
		this.output = output;
	}

	/**
	 * Reads a single packet
	 * 
	 * @return Packet bytes
	 * @throws IOException if reading fails
	 */
	public byte[] readPacket() throws IOException {
		synchronized (input) {
			// Read header
			int b = input.read();
			if (b == -1)
				throw new IOException("Disconnected");
			boolean encrypted = ((b & 64) > 0);
			boolean compressed = ((b & 32) > 0);
			boolean largeSize = ((b & 8) > 0);

			// Read length
			int length = (largeSize ? readInt(input) : readShort(input));

			// Read body
			byte[] payload = input.readNBytes(length);

			// Decrypt
			if (encrypted) {
				encrypted = encrypted;
				throw new IOException("Encryption not supported"); // FIXME
			}

			// Decompress
			if (compressed) {
				ByteArrayInputStream bIn = new ByteArrayInputStream(payload);
				InflaterInputStream inInf = new InflaterInputStream(bIn);
				payload = inInf.readAllBytes();
				inInf.close();
			}

			// Return
			return payload;
		}
	}

	/**
	 * Writes packets
	 * 
	 * @param payload   Packet to write
	 * @param encrypted True if encrypted, false otherwise
	 * @throws IOException if writing fails
	 */
	public void writePacket(byte[] payload, boolean encrypted) throws IOException {
		synchronized (output) {
			// Write bitswarm packet
			boolean compressed = payload.length >= 2000000; // If more than 2mb, compress

			// Compress if needed
			if (compressed) {
				ByteArrayOutputStream bOut = new ByteArrayOutputStream();
				DeflaterOutputStream dOut = new DeflaterOutputStream(bOut);
				dOut.write(payload);
				dOut.close();
				payload = bOut.toByteArray();
			}

			// Encrypt if needed
			if (encrypted) {
				encrypted = encrypted;
				throw new IOException("Encryption not supported"); // FIXME
			}

			// Compute length
			boolean largeSize = payload.length > Short.MAX_VALUE;

			// Build header
			int header = 0;
			if (encrypted)
				header = header | 64;
			if (compressed)
				header = header | 32;
			if (largeSize)
				header = header | 8;

			// Write header
			output.write(header);

			// Write length
			if (largeSize)
				writeInt(output, payload.length);
			else
				writeShort(output, (short) payload.length);

			// Write payload
			output.write(payload);
		}
	}

	private int readInt(InputStream strm) throws IOException {
		return ByteBuffer.wrap(strm.readNBytes(4)).getInt();
	}

	private int readShort(InputStream strm) throws IOException {
		return ByteBuffer.wrap(strm.readNBytes(2)).getShort();
	}

	private void writeInt(OutputStream strm, int val) throws IOException {
		strm.write(ByteBuffer.allocate(4).putInt(val).array());
	}

	private void writeShort(OutputStream strm, short val) throws IOException {
		strm.write(ByteBuffer.allocate(2).putShort(val).array());
	}

}
