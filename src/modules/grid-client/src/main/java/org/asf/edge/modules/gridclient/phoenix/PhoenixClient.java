package org.asf.edge.modules.gridclient.phoenix;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.util.LinkedHashMap;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.modules.gridclient.phoenix.certificate.PhoenixCertificate;
import org.asf.edge.modules.gridclient.phoenix.certificate.ServerCertificatePayload;
import org.asf.edge.modules.gridclient.phoenix.events.ClientClosedEvent;
import org.asf.edge.modules.gridclient.phoenix.events.ClientConnectedEvent;
import org.asf.edge.modules.gridclient.phoenix.events.ClientDisconnectedEvent;
import org.asf.edge.modules.gridclient.phoenix.events.ClientProgramHandshakeEvent;
import org.asf.edge.modules.gridclient.phoenix.events.ClientProgramLateHandshakeEvent;
import org.asf.edge.modules.gridclient.phoenix.exceptions.PhoenixConnectException;
import org.asf.edge.modules.gridclient.phoenix.networking.CorePacketHandler;
import org.asf.edge.modules.gridclient.phoenix.networking.DataReader;
import org.asf.edge.modules.gridclient.phoenix.networking.DataWriter;
import org.asf.edge.modules.gridclient.phoenix.networking.channels.AbstractPacketChannel;
import org.asf.edge.modules.gridclient.phoenix.networking.packets.IPhoenixPacket;
import org.asf.nexus.events.EventBus;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class PhoenixClient {

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	public static final int PHOENIX_PROTOCOL_VERSION = 4;
	private static SecureRandom rnd = new SecureRandom();

	private DisconnectReason disconnectReason;

	private Socket socket;
	private DataReader reader;
	private DataWriter writer;
	private boolean connected;

	private boolean secure;
	private String serverID;

	private long lastPacketSent = 0;
	private EventBus eventBus = EventBus.getInstance().createBus();

	private LinkedHashMap<AbstractPacketChannel, CorePacketHandler> registry = new LinkedHashMap<AbstractPacketChannel, CorePacketHandler>();
	private Logger logger = LogManager.getLogger("Phoenix");

	/**
	 * Retrieves the server ID
	 * 
	 * @return Server ID string or null if in insecure mode
	 */
	public String getServerID() {
		return serverID;
	}

	/**
	 * Checks if the client is in secure mode
	 * 
	 * @return True if in secure mode, false otherwise
	 */
	public boolean isSecureMode() {
		return secure;
	}

	/**
	 * Retrieves the logger instance
	 * 
	 * @return Logger instance
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Retrieves the client event bus
	 * 
	 * @return EventBus instance
	 */
	public EventBus getEventBus() {
		return eventBus;
	}

	/**
	 * Retrieves channels by type
	 * 
	 * @param <T>          Channel type
	 * @param channelClass Channel class
	 * @return AbstractPacketChannel instance or null
	 */
	@SuppressWarnings("unchecked")
	public <T extends AbstractPacketChannel> T getChannel(Class<T> channelClass) {
		for (AbstractPacketChannel ch : registry.keySet()) {
			if (channelClass.isAssignableFrom(ch.getClass()))
				return (T) ch;
		}
		return null;
	}

	/**
	 * Retrieves channels by ID
	 * 
	 * @param cId Channel ID
	 * @return AbstractPacketChannel instance or null
	 */
	public AbstractPacketChannel getChannel(int cId) {
		if (cId >= 0 && cId < registry.size())
			return registry.keySet().toArray(t -> new AbstractPacketChannel[t])[cId];
		return null;
	}

	/**
	 * Registers packet channels
	 * 
	 * @param channel Channel to register
	 */
	public void registerChannel(AbstractPacketChannel channel) {
		// Init
		CorePacketHandler handler = channel.init(this, (ch, packet) -> {
			// Send
			sendPacket(ch, packet);
		});

		// Add
		registry.put(channel, handler);
	}

	/**
	 * Connects the client
	 * 
	 * @param ip   Server host IP
	 * @param port Server port
	 * @param cert Phoenix certificate, null to disable
	 * @throws IOException             If connecting fails
	 * @throws PhoenixConnectException If handshaking fails
	 */
	public void connect(String ip, int port, PhoenixCertificate cert) throws IOException, PhoenixConnectException {
		if (connected)
			throw new IOException("Already connected");

		// Parse
		boolean lessSecure = false;
		if (ip.startsWith("lesssecure:")) {
			lessSecure = true;
			ip = ip.substring("lesssecure:".length());
		}

		// Connect
		logger.debug("Attempting to connect to " + ip + ", port " + port + "...");
		this.socket = new Socket(ip, port);
		writer = new DataWriter(socket.getOutputStream());
		reader = new DataReader(socket.getInputStream());
		disconnectReason = null;

		// Handshake
		logger.debug(
				"Attempting Phoenix networking handshake with protocol version " + PHOENIX_PROTOCOL_VERSION + "..");
		byte[] hello = ("PHOENIX/HELLO/" + PHOENIX_PROTOCOL_VERSION + "/").getBytes("UTF-8");
		byte[] helloSrv = ("PHOENIX/HELLO/SERVER/" + PHOENIX_PROTOCOL_VERSION + "/").getBytes("UTF-8");
		logger.debug("Sending HELLO messsage...");
		socket.getOutputStream().write(hello);
		for (byte b : helloSrv) {
			byte b2 = reader.readRawByte();
			if (b != b2) {
				logger.debug("Received handshake HELLO packet is invalid");
				writer.writeRawByte((byte) 0);
				socket.close();
				throw new PhoenixConnectException("Connection failed: invalid server response during HELLO",
						new DisconnectReason("handshake.failure.invalidhello"));
			}
		}

		// Send endpoint
		logger.debug("Sending connection endpoint....");
		writer.writeString(ip);
		writer.writeInt(port);

		// Set mode to connect
		logger.debug("Sending MODE packet: CONNECT...");
		socket.getOutputStream().write(1);

		// Encryption
		if (cert != null) {
			logger.debug("Reading server certificate from remote...");

			// Read server certificate
			ServerCertificatePayload certificate = ServerCertificatePayload.fromReader(reader);
			logger.debug("Certificate received: " + bytesToHex(certificate.certificate));

			try {
				// Verify certificate
				logger.debug("Verifying server certificate...");
				logger.debug("Verifying server certificate signature...");

				// Init
				Signature sig = Signature.getInstance("Sha512WithRSA");
				sig.initVerify(cert.publicKey);
				sig.update(certificate.certificate);

				// Verify signature
				if (!sig.verify(certificate.signature))
					throw new PhoenixConnectException("Connection failed: invalid server certificate",
							new DisconnectReason("handshake.failure.invalidcertificate"));

				// Verify certificate properties
				logger.debug("Verifying certificate expiry...");
				if (System.currentTimeMillis() > certificate.expiryTimestamp
						|| certificate.expiryTimestamp != cert.expiry) {
					logger.debug("Certificate verification failure!");
					throw new PhoenixConnectException("Connection failed: invalid server certificate",
							new DisconnectReason("handshake.failure.invalidcertificate"));
				}

				logger.debug("Verifying certificate generation timestamp...");
				if (System.currentTimeMillis() < (certificate.generationTimestamp - 120000)
						|| certificate.generationTimestamp != cert.timestamp) {
					logger.debug("Certificate verification failure!");
					throw new PhoenixConnectException("Connection failed: invalid server certificate",
							new DisconnectReason("handshake.failure.invalidcertificate"));
				}

				logger.debug("Verifying current server timestamp...");
				if (System.currentTimeMillis() < (certificate.serverTime - 120000)
						|| System.currentTimeMillis() > certificate.serverTime + 120000) {
					logger.debug("Certificate verification failure!");
					throw new PhoenixConnectException("Connection failed: invalid server certificate",
							new DisconnectReason("handshake.failure.invalidcertificate"));
				}

				// Check addresses
				if (!ip.equals("localhost") && !ip.equals("127.0.0.1") && !ip.equals("::1") && !lessSecure) {
					logger.debug("Verifying address... Checking if its part of the certificate...");

					boolean found = false;
					for (String address : cert.addresses) {
						logger.debug("Trying " + address);
						if (address.equals(ip)) {
							logger.debug("Valid address found: " + address);
							found = true;
							break;
						}
					}
					if (!found) {
						logger.debug("Invalid server address: " + ip + ", not in certificate!");
						throw new PhoenixConnectException("Connection failed: invalid server certificate",
								new DisconnectReason("handshake.failure.invalidcertificate"));
					}
				}

				// Success
				logger.debug("Sending verification success...");
				socket.getOutputStream().write(55);

				// Check if the server agrees
				logger.debug("Verifying remote verification success...");
				int resp = socket.getInputStream().read();
				if (resp != 55) {
					logger.debug("Remote verification failure!");
					writer.writeRawByte((byte) 0);
					socket.close();
					throw new PhoenixConnectException("Connection failed: server sent invalid response",
							new DisconnectReason("handshake.failure.invalidserverresponse"));
				}

				// Okay lets encrypt this connection
				// Generate AES key
				logger.debug("Generating AES encryption key...");
				KeyGenerator gen = KeyGenerator.getInstance("AES", "BC");
				SecretKey key = gen.generateKey();

				// Generate IV
				logger.debug("Generating AES encryption IV...");
				byte[] iv = new SecureRandom().generateSeed(16);

				// Encrypt the key and some data to make it more random
				long time = System.currentTimeMillis();
				int rnd1 = rnd.nextInt();
				int rnd2 = rnd.nextInt();
				logger.debug("Creating key exchange payload...");
				logger.debug("    " + time);
				logger.debug("    [REDACTED]");
				logger.debug("    [REDACTED]");
				logger.debug("    " + certificate.serverTime);
				logger.debug("    " + rnd1);
				logger.debug("    " + rnd2);
				ByteArrayOutputStream strm = new ByteArrayOutputStream();
				DataWriter wr = new DataWriter(strm);
				wr.writeLong(time);
				wr.writeBytes(key.getEncoded());
				wr.writeBytes(iv);
				wr.writeLong(certificate.serverTime);
				wr.writeInt(rnd1);
				wr.writeInt(rnd2);
				byte[] payload = strm.toByteArray();

				// Load cipher
				Cipher encryptCipher = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");
				encryptCipher.init(Cipher.ENCRYPT_MODE, cert.publicKey);
				byte[] enc = encryptCipher.doFinal(payload);
				writer.writeRawBytes(enc);
				logger.debug("Verifying encryption response...");
				resp = reader.readRawByte();
				if (resp != (byte) 243) {
					throw new PhoenixConnectException("Connection failed: server rejected key",
							new DisconnectReason("handshake.failure.encryptfailure"));
				}

				// Assign parameters
				IvParameterSpec i = new IvParameterSpec(iv);
				logger.debug("Building AES ciphers...");
				encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding", "BC");
				encryptCipher.init(Cipher.ENCRYPT_MODE, key, i);
				Cipher decryptCipher = Cipher.getInstance("AES/CFB8/NoPadding", "BC");
				decryptCipher.init(Cipher.DECRYPT_MODE, key, i);

				// Assign writers
				logger.debug("Assigning output writer...");
				reader = new DataReader(new CipherInputStream(reader.getStream(), decryptCipher));
				logger.debug("Assigning input reader...");
				writer = new DataWriter(new CipherOutputStream(writer.getStream(), encryptCipher));

				// Send test
				logger.debug("Sending encryption test message...");
				byte[] testMessage = new byte[127];
				rnd.nextBytes(testMessage);
				writer.writeBytes(testMessage);
				writer.writeBytes(testMessage);
				logger.debug("Test message sent: " + bytesToHex(testMessage) + bytesToHex(testMessage));

				// Read test
				logger.debug("Verifying response message...");
				byte[] t1 = reader.readBytes();
				byte[] t2 = reader.readBytes();
				if (t1.length != t2.length) {
					logger.debug("Encryption failure!");
					throw new PhoenixConnectException("Connection failed: corrupted post-handshake",
							new DisconnectReason("handshake.failure.encryptfailure"));
				}
				for (int i2 = 0; i2 < t1.length; i2++) {
					if (t1[i2] != t2[i2]) {
						logger.debug("Encryption failure!");
						throw new PhoenixConnectException("Connection failed: corrupted post-handshake",
								new DisconnectReason("handshake.failure.encryptfailure"));
					}
				}
				logger.debug("Encrypted Phoenix connection established successfully!");
				secure = true;
				serverID = cert.serverID;
			} catch (PhoenixConnectException e) {
				writer.writeRawByte((byte) 0);
				socket.close();
				throw e;
			} catch (Exception e) {
				logger.error("Error occurred while encrypting the connection", e);
				writer.writeRawByte((byte) 0);
				socket.close();
				throw new PhoenixConnectException("Connection failed: invalid server certificate",
						new DisconnectReason("handshake.failure.invalidcertificate"));
			}

			// Perform program handshake
			if (!attemptProgramHandshake() || reader.readRawByte() != 102) {
				logger.error("Connection failed: program handshake failed");
				writer.writeRawByte((byte) 0);
				socket.close();
				throw new PhoenixConnectException("Connection failed: program handshake failed",
						disconnectReason == null ? new DisconnectReason("handshake.failure.programhandshakefailure")
								: disconnectReason);
			}
		} else {
			// Perform program handshake
			if (attemptProgramHandshake() || socket.getInputStream().read() != 0) {
				logger.error("Connection failed: program handshake failed");
				socket.close();
				throw new PhoenixConnectException("Connection failed: program handshake failed",
						disconnectReason == null ? new DisconnectReason("handshake.failure.programhandshakefailure")
								: disconnectReason);
			}
		}

		// Mark connection as open
		connected = true;

		// Call late handshake
		logger.debug("Calling connection event...");
		ClientProgramLateHandshakeEvent ev = new ClientProgramLateHandshakeEvent(this, reader, writer);
		getEventBus().dispatchEvent(ev);
		logger.debug("Checking connection...");
		if (!isConnected()) {
			logger.trace("Disconnected from remote");
			closeConnection();
			throw new PhoenixConnectException("Connection failed: late handshake failed",
					disconnectReason == null ? new DisconnectReason("handshake.failure.latehandshakefailure")
							: disconnectReason);
		}
		logger.debug("Sending post-handshake completion...");
		writer.writeRawByte((byte) 102);
		logger.debug("Verifying post-handshake...");
		try {
			byte b = reader.readRawByte();
			if (b != 102) {
				// Error
				throw new Exception();
			}
		} catch (Exception e) {
			// Connection ended
			logger.debug("Connection closed by remote from post-handshake");
			closeConnection();
			throw new PhoenixConnectException("Connection failed: late handshake failed",
					disconnectReason == null ? new DisconnectReason("handshake.failure.latehandshakefailure")
							: disconnectReason);
		}

		// Log
		logger.debug("Starting packet handlers...");

		// Start packet handling
		AsyncTaskManager.runAsync(() -> {
			while (isConnected()) {
				int cId;
				int pId;
				try {
					// Read packet
					cId = reader.readInt();
					pId = reader.readInt();
				} catch (Exception e) {
					disconnect("connection.lost");
					break;
				}

				// Handle system channel messages
				if (cId == -1) {

					// Handle
					switch (pId) {

					// Disconnect
					case 0: {
						try {
							// Read request
							DataReader b = new DataReader(new ByteArrayInputStream(reader.readBytes()));
							String reason = b.readString();
							int l = b.readInt();
							String[] args = new String[l];
							for (int i = 0; i < l; i++)
								args[i] = b.readString();

							// Call disconnect
							disconnect(reason, args);
						} catch (IOException e) {
							break;
						}
						break;
					}

					// Ping
					case 1: {
						try {
							DataReader b = new DataReader(new ByteArrayInputStream(reader.readBytes()));
							b.readLong();
						} catch (IOException e) {
							break;
						}
						break;
					}

					}

				} else {
					// Handle packets
					if (connected) {
						try {
							// Get channel
							AbstractPacketChannel channel = getChannel(cId);
							if (channel != null) {
								// Find packet
								IPhoenixPacket def = channel.getPacketDefinition(pId);
								if (def != null && !def.lengthPrefixed()) {
									// Handle unprefixed packet
									if (connected)
										if (!handlePacket(cId, pId, reader)) {
											// Error
											logger.error("Unhandled packet: " + def.getClass().getTypeName()
													+ ", channel type name: " + channel.getClass().getTypeName());
										}
									continue;
								} else if (def != null && def.isSynchronized()) {
									// Read data
									byte[] packet = reader.readBytes();
									DataReader rd = new DataReader(new ByteArrayInputStream(packet));
									if (connected) {
										if (!handlePacket(cId, pId, rd)) {
											logger.error("Unhandled packet: " + def.getClass().getTypeName() + ": ["
													+ bytesToHex(packet) + "], channel type name: "
													+ channel.getClass().getTypeName());
										}
									}
									continue;
								}
							}

							// Read data
							byte[] packet = reader.readBytes();
							DataReader rd = new DataReader(new ByteArrayInputStream(packet));

							// Handle
							AsyncTaskManager.runAsync(() -> {
								if (!handlePacket(cId, pId, rd)) {
									AbstractPacketChannel ch = getChannel(cId);
									if (ch == null)
										logger.error("Unhandled packet: " + cId + ":" + pId + ": [" + bytesToHex(packet)
												+ "]");
									else {
										IPhoenixPacket pkt = ch.getPacketDefinition(pId);
										if (pkt != null)
											logger.error("Unhandled packet: " + pkt.getClass().getTypeName() + ": ["
													+ bytesToHex(packet) + "], channel type name: "
													+ ch.getClass().getTypeName());
										else
											logger.error(
													"Unhandled packet: " + cId + ":" + pId + ": [" + bytesToHex(packet)
															+ "], channel type name: " + ch.getClass().getTypeName());
									}
								}
							});
						} catch (Exception e) {
						}
					}
				}
			}
		});

		// Start pinger
		lastPacketSent = System.currentTimeMillis();
		AsyncTaskManager.runAsync(() -> {
			while (isConnected()) {
				if ((System.currentTimeMillis() - lastPacketSent) > 20000) {
					try {
						// Send ping
						synchronized (writer) {
							// Write packet header
							writer.writeInt(-1);
							writer.writeInt(1);

							// Write packet
							ByteArrayOutputStream bO = new ByteArrayOutputStream();
							DataWriter wr = new DataWriter(bO);
							wr.writeLong(System.currentTimeMillis());
							writer.writeBytes(bO.toByteArray());
							lastPacketSent = System.currentTimeMillis();

							// Flush
							writer.getStream().flush();
						}
					} catch (IOException e) {
						disconnect("connection.lost");
					}
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
		});

		// Call connected
		logger.debug("Calling connection success...");
		getEventBus().dispatchEvent(new ClientConnectedEvent(this));
		logger.debug("Connection successfully established!");
	}

	private boolean handlePacket(int cId, int pId, DataReader reader) {
		try {
			// Get channel
			AbstractPacketChannel channel = getChannel(cId);
			if (channel != null) {
				registry.get(channel).handle(cId, pId, reader);
				return true;
			}
			return false;
		} catch (Exception e) {
			logger.error("Error occured while handling packet: " + cId + ":" + pId, e);
			return true;
		}
	}

	private boolean attemptProgramHandshake() {
		// Attempt handshake
		ClientProgramHandshakeEvent ev = new ClientProgramHandshakeEvent(this, reader, writer);
		getEventBus().dispatchEvent(ev);
		return !ev.hasFailed();
	}

	/**
	 * Checks if the client is connected
	 * 
	 * @return True if connected, false otherwise
	 */
	public boolean isConnected() {
		return connected;
	}

	/**
	 * Disconnects the client
	 */
	public void disconnect() {
		disconnect("disconnect.generic");
	}

	/**
	 * Disconnects the client
	 * 
	 * @param reason     Disconnect reason
	 * @param reasonArgs Disconnect reason arguments
	 */
	public void disconnect(String reason, String... reasonArgs) {
		if (disconnectReason == null) {
			disconnectReason = new DisconnectReason(reason, reasonArgs);
			logger.debug("Disconnected from server: " + reason);
		}
		if (!connected)
			return;
		connected = false;

		// Send disconnect
		try {
			synchronized (writer) {
				// Write packet header
				writer.writeInt(-1);
				writer.writeInt(0);

				// Write packet
				ByteArrayOutputStream bO = new ByteArrayOutputStream();
				DataWriter wr = new DataWriter(bO);
				wr.writeString(reason);
				wr.writeInt(reasonArgs.length);
				for (String arg : reasonArgs)
					wr.writeString(arg);
				writer.writeBytes(bO.toByteArray());
				lastPacketSent = System.currentTimeMillis();
			}
		} catch (IOException e) {
		}

		// Close
		socket = null;
		writer = null;
		reader = null;

		// Fire events
		getEventBus().dispatchEvent(new ClientDisconnectedEvent(this, new DisconnectReason(reason, reasonArgs)));
		getEventBus().dispatchEvent(new ClientClosedEvent(this));
		logger.debug("Disconnected from server: " + reason);
	}

	/**
	 * Closes the connection
	 */
	public void closeConnection() {
		if (!connected)
			return;
		connected = false;
		try {
			socket.close();
		} catch (IOException e) {
		}
		socket = null;
		writer = null;
		reader = null;
		getEventBus().dispatchEvent(new ClientClosedEvent(this));
	}

	private void sendPacket(AbstractPacketChannel channel, IPhoenixPacket packet) throws IOException {
		// Find channel
		int chId = 0;
		for (AbstractPacketChannel c : registry.keySet()) {
			if (channel.getClass().isAssignableFrom(c.getClass())) {
				int pkId = 0;
				for (IPhoenixPacket pk : channel.getPacketDefinitions()) {
					if (pk.getClass().isAssignableFrom(packet.getClass())) {
						sendPacket(chId, pkId, packet);
						break;
					}
					pkId++;
				}
				break;
			}
			chId++;
		}
	}

	private void sendPacket(int chId, int pkId, IPhoenixPacket pk) throws IOException {
		if (!connected)
			throw new IOException("Not connected");

		synchronized (writer) {
			// Write packet header
			try {
				writer.writeInt(chId);
				writer.writeInt(pkId);
			} catch (IOException e) {
				if (connected) {
					// Disconnect
					disconnect("connection.lost");
				}
			}

			// Write packet
			if (pk.lengthPrefixed()) {
				ByteArrayOutputStream bO = new ByteArrayOutputStream();
				DataWriter wr = new DataWriter(bO);
				try {
					pk.build(wr);
				} finally {
					writer.writeBytes(bO.toByteArray());
				}
			} else
				pk.build(writer);

			// Flush
			writer.getStream().flush();
		}
		lastPacketSent = System.currentTimeMillis();
	}

	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

	private static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}

	private String str;

	@Override
	public String toString() {
		if (str != null)
			return str;
		if (isConnected() || socket != null) {
			str = "Network Client: " + socket.getRemoteSocketAddress();
			return str;
		} else
			return "Network Client";
	}
}
