package org.asf.edge.commonapi.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.CommonInit;
import org.asf.edge.common.util.TripleDesUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SnifferMain {

	private static class ProxyProcessor extends HttpPushProcessor {

		private static OutputStream fOut;
		private static Object writeLock = new Object();

		static {
			new File("sniffs").mkdirs();
			try {
				fOut = new FileOutputStream("sniffs/sniff-" + System.currentTimeMillis() + ".log");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public HttpPushProcessor createNewInstance() {
			return new ProxyProcessor();
		}

		@Override
		public String path() {
			return "/";
		}

		@Override
		public boolean supportsNonPush() {
			return true;
		}

		@Override
		public boolean supportsChildPaths() {
			return true;
		}

		@Override
		public void process(String path, String method, RemoteClient client, String contentType) throws IOException {
			// Parse path
			if (path.startsWith("/"))
				path = path.substring(1);
			if (path.split("/").length < 2) {
				setResponseStatus(400, "Bad Request");
				return;
			}

			// Parse remote
			String protocol = path;
			String server = protocol.substring(protocol.indexOf("/") + 1);
			protocol = protocol.substring(0, protocol.indexOf("/"));
			path = server.substring(server.indexOf("/"));
			server = server.substring(0, server.indexOf("/"));

			// Build url
			String url = protocol + "://" + server + path;
			if (!getRequest().getRequestQuery().isEmpty())
				url += "?" + getRequest().getRequestQuery();

			// Build request info json
			JsonObject requestInfo = new JsonObject();
			requestInfo.addProperty("url", url);
			requestInfo.addProperty("method", method);
			JsonObject headers = new JsonObject();
			for (String name : getHeaders().getHeaderNames()) {
				JsonArray hValues = new JsonArray();
				for (String value : getHeaders().getHeaderValues(name))
					hValues.add(value);
				headers.add(name, hValues);
			}
			requestInfo.add("headers", headers);
			byte[] payload = null;
			requestInfo.addProperty("hasBody", getRequest().hasRequestBody());
			if (getRequest().hasRequestBody()) {
				// Read request body
				ByteArrayOutputStream outp = new ByteArrayOutputStream();
				getRequest().transferRequestBody(outp);
				payload = outp.toByteArray();
				requestInfo.addProperty("requestBody", Base64.getEncoder().encodeToString(payload));
			}
			try {
				// Create request
				HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
				conn.setRequestMethod(method);
				conn.setDoInput(true);

				// Set headers
				for (String name : getHeaders().getHeaderNames()) {
					if (name.equalsIgnoreCase("Host"))
						continue;
					for (String value : getHeaders().getHeaderValues(name))
						conn.addRequestProperty(name, value);
				}

				// Write body
				if (payload != null) {
					conn.setDoOutput(true);
					conn.getOutputStream().write(payload);
				}

				// Create response json
				JsonObject respInfo = new JsonObject();

				// Read response code
				int responseCode = conn.getResponseCode();
				String responseMessage = conn.getResponseMessage();
				setResponseStatus(responseCode, responseMessage);
				respInfo.addProperty("status", responseCode);
				respInfo.addProperty("statusMessage", responseMessage);

				// Read headers
				JsonObject respHeaders = new JsonObject();
				for (String name : conn.getHeaderFields().keySet()) {
					if (name == null || name.equalsIgnoreCase("transfer-encoding"))
						continue;
					JsonArray hValues = new JsonArray();
					boolean first = true;
					for (String value : conn.getHeaderFields().get(name)) {
						hValues.add(value);
						if (first) {
							first = false;
							setResponseHeader(name, value, false);
						} else
							setResponseHeader(name, value, true);
					}
					respHeaders.add(name, hValues);
				}
				respInfo.add("headers", respHeaders);

				// Retrieve body
				InputStream body;
				if (responseCode >= 400)
					body = conn.getErrorStream();
				else
					body = conn.getInputStream();
				try {
					// Read body
					byte[] respData = body.readAllBytes();
					respInfo.addProperty("responseBody", Base64.getEncoder().encodeToString(respData));

					// Set response
					setResponseContent(respData);
				} catch (IOException e) {
					respInfo.addProperty("responseBody", "");
				}

				// Check path
				if (path.equalsIgnoreCase("/v3/AuthenticationWebService.asmx/LoginParent")
						|| path.equalsIgnoreCase("/AuthenticationWebService.asmx/LoginChild")) {
					LogManager.getLogger("Sniffer").warn("Skipped logging " + url + " to prevent credential leak");
					return;
				}

				// Log
				logRequest(requestInfo, respInfo);
			} catch (IOException e) {
				// Log error
				JsonObject respInfo = new JsonObject();
				respInfo.addProperty("status", 500);
				respInfo.addProperty("statusMessage", "[Sniffer] Failed to proxy to " + url);
				respInfo.add("headers", new JsonObject());
				respInfo.addProperty("responseBody", "");
				setResponseStatus(404, "Not found");
				LogManager.getLogger("Sniffer").error("Failed to proxy to " + url, e);
				logRequest(requestInfo, respInfo);

				// Check path
				if (path.equalsIgnoreCase("/v3/AuthenticationWebService.asmx/LoginParent")
						|| path.equalsIgnoreCase("/AuthenticationWebService.asmx/LoginChild")) {
					LogManager.getLogger("Sniffer").warn("Skipped logging " + url + " to prevent credential leak");
					return;
				}
			}
		}

		private void logRequest(JsonObject requestInfo, JsonObject respInfo)
				throws UnsupportedEncodingException, IOException {
			// Write
			synchronized (writeLock) {
				// Build output
				JsonObject itm = new JsonObject();
				itm.addProperty("time", System.currentTimeMillis());
				itm.add("request", requestInfo);
				itm.add("response", respInfo);

				// Write
				fOut.write((itm.toString() + "\n\n").getBytes("UTF-8"));
				fOut.flush();
			}
		}

	}

	private static class ManifestRequestProcessor extends HttpPushProcessor {
		private String path;

		public ManifestRequestProcessor(String path) {
			this.path = path;
		}

		@Override
		public void process(String path, String method, RemoteClient client, String contentType) throws IOException {
			setResponseContent("text/xml", encryptedManifest);
		}

		@Override
		public HttpPushProcessor createNewInstance() {
			return new ManifestRequestProcessor(path);
		}

		@Override
		public String path() {
			return path;
		}

		@Override
		public boolean supportsNonPush() {
			return true;
		}

	}

	private static String encryptedManifest;

	public static void main(String[] args) throws IOException {
		// Init common
		CommonInit.initAll();

		// Create logger
		Logger log = LogManager.getLogger("Sniffer");
		log.info("Setting up sniffer...");

		// Load config
		log.info("Loading configuration...");
		JsonObject config = JsonParser.parseString(Files.readString(Path.of("sniffer.json"))).getAsJsonObject();

		// Download manifest
		log.info("Downloading application manifest...");
		String url = config.get("mediaServer").getAsString();
		if (!url.endsWith("/"))
			url += "/";
		url += "DWADragonsUnity/" + config.get("platform").getAsString() + "/" + config.get("version").getAsString()
				+ "/DWADragonsMain.xml";
		URL u = new URL(url);
		InputStream strm = u.openStream();
		byte[] enc = Base64.getDecoder().decode(strm.readAllBytes());
		strm.close();

		// Compute key
		log.info("Computing key...");
		// Compute key
		byte[] key;
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			key = digest.digest(config.get("secret").getAsString().getBytes("ASCII"));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		// Decrypt manifest
		log.info("Decrypting manifest...");
		byte[] dec = TripleDesUtil.decrypt(enc, key);
		String man = new String(dec, "ASCII");

		// Modify manifest
		log.info("Modifying manifest...");
		JsonObject replace = config.get("manifestReplace").getAsJsonObject();
		for (String replaceKey : replace.keySet())
			man = man.replace(replaceKey, replace.get(replaceKey).getAsString().replace("%local%",
					config.get("host").getAsString() + ":" + config.get("port").getAsString()));

		// Re-encrypt
		log.info("Re-encrypting manifest...");
		enc = TripleDesUtil.encrypt(man.getBytes("ASCII"), key);
		encryptedManifest = Base64.getEncoder().encodeToString(enc);

		// Create http server
		log.info("Setting up http server...");
		ConnectiveHttpServer srv = ConnectiveHttpServer.create("HTTP/1.1",
				Map.of("address", config.get("host").getAsString(), "port", config.get("port").getAsString()));
		srv.registerProcessor(new ManifestRequestProcessor("/DWADragonsUnity/" + config.get("platform").getAsString()
				+ "/" + config.get("version").getAsString() + "/DWADragonsMain.xml"));
		srv.registerProcessor(new ProxyProcessor());

		// Start
		log.info("Starting sniffer server on " + config.get("host").getAsString() + ":"
				+ config.get("port").getAsString() + "...");
		srv.start();
		log.info("Sniffer is running!");
		log.info("Modified manifest published to http://" + config.get("host").getAsString() + ":"
				+ config.get("port").getAsString() + "/DWADragonsUnity/" + config.get("platform").getAsString() + "/"
				+ config.get("version").getAsString() + "/DWADragonsMain.xml");
		srv.waitForExit();
	}

}
