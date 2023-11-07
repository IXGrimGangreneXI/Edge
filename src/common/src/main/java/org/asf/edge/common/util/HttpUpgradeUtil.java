package org.asf.edge.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import org.asf.nexus.common.io.IoUtil;

public class HttpUpgradeUtil {

	/**
	 * Creates a HTTP upgrade request and returns a full-duplex socket connection if
	 * the handshake passes
	 * 
	 * @param url             Server URL
	 * @param method          Upgrade method
	 * @param body            Request body (null for none)
	 * @param length          Request length (-1 for none)
	 * @param requestHeaders  Request headers
	 * @param upgradeProtocol Upgrade protocol string
	 * @return Socket instance
	 * @throws IOException If upgrade call fails
	 */
	public static Socket upgradeRequest(String url, String method, InputStream body, long length,
			Map<String, String> requestHeaders, Map<String, String> responseHeadersOutput, String upgradeProtocol,
			String expectedResponseProtocol) throws IOException {
		// Parse URL
		URL u = new URL(url);
		if (!u.getProtocol().equals("http") && !u.getProtocol().equals("https"))
			throw new IOException(
					"Unsupported protocol for protocol Upgrade to binary communication: " + u.getProtocol());
		Socket conn = (u.getProtocol().equals("http") ? new Socket(u.getHost(), u.getPort())
				: SSLSocketFactory.getDefault().createSocket(u.getHost(), u.getPort()));

		// Set content-length header if needed
		if (body != null && length != -1) {
			requestHeaders = new HashMap<String, String>(requestHeaders);
			requestHeaders.put("Content-Length", Long.toString(length));
		}

		// Write request
		conn.getOutputStream()
				.write(("POST " + u.getFile()
						+ (u.getQuery() != null && !u.getQuery().isEmpty() ? "?" + u.getQuery() : "") + " HTTP/1.1\r\n")
						.getBytes("UTF-8"));
		conn.getOutputStream().write(("Host: " + u.getHost() + "\r\n").getBytes("UTF-8"));
		for (String key : requestHeaders.keySet()) {
			String value = requestHeaders.get(key);
			conn.getOutputStream().write((key + ": " + value + "\r\n").getBytes("UTF-8"));
		}
		conn.getOutputStream().write(("Upgrade: " + upgradeProtocol + "\r\n").getBytes("UTF-8"));
		conn.getOutputStream().write("\r\n".getBytes("UTF-8"));

		// Transfer body
		if (body != null && length != -1) {
			int tr = 0;
			for (long i = 0; i < length; i += tr) {
				tr = Integer.MAX_VALUE / 1000;
				if ((length - (long) i) < tr) {
					tr = body.available();
					if (tr == 0) {
						conn.getOutputStream().write(body.read());
						i += 1;
					}
					tr = body.available();
				}
				conn.getOutputStream().write(IoUtil.readNBytes(body, tr));
			}
		}

		// Check response
		String line = readStreamLine(conn.getInputStream());
		String statusLine = line;
		if (!line.startsWith("HTTP/1.1 ")) {
			conn.close();
			throw new IOException("Server returned invalid protocol");
		}
		while (true) {
			line = readStreamLine(conn.getInputStream());
			if (line.equals(""))
				break;
			String key = line.substring(0, line.indexOf(": "));
			String value = line.substring(line.indexOf(": ") + 2);
			responseHeadersOutput.put(key, value);
		}

		// Transfer output body to memory
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		transferRequestBody(responseHeadersOutput, conn.getInputStream(), buffer);

		// Verify response
		int status = Integer.parseInt(statusLine.split(" ")[1]);
		if (status != 101) {
			conn.close();
			throw new IOException("Server returned HTTP " + statusLine.substring("HTTP/1.1 ".length()));
		}
		if (!responseHeadersOutput.containsKey("Upgrade") && !responseHeadersOutput.containsKey("upgrade")) {
			conn.close();
			throw new IOException("Server response did not contain a Upgrage header.");
		}
		if (responseHeadersOutput.containsKey("Upgrade")
				&& !responseHeadersOutput.get("Upgrade").equals(expectedResponseProtocol)) {
			conn.close();
			throw new IOException(
					"Server response used an invalid protocol: " + responseHeadersOutput.get("Upgrade") + ".");
		}
		if (responseHeadersOutput.containsKey("upgrade")
				&& !responseHeadersOutput.get("upgrade").equals(expectedResponseProtocol)) {
			conn.close();
			throw new IOException(
					"Server response used an invalid protocol: " + responseHeadersOutput.get("upgrade") + ".");
		}

		// Return connection
		return conn;
	}

	private static String readStreamLine(InputStream strm) throws IOException {
		String buffer = "";
		while (true) {
			char ch = (char) strm.read();
			if (ch == (char) -1)
				return null;
			if (ch == '\n') {
				return buffer;
			} else if (ch != '\r') {
				buffer += ch;
			}
		}
	}

	private static void transferRequestBody(Map<String, String> headers, InputStream bodyStream, OutputStream output)
			throws IOException {
		if (headers.containsKey("Content-Length")) {
			long length = Long.valueOf(headers.get("Content-Length"));
			int tr = 0;
			for (long i = 0; i < length; i += tr) {
				tr = Integer.MAX_VALUE / 1000;
				if ((length - (long) i) < tr) {
					tr = bodyStream.available();
					if (tr == 0) {
						output.write(bodyStream.read());
						i += 1;
					}
					tr = bodyStream.available();
				}
				output.write(IoUtil.readNBytes(bodyStream, tr));
			}
		}
	}

}
