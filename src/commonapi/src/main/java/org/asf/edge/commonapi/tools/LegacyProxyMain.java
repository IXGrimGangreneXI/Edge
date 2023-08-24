package org.asf.edge.commonapi.tools;

import java.io.IOException;
import java.io.InputStream;
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
import org.asf.edge.common.EdgeServerEnvironment;
import org.asf.edge.common.util.TripleDesUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class LegacyProxyMain {

	private static class ProxyProcessor extends HttpPushProcessor {

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

		@JsonIgnoreProperties(ignoreUnknown = true)
		@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
		private static class ServerList {

			@JsonProperty("MMOServerInfo")
			@JacksonXmlElementWrapper(useWrapping = false)
			public ObjectNode[] servers;

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

			// Proxy
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
				if (getRequest().hasRequestBody()) {
					conn.setDoOutput(true);
					getRequest().transferRequestBody(conn.getOutputStream());
				}

				// Read response code
				int responseCode = conn.getResponseCode();
				String responseMessage = conn.getResponseMessage();
				setResponseStatus(responseCode, responseMessage);

				// Read headers
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
				}
				// Retrieve body
				InputStream body;
				if (responseCode >= 400)
					body = conn.getErrorStream();
				else
					body = conn.getInputStream();

				// Set response
				setResponseContent(body);
			} catch (IOException e) {
				// Log error
				JsonObject respInfo = new JsonObject();
				respInfo.addProperty("status", 500);
				respInfo.addProperty("statusMessage", "[Proxy] Failed to proxy to " + url);
				respInfo.add("headers", new JsonObject());
				respInfo.addProperty("responseBody", "");
				setResponseStatus(404, "Not found");
				LogManager.getLogger("Proxy").error("Failed to proxy to " + url, e);
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
		EdgeServerEnvironment.initAll();

		// Create logger
		Logger log = LogManager.getLogger("Proxy");
		log.info("Setting up proxy...");

		// Load config
		log.info("Loading configuration...");
		JsonObject config = JsonParser.parseString(Files.readString(Path.of("proxy.json"))).getAsJsonObject();

		// Download manifest
		log.info("Downloading application manifest...");
		String url = config.get("mediaServer").getAsString();
		if (!url.endsWith("/"))
			url += "/";
		url += "DWADragonsUnity/" + config.get("platform").getAsString() + "/" + config.get("version").getAsString()
				+ "/DWADragonsMain.xml";
		URL u = new URL(url);

		// Compute key
		String man;
		if (!config.get("secret").getAsString().equals("unset")) {
			InputStream strm = u.openStream();
			byte[] enc = Base64.getDecoder().decode(strm.readAllBytes());
			strm.close();

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
			man = new String(dec, "ASCII");
		} else {
			InputStream strm = u.openStream();
			man = new String(strm.readAllBytes(), "UTF-8");
			strm.close();
		}

		// Modify manifest
		log.info("Modifying manifest...");
		JsonObject replace = config.get("manifestReplace").getAsJsonObject();
		for (String replaceKey : replace.keySet())
			man = man.replace(replaceKey, replace.get(replaceKey).getAsString().replace("%local%",
					config.get("host").getAsString() + ":" + config.get("port").getAsString()));

		// Re-encrypt
		if (!config.get("secret").getAsString().equals("unset")) {
			log.info("Computing key...");

			// Compute key
			byte[] key;
			try {
				MessageDigest digest = MessageDigest.getInstance("MD5");
				key = digest.digest(config.get("secret").getAsString().getBytes("ASCII"));
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}

			// Re-encrypt
			log.info("Re-encrypting manifest...");
			encryptedManifest = Base64.getEncoder().encodeToString(TripleDesUtil.encrypt(man.getBytes("ASCII"), key));
		} else
			encryptedManifest = man;

		// Create http server
		log.info("Setting up http server...");
		ConnectiveHttpServer srv = ConnectiveHttpServer.create("HTTP/1.1",
				Map.of("address", config.get("host").getAsString(), "port", config.get("port").getAsString()));
		srv.registerProcessor(new ManifestRequestProcessor("/DWADragonsUnity/" + config.get("platform").getAsString()
				+ "/" + config.get("version").getAsString() + "/DWADragonsMain.xml"));
		srv.registerProcessor(new ProxyProcessor());

		// Start
		log.info("Starting proxy server on " + config.get("host").getAsString() + ":" + config.get("port").getAsString()
				+ "...");
		srv.start();
		log.info("Proxy is running!");
		log.info("Modified manifest published to http://" + config.get("host").getAsString() + ":"
				+ config.get("port").getAsString() + "/DWADragonsUnity/" + config.get("platform").getAsString() + "/"
				+ config.get("version").getAsString() + "/DWADragonsMain.xml");
		srv.waitForExit();
	}

}
