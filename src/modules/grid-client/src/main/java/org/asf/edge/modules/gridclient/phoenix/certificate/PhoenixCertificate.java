package org.asf.edge.modules.gridclient.phoenix.certificate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.asf.edge.modules.gridclient.phoenix.PhoenixEnvironment;
import org.asf.edge.modules.gridclient.phoenix.networking.DataReader;

public class PhoenixCertificate {

	public String gameID;
	public String serverID;
	public long timestamp;
	public long expiry;

	public String[] addresses;
	public PublicKey publicKey;

	public static PhoenixCertificate downloadFromURL(String url) throws IOException {
		URL u = new URL(url);
		InputStream strm = u.openStream();
		byte[] certD = strm.readAllBytes();
		PhoenixCertificate cert = fromReader(new DataReader(new ByteArrayInputStream(certD)));
		strm.close();
		return cert;
	}

	public static PhoenixCertificate downloadFromAPI(String gameID, String id) throws IOException {
		return downloadFromAPI(PhoenixEnvironment.defaultAPIServer, gameID, id);
	}

	public static PhoenixCertificate downloadFromAPI(String url, String gameID, String id) throws IOException {
		if (!url.endsWith("/"))
			url += "/";
		url += "servers/certificate/" + gameID + "/" + id;
		return downloadFromURL(url);
	}

	public static PhoenixCertificate fromReader(DataReader reader) throws IOException {
		// Read
		PhoenixCertificate payload = new PhoenixCertificate();
		payload.gameID = reader.readString();
		payload.serverID = reader.readString();
		payload.addresses = new String[reader.readInt()];
		for (int i = 0; i < payload.addresses.length; i++)
			payload.addresses[i] = reader.readString();
		payload.timestamp = reader.readLong();
		payload.expiry = reader.readLong();

		try {
			// Parse key
			KeyFactory fac = KeyFactory.getInstance("RSA");
			payload.publicKey = fac
					.generatePublic(new X509EncodedKeySpec(pemDecode(new String(reader.readAllBytes(), "UTF-8"))));
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new IOException("Key decoding error", e);
		}

		// Return
		return payload;
	}

	// PEM parser
	private static byte[] pemDecode(String pem) {
		String base64 = pem.replace("\r", "");

		// Strip header
		while (base64.startsWith("-"))
			base64 = base64.substring(1);
		while (!base64.startsWith("-"))
			base64 = base64.substring(1);
		while (base64.startsWith("-"))
			base64 = base64.substring(1);

		// Clean data
		base64 = base64.replace("\n", "");

		// Strip footer
		while (base64.endsWith("-"))
			base64 = base64.substring(0, base64.length() - 1);
		while (!base64.endsWith("-"))
			base64 = base64.substring(0, base64.length() - 1);
		while (base64.endsWith("-"))
			base64 = base64.substring(0, base64.length() - 1);

		// Decode and return
		return Base64.getDecoder().decode(base64);
	}
}
