package org.asf.edge.modules.gridclient.phoenix.certificate;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.asf.edge.modules.gridclient.phoenix.networking.DataReader;

public class ServerCertificatePayload {

	public String gameID;
	public String serverID;
	public long generationTimestamp;
	public long expiryTimestamp;
	public long serverTime;
	public byte[] signature;
	public byte[] certificate;

	public static ServerCertificatePayload fromReader(DataReader reader) throws IOException {
		// Read
		ServerCertificatePayload payload = new ServerCertificatePayload();
		payload.certificate = reader.readBytes();
		payload.signature = reader.readBytes();

		// Read certificate
		reader = new DataReader(new ByteArrayInputStream(payload.certificate));
		payload.gameID = reader.readString();
		payload.serverID = reader.readString();
		payload.serverTime = reader.readLong();
		payload.generationTimestamp = reader.readLong();
		payload.expiryTimestamp = reader.readLong();

		// Return
		return payload;
	}

}
