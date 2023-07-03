package org.asf.edge.commonapi.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.common.CommonInit;
import org.asf.edge.common.util.TripleDesUtil;
import org.bouncycastle.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SnifferMain {

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

	private static int currentPort = 9933;
	private static HashMap<String, Integer> binaryProxies = new HashMap<String, Integer>();

	private static class BinaryProxy {
		private ServerSocket srvTcp;
		private DatagramSocket srvUdp;

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

		private HashMap<String, DatagramSocket> clients = new HashMap<String, DatagramSocket>();

		public void start(int localPort, String remoteHost, int remotePort) throws UnknownHostException, IOException {
			// Start tcp proxy
			srvTcp = new ServerSocket(localPort, 0, InetAddress.getLoopbackAddress());
			AsyncTaskManager.runAsync(() -> {
				while (true) {
					try {
						Socket client = srvTcp.accept();

						// Connection accepted
						AsyncTaskManager.runAsync(() -> {
							// Attempt connection with remote
							try {
								Socket remoteConn = new Socket(InetAddress.getByName(remoteHost), remotePort);

								// Connection established with remote

								// Start sniffer
								AsyncTaskManager.runAsync(() -> {
									// Reader
									while (client.isConnected()) {
										try {
											// Read single bitswarm packet

											// Read header
											int b = client.getInputStream().read();
											if (b == -1)
												throw new IOException("Disconnected");
											boolean encrypted = ((b & 64) > 0);
											boolean compressed = ((b & 32) > 0);
											boolean blueBoxed = ((b & 16) > 0);
											boolean largeSize = ((b & 8) > 0);

											// Read length
											int length = (largeSize ? readInt(client.getInputStream())
													: readShort(client.getInputStream()));

											// Read body
											byte[] payload = client.getInputStream().readNBytes(length);

											// Decompress and decrypt
											if (encrypted)
												throw new IOException("Encryption not supported");
											if (compressed) {
												ByteArrayInputStream bIn = new ByteArrayInputStream(payload);
												InflaterInputStream inInf = new InflaterInputStream(bIn);
												payload = inInf.readAllBytes();
												inInf.close();
											}

											// Write
											synchronized (writeLock) {
												// Build output
												JsonObject itm = new JsonObject();
												itm.addProperty("time", System.currentTimeMillis());
												itm.addProperty("type", "bstcp");
												itm.addProperty("host", remoteHost);
												itm.addProperty("port", remotePort);
												itm.addProperty("side", "C->S");
												itm.addProperty("data", Base64.getEncoder().encodeToString(payload));

												// Write
												fOut.write((itm.toString() + "\n\n").getBytes("UTF-8"));
												fOut.flush();
											}

											// Write bitswarm packet
											compressed = payload.length >= 2000000; // If more than 2mb, compress

											// Compress if needed
											if (compressed) {
												ByteArrayOutputStream bOut = new ByteArrayOutputStream();
												DeflaterOutputStream dOut = new DeflaterOutputStream(bOut);
												dOut.write(payload);
												dOut.close();
												payload = bOut.toByteArray();
											}

											// Encrypt if needed
											// TODO

											// Compute length
											largeSize = payload.length > Short.MAX_VALUE;

											// Build header
											int header = 0;
											if (encrypted)
												header = header | 64;
											if (compressed)
												header = header | 32;
											if (blueBoxed)
												header = header | 16;
											if (largeSize)
												header = header | 8;

											// Write header
											remoteConn.getOutputStream().write(header);

											// Write length
											if (largeSize)
												writeInt(remoteConn.getOutputStream(), payload.length);
											else
												writeShort(remoteConn.getOutputStream(), (short) payload.length);

											// Write payload
											remoteConn.getOutputStream().write(payload);
										} catch (IOException e) {
											try {
												client.close();
											} catch (IOException e1) {
											}
											try {
												remoteConn.close();
											} catch (IOException e1) {
											}
											break;
										}
									}
								});
								AsyncTaskManager.runAsync(() -> {
									// Writer
									while (remoteConn.isConnected()) {
										try {
											// Read single bitswarm packet

											// Read header
											int b = remoteConn.getInputStream().read();
											if (b == -1)
												throw new IOException("Disconnected");
											boolean encrypted = ((b & 64) > 0);
											boolean compressed = ((b & 32) > 0);
											boolean blueBoxed = ((b & 16) > 0);
											boolean largeSize = ((b & 8) > 0);

											// Read length
											int length = (largeSize ? readInt(remoteConn.getInputStream())
													: readShort(remoteConn.getInputStream()));

											// Read body
											byte[] payload = remoteConn.getInputStream().readNBytes(length);

											// Decompress and decrypt
											if (encrypted)
												throw new IOException("Encryption not supported");
											if (compressed) {
												ByteArrayInputStream bIn = new ByteArrayInputStream(payload);
												InflaterInputStream inInf = new InflaterInputStream(bIn);
												payload = inInf.readAllBytes();
												inInf.close();
											}

											// Write
											synchronized (writeLock) {
												// Build output
												JsonObject itm = new JsonObject();
												itm.addProperty("time", System.currentTimeMillis());
												itm.addProperty("type", "bstcp");
												itm.addProperty("host", remoteHost);
												itm.addProperty("port", remotePort);
												itm.addProperty("side", "S->C");
												itm.addProperty("data", Base64.getEncoder().encodeToString(payload));

												// Write
												fOut.write((itm.toString() + "\n\n").getBytes("UTF-8"));
												fOut.flush();
											}

											// Write bitswarm packet
											compressed = payload.length >= 2000000; // If more than 2mb, compress

											// Compress if needed
											if (compressed) {
												ByteArrayOutputStream bOut = new ByteArrayOutputStream();
												DeflaterOutputStream dOut = new DeflaterOutputStream(bOut);
												dOut.write(payload);
												dOut.close();
												payload = bOut.toByteArray();
											}

											// Encrypt if needed
											// TODO

											// Compute length
											largeSize = payload.length > Short.MAX_VALUE;

											// Build header
											int header = 0;
											if (encrypted)
												header = header | 64;
											if (compressed)
												header = header | 32;
											if (blueBoxed)
												header = header | 16;
											if (largeSize)
												header = header | 8;

											// Write header
											client.getOutputStream().write(header);

											// Write length
											if (largeSize)
												writeInt(client.getOutputStream(), payload.length);
											else
												writeShort(client.getOutputStream(), (short) payload.length);

											// Write payload
											client.getOutputStream().write(payload);
										} catch (IOException e) {
											try {
												client.close();
											} catch (IOException e1) {
											}
											try {
												remoteConn.close();
											} catch (IOException e1) {
											}
											break;
										}
									}
								});
							} catch (IOException e) {
								// Failed to connect
								try {
									client.close();
								} catch (IOException e1) {
								}
							}
						});
					} catch (IOException e) {
						break;
					}
				}
			});

			// Start udp proxy
			srvUdp = new DatagramSocket(localPort);
			AsyncTaskManager.runAsync(() -> {
				while (true) {
					// Read
					try {
						byte[] buf = new byte[20480000];
						DatagramPacket packet = new DatagramPacket(buf, buf.length);
						srvUdp.receive(packet);
						int read = packet.getLength();

						// Log
						byte[] data = packet.getData();

						// Write
						synchronized (writeLock) {
							// Build output
							JsonObject itm = new JsonObject();
							itm.addProperty("time", System.currentTimeMillis());
							itm.addProperty("type", "udp");
							itm.addProperty("host", remoteHost);
							itm.addProperty("port", remotePort);
							itm.addProperty("side", "C->S");
							itm.addProperty("data", Base64.getEncoder().encodeToString(Arrays.copyOf(data, read)));

							// Write
							fOut.write((itm.toString() + "\n\n").getBytes("UTF-8"));
							fOut.flush();
						}

						// Find address
						DatagramSocket client;
						synchronized (clients) {
							client = clients.get((packet.getAddress().toString() + "///" + packet.getPort()));
						}
						if (client == null) {
							// Connect
							client = new DatagramSocket();
							client.connect(InetAddress.getByName(remoteHost), remotePort);
							synchronized (clients) {
								clients.put((packet.getAddress().toString() + "///" + packet.getPort()), client);
							}

							// Start reader
							DatagramSocket cF = client;
							AsyncTaskManager.runAsync(() -> {
								while (true) {
									// Read
									try {
										// Read from client
										byte[] buf2 = new byte[20480000];
										DatagramPacket packet2 = new DatagramPacket(buf2, buf2.length);
										cF.receive(packet);
										int read2 = packet.getLength();

										// Log
										byte[] data2 = packet2.getData();

										// Write
										synchronized (writeLock) {
											// Build output
											JsonObject itm = new JsonObject();
											itm.addProperty("time", System.currentTimeMillis());
											itm.addProperty("type", "udp");
											itm.addProperty("host", remoteHost);
											itm.addProperty("port", remotePort);
											itm.addProperty("side", "S->C");
											itm.addProperty("data",
													Base64.getEncoder().encodeToString(Arrays.copyOf(data2, read2)));

											// Write
											fOut.write((itm.toString() + "\n\n").getBytes("UTF-8"));
											fOut.flush();
										}

										// Send to server
										DatagramPacket outp = new DatagramPacket(packet2.getData(), read2,
												packet.getAddress(), packet.getPort());
										srvUdp.send(outp);
									} catch (IOException e) {
										cF.close();
										synchronized (clients) {
											clients.remove((packet.getAddress().toString() + "///" + packet.getPort()));
										}
										break;
									}
								}
							});
						}

						// Send to client
						DatagramPacket outp = new DatagramPacket(packet.getData(), read);
						client.send(outp);
					} catch (IOException e) {
						break;
					}
				}
			});
		}
	}

	private static void startBinaryProxy(int localPort, String host, int port) throws IOException {
		new BinaryProxy().start(localPort, host, port);
	}

	private static class ProxyProcessor extends HttpPushProcessor {

		private static XmlMapper mapper = new XmlMapper();

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

					// Set response info body
					respInfo.addProperty("responseBody", Base64.getEncoder().encodeToString(respData));

					// Check path
					if (path.equalsIgnoreCase("/ConfigurationWebService.asmx/GetMMOServerInfoWithZone")) {
						// Read to string
						String xml = new String(respData, "UTF-8");
						ServerList serverList = mapper.readValue(xml, ServerList.class);
						for (ObjectNode obj : serverList.servers) {
							String host = obj.get("IP").asText();
							int port = obj.get("PN").asInt();

							// Add to map if needed
							synchronized (binaryProxies) {
								if (!binaryProxies.containsKey("[" + host + "]:" + port)) {
									// Start proxy
									int p = currentPort++;
									startBinaryProxy(p, host, port);

									// Set info
									binaryProxies.put("[" + host + "]:" + port, p);
								}

								// Set
								obj.set("IP", new TextNode("localhost"));
								obj.set("PN", new IntNode(binaryProxies.get("[" + host + "]:" + port)));
							}
						}

						// Write to string
						xml = mapper.writer().withFeatures(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL)
								.withRootName("ArrayOfMMOServerInfo").writeValueAsString(serverList);
						respData = xml.getBytes("UTF-8");
					}

					// Set response
					setResponseContent(respData);
				} catch (IOException e) {
					respInfo.addProperty("responseBody", "");
				}

				// Check path
				if (!disableCredentialSafeties
						&& (path.equalsIgnoreCase("/v3/AuthenticationWebService.asmx/LoginParent")
								|| path.equalsIgnoreCase("/AuthenticationWebService.asmx/LoginChild")
								|| path.equalsIgnoreCase("/MembershipWebService.asmx/GetSubscriptionInfo"))) {
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
				if (!disableCredentialSafeties
						&& (path.equalsIgnoreCase("/v3/AuthenticationWebService.asmx/LoginParent")
								|| path.equalsIgnoreCase("/AuthenticationWebService.asmx/LoginChild")
								|| path.equalsIgnoreCase("/MembershipWebService.asmx/GetSubscriptionInfo"))) {
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
				itm.addProperty("type", "http");
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
	private static boolean disableCredentialSafeties;

	public static void main(String[] args) throws IOException {
		// Init common
		CommonInit.initAll();

		// Create logger
		Logger log = LogManager.getLogger("Sniffer");
		log.info("Setting up sniffer...");

		// Load config
		log.info("Loading configuration...");
		JsonObject config = JsonParser.parseString(Files.readString(Path.of("sniffer.json"))).getAsJsonObject();
		if (config.has("disableCredentialSafeties"))
			disableCredentialSafeties = config.get("disableCredentialSafeties").getAsBoolean();
		if (disableCredentialSafeties) {
			log.warn("!!! WARNING !!!");
			log.warn("");
			log.warn("Credential filtering has been disabled!");
			log.warn(
					"If you do not wish filtering to be disabled, edit config.json and set 'disableCredentialSafeties' to false!");
			log.warn("");
			log.warn("DO NOT SEND THIS SNIFF TO ANYONE YOU DO NOT PERSONALLY TRUST IF THIS OPTION IS ENABLED");
			log.warn("OTHERWISE THEY WILL RECEIVE YOUR LOGIN CREDENTIALS");
			log.warn("");
			log.warn("!!! WARNING !!!");
		}

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

		// Parse manifest
		log.info("Parsing manifest...");
		XmlMapper mapper = new XmlMapper();
		ObjectNode manifest = mapper.readValue(man, ObjectNode.class);
		if (manifest.has("MMOServer")) {
			String sfsHost = manifest.get("MMOServer").asText();
			int sfsPort = manifest.get("MMOServerPort").asInt();

			// Start binary proxy
			log.info("Starting binary proxy...");
			currentPort = sfsPort + 1;
			startBinaryProxy(sfsPort, sfsHost, sfsPort);
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
