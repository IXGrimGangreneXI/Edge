package org.asf.edge.gameplayapi.tools;

import java.awt.EventQueue;

import javax.swing.JFrame;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultCaret;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.common.CommonInit;
import org.asf.edge.common.http.apihandlerutils.EdgeWebService;
import org.asf.edge.common.util.HttpUpgradeUtil;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.event.ChangeEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.awt.event.ActionEvent;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;

public class AccountDownloader {

	private static AccountDownloader downloadWindowInstance;
	private JFrame frmEdgeAccountDownloader;
	private JTextField boxServer;
	private JTextField boxUsername;
	private JTextField boxPassword;
	private JTextField textField;
	private JCheckBox chckbxGuestAcc;
	private JTextArea textArea = new JTextArea();
	private JProgressBar progressBar;
	private JButton btnNewButton_1;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		CommonInit.initLogging();

		try {
			try {
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
					| UnsupportedLookAndFeelException e1) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e1) {
		}

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					AccountDownloader window = new AccountDownloader();
					downloadWindowInstance = window;
					WindowAppender.setup();
					window.frmEdgeAccountDownloader.setVisible(true);

					// Init
					Logger logger = LogManager.getLogger("Downloader");
					logger.info("Waiting...");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public AccountDownloader() {
		initialize();
	}

	private void downloadData() {
		setValue(0);
		
		// Prepare
		Logger logger = LogManager.getLogger("Downloader");
		logger.info("Verifying settings...");

		// Parse URL
		String url = boxServer.getText();
		if (!url.endsWith("/"))
			url += "/";
		try {
			new URL(url);
		} catch (Exception e) {
			logger.error("Invalid server URL, unable to continue.");
			JOptionPane.showMessageDialog(frmEdgeAccountDownloader,
					"Server URL is not valid, please check the settings", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Verify output
		if (textField.getText().isBlank()) {
			logger.error("Invalid target folder, unable to continue.");
			JOptionPane.showMessageDialog(frmEdgeAccountDownloader, "Please select an output folder", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		File outputDir = new File(textField.getText());
		if (!outputDir.exists()) {
			logger.error("Invalid target folder, unable to continue.");
			JOptionPane.showMessageDialog(frmEdgeAccountDownloader,
					"Invalid output folder, please make sure the folder you entered exists", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Check guest
		logger.info("Processing login details...");
		String token = null;
		String[] guestIDs = null;
		if (chckbxGuestAcc.isSelected()) {
			// Check OS
			String os = "";
			if (System.getProperty("os.name").toLowerCase().contains("win")
					&& !System.getProperty("os.name").toLowerCase().contains("darwin")) { // Windows
				os = "win";
			} else if (System.getProperty("os.name").toLowerCase().contains("darwin")
					|| System.getProperty("os.name").toLowerCase().contains("mac")) { // MacOS
				os = "osx";
			} else if (System.getProperty("os.name").toLowerCase().contains("linux")) {// Linux
				os = "linux";
			} else {
				os = "generic";
			}
			if (!os.equals("win")) {
				// Non-windows
				logger.info("Non-windows system, requesting path to wine prefix...");
				File userReg;
				while (true) {
					if (JOptionPane.showConfirmDialog(frmEdgeAccountDownloader,
							"You are on a non-windows system, if you are running the game via wine, please select the wine prefix folder in the next popup.",
							"More information required", JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.INFORMATION_MESSAGE) != JOptionPane.OK_OPTION)
						return;

					// Show folder selection
					JFileChooser f = new JFileChooser(System.getProperty("user.home"));
					f.setDialogTitle("Select wine prefix folder...");
					f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					f.showSaveDialog(frmEdgeAccountDownloader);
					if (f.getSelectedFile() == null)
						return;

					// Check
					userReg = new File(f.getSelectedFile(), "user.reg");
					if (!userReg.exists()) {
						logger.error("Invalid wine prefix selected");
						if (JOptionPane.showConfirmDialog(frmEdgeAccountDownloader,
								"Invalid wine prefix selected, please select a valid wine prefix", "Error",
								JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE) != JOptionPane.OK_OPTION)
							return;
						continue;
					}
					logger.info("Selected wine prefix: " + f.getSelectedFile());
					break;
				}

				// Find
				logger.info("Parsing registry... Please wait...");
				HashMap<String, HashMap<String, String>> reg;
				try {
					FileInputStream fIn = new FileInputStream(userReg);
					try {
						// Read first line
						String start = readLine(fIn);
						if (!start.equalsIgnoreCase("WINE REGISTRY Version 2"))
							throw new IOException("Incompatible registry header: " + start
									+ ": expected 'WINE REGISTRY Version 2', if the format updated please contact the Edge developers.");

						// Read until first key
						String line;
						while (true) {
							line = readLine(fIn);
							if (line == null || line.startsWith("["))
								break;
						}

						// Parse registry
						reg = new HashMap<String, HashMap<String, String>>();
						while (line != null) {
							// Parse object
							String obj = line.substring(1);
							obj = obj.substring(0, obj.lastIndexOf("]"));

							// Create key set
							HashMap<String, String> keys = new HashMap<String, String>();
							reg.put(obj, keys);

							// Read all keys
							while (line != null) {
								line = readLine(fIn);
								if (line == null)
									break;
								else if (line.isEmpty())
									continue;
								else if (line.startsWith("["))
									break;
								while (line.endsWith("\\")) {
									String ln2 = readLine(fIn);
									if (ln2 == null)
										break;
									while (ln2.startsWith(" "))
										ln2 = ln2.substring(1);
									line = line.substring(0, line.lastIndexOf("\\")) + ln2;
								}

								// Parse
								if (line.startsWith("#") || line.startsWith(";;") || !line.contains("="))
									continue;
								String key = "";
								String val = "";
								boolean isKey = true;
								char[] argarray = line.toCharArray();
								boolean ignoreEquals = false;
								int i = 0;
								for (char c : line.toCharArray()) {
									if (c == '"' && (i == 0 || argarray[i - 1] != '\\')) {
										if (ignoreEquals)
											ignoreEquals = false;
										else
											ignoreEquals = true;
									} else if (c == '=' && isKey && !ignoreEquals
											&& (i == 0 || argarray[i - 1] != '\\')) {
										key = val;
										val = "";
										isKey = false;
									} else if (c != '\\' || (i + 1 < argarray.length && argarray[i + 1] != '"'
											&& (argarray[i + 1] != ' ' || ignoreEquals))) {
										val += c;
									}

									i++;
								}
								keys.put(key, val);
							}
						}
					} finally {
						fIn.close();
					}
				} catch (Exception e) {
					logger.error("Failed to parse wine user registry!", e);
					JOptionPane.showMessageDialog(frmEdgeAccountDownloader,
							"An error occurred while reading the registry file!", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Find SoD
				logger.info("Locating guest ID...");
				if (!reg.containsKey("Software\\\\JumpStart\\\\SoD")) {
					logger.error("Unable to locate guest account information");
					JOptionPane.showMessageDialog(frmEdgeAccountDownloader,
							"Unable to locate guest account details, please make sure you have selected the right wine prefix.",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Load into memory
				HashMap<String, String> sodReg = reg.get("Software\\\\JumpStart\\\\SoD");

				// Find keys
				if (!sodReg.keySet().stream().anyMatch(t -> t.startsWith("UNIQRE_ID_h"))) {
					logger.error("Unable to locate guest account information");
					JOptionPane.showMessageDialog(frmEdgeAccountDownloader,
							"Unable to locate guest account details, please make sure you have selected the right wine prefix.",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Find
				String keyV = sodReg
						.get(sodReg.keySet().stream().filter(t -> t.startsWith("UNIQRE_ID_h")).findFirst().get());
				if (keyV.startsWith("hex:")) {
					String hex = keyV.substring(4);
					keyV = "";
					for (String o : hex.split(",")) {
						int i = Integer.parseInt(o, 16);
						if (i == 0)
							break;
						keyV += (char) i;
					}
				}

				// Build ID map
				String keyVF = keyV;
				guestIDs = EdgeWebService.API_SECRET_MAP.keySet().stream().map(t -> t + keyVF)
						.toArray(t -> new String[t]);
				logger.info("Located potential guest IDs: {}", (Object) guestIDs);
			} else {
				// Windows
				logger.info("Exporting registry key...");
				File r;
				try {
					r = File.createTempFile("regsod-", ".reg");

					// Create request
					ProcessBuilder builder = new ProcessBuilder("reg", "export",
							"HKEY_CURRENT_USER\\Software\\JumpStart\\SoD", r.getCanonicalPath(), "/y");
					if (builder.start().waitFor() == 1) {
						logger.error("Unable to locate guest account information");
						JOptionPane.showMessageDialog(frmEdgeAccountDownloader,
								"Unable to locate guest account details.", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
				} catch (Exception e) {
					logger.error("Unable to create temporary file for guest detection", e);
					JOptionPane.showMessageDialog(frmEdgeAccountDownloader,
							"Unable to create temporary file for guest detection!", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Find
				logger.info("Parsing registry... Please wait...");
				HashMap<String, HashMap<String, String>> reg;
				try {
					FileInputStream fIn = new FileInputStream(r);
					try {
						// Read until first key
						String line;
						while (true) {
							line = readLineUTF16LE(fIn);
							if (line == null || line.startsWith("["))
								break;
						}

						// Parse registry
						reg = new HashMap<String, HashMap<String, String>>();
						while (line != null) {
							// Parse object
							String obj = line.substring(1);
							obj = obj.substring(0, obj.lastIndexOf("]"));

							// Create key set
							HashMap<String, String> keys = new HashMap<String, String>();
							reg.put(obj, keys);

							// Read all keys
							while (line != null) {
								line = readLineUTF16LE(fIn);
								if (line == null)
									break;
								else if (line.isEmpty())
									continue;
								else if (line.startsWith("["))
									break;
								while (line.endsWith("\\")) {
									String ln2 = readLineUTF16LE(fIn);
									if (ln2 == null)
										break;
									while (ln2.startsWith(" "))
										ln2 = ln2.substring(1);
									line = line.substring(0, line.lastIndexOf("\\")) + ln2;
								}

								// Parse
								if (line.startsWith("#") || line.startsWith(";;") || !line.contains("="))
									continue;
								String key = "";
								String val = "";
								boolean isKey = true;
								char[] argarray = line.toCharArray();
								boolean ignoreEquals = false;
								int i = 0;
								for (char c : line.toCharArray()) {
									if (c == '"' && (i == 0 || argarray[i - 1] != '\\')) {
										if (ignoreEquals)
											ignoreEquals = false;
										else
											ignoreEquals = true;
									} else if (c == '=' && isKey && !ignoreEquals
											&& (i == 0 || argarray[i - 1] != '\\')) {
										key = val;
										val = "";
										isKey = false;
									} else if (c != '\\' || (i + 1 < argarray.length && argarray[i + 1] != '"'
											&& (argarray[i + 1] != ' ' || ignoreEquals))) {
										val += c;
									}

									i++;
								}
								keys.put(key, val);
							}
						}
					} finally {
						fIn.close();
						r.delete();
					}
				} catch (Exception e) {
					logger.error("Failed to parse user registry!", e);
					JOptionPane.showMessageDialog(frmEdgeAccountDownloader,
							"An error occurred while reading the registry file!", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Find SoD
				logger.info("Locating guest ID...");
				if (!reg.containsKey("HKEY_CURRENT_USER\\Software\\JumpStart\\SoD")) {
					logger.error("Unable to locate guest account information");
					JOptionPane.showMessageDialog(frmEdgeAccountDownloader, "Unable to locate guest account details.",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Load into memory
				HashMap<String, String> sodReg = reg.get("HKEY_CURRENT_USER\\Software\\JumpStart\\SoD");

				// Find keys
				if (!sodReg.keySet().stream().anyMatch(t -> t.startsWith("UNIQRE_ID_h"))) {
					logger.error("Unable to locate guest account information");
					JOptionPane.showMessageDialog(frmEdgeAccountDownloader, "Unable to locate guest account details.",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Find
				String keyV = sodReg
						.get(sodReg.keySet().stream().filter(t -> t.startsWith("UNIQRE_ID_h")).findFirst().get());
				if (keyV.startsWith("hex:")) {
					String hex = keyV.substring(4);
					keyV = "";
					for (String o : hex.split(",")) {
						int i = Integer.parseInt(o, 16);
						if (i == 0)
							break;
						keyV += (char) i;
					}
				}

				// Build ID map
				String keyVF = keyV;
				guestIDs = EdgeWebService.API_SECRET_MAP.keySet().stream().map(t -> t + keyVF)
						.toArray(t -> new String[t]);
				logger.info("Located potential guest IDs: {}", (Object) guestIDs);
			}

			// Check guests
			if (guestIDs.length == 0) {
				logger.error("Unable to locate guest account information");
				JOptionPane.showMessageDialog(frmEdgeAccountDownloader, "Unable to locate guest account details.",
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			// Contact server
			logger.info("Contacting server...");
			HashMap<String, GuestData> guests = new HashMap<String, GuestData>();
			for (String id : guestIDs) {
				// Build URL
				logger.info("Trying to authenticate as guest " + id);
				String u = url + "AuthenticateGuest";
				try {
					// Make request
					URL u2 = new URL(u);
					HttpURLConnection conn = (HttpURLConnection) u2.openConnection();
					conn.setRequestMethod("POST");
					JsonObject req = new JsonObject();
					req.addProperty("guestID", id);
					conn.setDoOutput(true);
					conn.getOutputStream().write(req.toString().getBytes("UTF-8"));

					// Get response
					if (conn.getResponseCode() < 400) {
						JsonObject resp = JsonParser
								.parseString(new String(conn.getInputStream().readAllBytes(), "UTF-8"))
								.getAsJsonObject();
						if (resp.has("token")) {
							GuestData obj = new GuestData();
							obj.id = id;
							obj.token = resp.get("token").getAsString();
							obj.response = resp;
							guests.put(id, obj);
							logger.info("Authenticated successfully");
						} else
							logger.info("Failed to authenticate");
					} else
						logger.info("Failed to authenticate");
				} catch (IOException e) {
					logger.info("Failed to authenticate", e);
					JOptionPane.showMessageDialog(frmEdgeAccountDownloader,
							"Authentication failure: failed to contact the server", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
			}

			// Check responses
			if (guests.size() == 0) {
				logger.error("Unable to locate guest account information");
				JOptionPane.showMessageDialog(frmEdgeAccountDownloader, "Unable to locate guest account details.",
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			} else if (guests.size() == 1) {
				token = guests.values().stream().findFirst().get().token;
			} else {
				Object res = JOptionPane.showInputDialog(frmEdgeAccountDownloader, "Please select a guest account",
						"Guest selection", JOptionPane.QUESTION_MESSAGE, null,
						guests.values().toArray(t -> new Object[t]), "");
				if (res == null)
					return;
				token = ((GuestData) res).token;
			}

			// Check result
			if (token == null) {
				logger.error("Unable to locate guest account information");
				JOptionPane.showMessageDialog(frmEdgeAccountDownloader, "Unable to locate guest account details.",
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			logger.info("Guest account found!");
		} else {
			// Normal login
			logger.info("Authenticating...");
			String u = url + "Authenticate";
			try {
				// Make request
				URL u2 = new URL(u);
				HttpURLConnection conn = (HttpURLConnection) u2.openConnection();
				conn.setRequestMethod("POST");
				JsonObject req = new JsonObject();
				req.addProperty("username", boxUsername.getText());
				req.addProperty("password", boxPassword.getText());
				conn.setDoOutput(true);
				conn.getOutputStream().write(req.toString().getBytes("UTF-8"));

				// Get response
				if (conn.getResponseCode() >= 400) {
					JsonObject resp = JsonParser.parseString(new String(conn.getErrorStream().readAllBytes(), "UTF-8"))
							.getAsJsonObject();
					String errorMessage = resp.get("error").getAsString();
					if (errorMessage.equalsIgnoreCase("invalid_credentials"))
						errorMessage = "Credentials are invalid";
					logger.info("Failed to authenticate: " + errorMessage);
					JOptionPane.showMessageDialog(frmEdgeAccountDownloader, "Authentication failure: " + errorMessage,
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				} else {
					JsonObject resp = JsonParser.parseString(new String(conn.getInputStream().readAllBytes(), "UTF-8"))
							.getAsJsonObject();
					token = resp.get("token").getAsString();
					logger.info(
							"Authenticated successfully, logged in as " + resp.get("accountUsername").getAsString());
				}
			} catch (IOException e) {
				logger.info("Failed to authenticate", e);
				JOptionPane.showMessageDialog(frmEdgeAccountDownloader,
						"Authentication failure: failed to contact the server", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		// Perform download
		logger.info("Contacting servers... Preparing to download...");
		String u = url + "DownloadAccount";
		try {
			// Open connection
			Socket conn = HttpUpgradeUtil.upgradeRequest(u, "POST", null, -1,
					Map.of("X-Request-ID", UUID.randomUUID().toString(), "Authorization", "Bearer " + token),
					new HashMap<String, String>(), "EDGEBINPROT/EDGESERVICES/DOWNLOADACCOUNT",
					"EDGEBINPROT/EDGESERVICES/DOWNLOADACCOUNT");

			// Download headers
			logger.info("Downloading headers...");
			String accID = readString(conn.getInputStream());
			String username = readString(conn.getInputStream());
			String email = null;
			if (readBoolean(conn.getInputStream()))
				email = readString(conn.getInputStream());
			long lastLogin = readLong(conn.getInputStream());
			long registerTime = readLong(conn.getInputStream());
			boolean isGuest = readBoolean(conn.getInputStream());
			boolean multiplayerEnabled = readBoolean(conn.getInputStream());
			boolean chatEnabled = readBoolean(conn.getInputStream());
			boolean strictChatEnabled = readBoolean(conn.getInputStream());

			// Indexing
			logger.info("Server is indexing save data... This may take a while...");
			long dataSize = readLong(conn.getInputStream());
			setMax((int) dataSize);

			// Prepare output
			logger.info("Preparing output...");
			FileOutputStream fO = new FileOutputStream(new File(outputDir, "account-" + accID + ".spf"));
			ZipOutputStream output = new ZipOutputStream(fO);
			try {
				// Create header
				output.putNextEntry(new ZipEntry("payloadinfo"));
				output.write("Type: resource\nResource-Target: server\nResource-Target-Path: accountimports"
						.getBytes("UTF-8"));
				output.closeEntry();

				// Create folder
				output.putNextEntry(new ZipEntry("payloaddata/"));
				output.closeEntry();

				// Create file
				output.putNextEntry(new ZipEntry("payloaddata/account-" + accID + ".ead"));

				// Write headers
				logger.info("Writing headers...");
				writeString(output, accID);
				writeString(output, username);
				writeBoolean(output, email != null);
				if (email != null)
					writeString(output, email);
				writeLong(output, lastLogin);
				writeLong(output, registerTime);
				writeBoolean(output, isGuest);
				writeBoolean(output, multiplayerEnabled);
				writeBoolean(output, chatEnabled);
				writeBoolean(output, strictChatEnabled);

				// Write index length
				writeLong(output, dataSize);

				// Start reading
				logger.info("Downloading account data containers...");
				downloadDataContainer(logger, conn.getInputStream(), output, "/");

				// Read saves
				int saveCount = readInt(conn.getInputStream());
				writeInt(output, saveCount);
				logger.info("Downloading " + saveCount + " save(s)...");
				for (int i = 0; i < saveCount; i++) {
					// Read headers
					String id = readString(conn.getInputStream());
					String name = readString(conn.getInputStream());
					long creationTime = readLong(conn.getInputStream());
					logger.info("Downloading save: " + name);

					// Write headers
					writeString(output, id);
					writeString(output, name);
					writeLong(output, creationTime);

					// Download data
					downloadDataContainer(logger, conn.getInputStream(), output, "/");

					// Increase progress
					increaseProgress();
				}

				// Done
				logger.info("");
				logger.info("");
				logger.info("Finished downloading account data!");
				logger.info("Downloaded to: " + new File(outputDir, "account-" + accID + ".spf"));
			} finally {
				output.close();
				fO.close();
			}
		} catch (Exception e) {
			logger.info("An error occurred while downloading account data", e);
			JOptionPane.showMessageDialog(frmEdgeAccountDownloader, "An error occurred while downloading account data!",
					"Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
	}

	private void downloadDataContainer(Logger logger, InputStream input, OutputStream output, String pth)
			throws IOException {
		logger.info("Downloading data container: " + pth);

		// Read keys
		int length = readInt(input);
		writeInt(output, length);
		for (int i = 0; i < length; i++) {
			// Read key
			String key = readString(input);
			logger.info("Downloading data key: " + (pth.equals("/") ? "/" + key : pth + "/" + key));

			// Read element
			boolean nonNull = readBoolean(input);
			String data = null;
			if (nonNull)
				data = readString(input);

			// Write
			writeString(output, key);
			writeBoolean(output, nonNull);
			if (nonNull)
				writeString(output, data);

			// Increase
			increaseProgress();
		}

		// Read containers
		length = readInt(input);
		writeInt(output, length);
		for (int i = 0; i < length; i++) {
			// Read key
			String key = readString(input);

			// Write key
			writeString(output, key);

			// Read and write container
			downloadDataContainer(logger, input, output, pth.equals("/") ? "/" + key : pth + "/" + key);

			// Increase
			increaseProgress();
		}
	}

	private boolean readBoolean(InputStream strm) throws IOException {
		return strm.read() == 1;
	}

	private String readString(InputStream strm) throws IOException {
		return new String(readByteArray(strm), "UTF-8");
	}

	private int readInt(InputStream strm) throws IOException {
		return ByteBuffer.wrap(strm.readNBytes(4)).getInt();
	}

	private long readLong(InputStream strm) throws IOException {
		return ByteBuffer.wrap(strm.readNBytes(8)).getLong();
	}

	private byte[] readByteArray(InputStream strm) throws IOException {
		return strm.readNBytes(readInt(strm));
	}

	private void writeInt(OutputStream strm, int val) throws IOException {
		strm.write(ByteBuffer.allocate(4).putInt(val).array());
	}

	private void writeLong(OutputStream strm, long val) throws IOException {
		strm.write(ByteBuffer.allocate(8).putLong(val).array());
	}

	private void writeString(OutputStream strm, String str) throws IOException {
		byte[] data = str.getBytes("UTF-8");
		writeByteArray(strm, data);
	}

	private void writeByteArray(OutputStream strm, byte[] data) throws IOException {
		writeInt(strm, data.length);
		strm.write(data);
	}

	private void writeBoolean(OutputStream strm, boolean b) throws IOException {
		strm.write(b ? 1 : 0);
	}

	private class GuestData {
		public String token;
		public String id;
		public JsonObject response;

		@Override
		public String toString() {
			String str = "Last login: ";
			str += new Date(response.get("lastLoginTime").getAsLong()).toString();
			str += " (" + id + ")";
			return str;
		}
	}

	private String readLine(FileInputStream fIn) throws IOException {
		String buffer = "";
		while (true) {
			int i = fIn.read();
			if (i == -1)
				return null;
			char ch = (char) i;
			if (ch == '\r')
				continue;
			else if (ch == '\n')
				break;
			else
				buffer += ch;
		}
		return buffer;
	}

	private String readLineUTF16LE(FileInputStream fIn) throws IOException {
		String buffer = "";
		while (true) {
			int i = fIn.read();
			if (i == -1)
				return null;
			char ch = (char) new String(new byte[] { (byte) i, (byte) fIn.read() }, "UTF-16LE").charAt(0);
			if (ch == '\r')
				continue;
			else if (ch == '\n')
				break;
			else
				buffer += ch;
		}
		return buffer;
	}

	/**
	 * Initialize the contents of the
	 */
	private void initialize() {
		frmEdgeAccountDownloader = new JFrame();
		frmEdgeAccountDownloader.setResizable(false);
		frmEdgeAccountDownloader.setTitle("Edge Account Downloader");
		frmEdgeAccountDownloader.setBounds(100, 100, 630, 707);
		frmEdgeAccountDownloader.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmEdgeAccountDownloader.getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		frmEdgeAccountDownloader.setLocationRelativeTo(null);

		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(600, 650));
		frmEdgeAccountDownloader.getContentPane().add(panel);
		panel.setLayout(null);

		JButton btnNewButton = new JButton("Download account data");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boxServer.setEnabled(false);
				chckbxGuestAcc.setEnabled(false);
				if (!chckbxGuestAcc.isSelected()) {
					boxUsername.setEnabled(false);
					boxPassword.setEnabled(false);
				}
				textField.setEnabled(false);
				textField.setEnabled(false);
				btnNewButton_1.setEnabled(false);
				btnNewButton.setEnabled(false);
				AsyncTaskManager.runAsync(() -> {
					downloadData();

					// Re-enable boxes
					SwingUtilities.invokeLater(() -> {
						boxServer.setEnabled(true);
						if (!chckbxGuestAcc.isSelected()) {
							boxUsername.setEnabled(true);
							boxPassword.setEnabled(true);
						}
						chckbxGuestAcc.setEnabled(true);
						textField.setEnabled(true);
						textField.setEnabled(true);
						btnNewButton_1.setEnabled(true);
						btnNewButton.setEnabled(true);
					});
				});
			}
		});
		btnNewButton.setBounds(12, 605, 576, 45);
		panel.add(btnNewButton);

		JLabel lblNewLabel = new JLabel("Server");
		lblNewLabel.setBounds(12, 12, 576, 17);
		panel.add(lblNewLabel);

		boxServer = new JTextField();
		boxServer.setText("https://aerialworks.ddns.net:5320/API/ProjectEdge/");
		boxServer.setBounds(12, 32, 576, 21);
		panel.add(boxServer);
		boxServer.setColumns(10);

		JLabel lblUsername = new JLabel("Username");
		lblUsername.setBounds(12, 81, 576, 17);
		panel.add(lblUsername);

		boxUsername = new JTextField();
		boxUsername.setBounds(12, 99, 576, 21);
		panel.add(boxUsername);
		boxUsername.setColumns(10);

		JLabel lblPassword = new JLabel("Password");
		lblPassword.setBounds(12, 132, 576, 17);
		panel.add(lblPassword);

		boxPassword = new JPasswordField();
		boxPassword.setColumns(10);
		boxPassword.setBounds(12, 150, 576, 21);
		panel.add(boxPassword);

		chckbxGuestAcc = new JCheckBox("Use guest account");
		chckbxGuestAcc.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (chckbxGuestAcc.isEnabled()) {
					boxUsername.setEnabled(!chckbxGuestAcc.isSelected());
					boxPassword.setEnabled(!chckbxGuestAcc.isSelected());
				}
			}
		});
		chckbxGuestAcc.setBounds(10, 178, 584, 25);
		panel.add(chckbxGuestAcc);

		JLabel lblTargetFolder = new JLabel("Target folder");
		lblTargetFolder.setBounds(12, 223, 576, 17);
		panel.add(lblTargetFolder);

		textField = new JTextField();
		textField.setColumns(10);
		textField.setBounds(12, 242, 462, 32);
		panel.add(textField);

		btnNewButton_1 = new JButton("Browse...");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser f = new JFileChooser(textField.getText());
				f.setDialogTitle("Select target folder...");
				f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				f.showSaveDialog(frmEdgeAccountDownloader);
				if (f.getSelectedFile() != null)
					textField.setText(f.getSelectedFile().getAbsolutePath());
			}
		});
		btnNewButton_1.setBounds(483, 242, 105, 32);
		panel.add(btnNewButton_1);

		JLabel lblProgress = new JLabel("Progress");
		lblProgress.setBounds(12, 297, 60, 17);
		panel.add(lblProgress);

		progressBar = new JProgressBar();
		progressBar.setBounds(12, 315, 576, 21);
		panel.add(progressBar);

		textArea.setFont(new Font("Liberation Mono", Font.PLAIN, 12));

		textArea.setEditable(false);
		JScrollPane pane = new JScrollPane(textArea);
		pane.setLocation(12, 341);
		pane.setSize(new Dimension(576, 230));
		pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		pane.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));

		DefaultCaret caret = (DefaultCaret) textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		panel.add(pane);
	}

	public void log(String message) {
		String log = textArea.getText() + message;
		while (log.split("\n").length >= 200)
			log = log.substring(log.indexOf("\n") + 1);

		textArea.setText(log + "\n");
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}

	public void addMax(int max) {
		SwingUtilities.invokeLater(() -> {
			progressBar.setMaximum(progressBar.getMaximum() + max);
		});
	}

	public void setMax(int max) {
		SwingUtilities.invokeLater(() -> {
			progressBar.setMaximum(max);
		});
	}

	public void setValue(int val) {
		SwingUtilities.invokeLater(() -> {
			progressBar.setValue(val);
		});
	}

	public int getMax() {
		return progressBar.getMaximum();
	}

	public int getValue() {
		return progressBar.getValue();
	}

	public void increaseProgress() {
		SwingUtilities.invokeLater(() -> {
			if (progressBar.getValue() + 1 > progressBar.getMaximum())
				return;
			progressBar.setValue(progressBar.getValue() + 1);
		});
	}

	public void fatalError(String msg) {
		JOptionPane.showMessageDialog(frmEdgeAccountDownloader, "A fatal error occured:\n" + msg, "Fatal Error",
				JOptionPane.ERROR_MESSAGE);
	}

	public static class WindowAppender extends AbstractAppender {

		@SuppressWarnings("deprecation")
		protected WindowAppender(PatternLayout layout) {
			super("Window Appender", null, layout);
			start();
		}

		@Override
		public void append(LogEvent event) {
			if (downloadWindowInstance == null)
				return;
			if (!event.getLevel().isInRange(Level.FATAL, Level.INFO))
				return;
			String message = new String(getLayout().toByteArray(event));
			if (message.endsWith("\n"))
				message = message.substring(0, message.length() - 1);

			if (message.contains("\n"))
				for (String ln : message.split("\n"))
					downloadWindowInstance.log(ln);
			else
				downloadWindowInstance.log(message);
		}

		public static void setup() {
			final LoggerContext context = LoggerContext.getContext(false);
			final Configuration config = context.getConfiguration();
			final PatternLayout layout = PatternLayout.createDefaultLayout();
			final Appender appender = new WindowAppender(layout);
			config.addAppender(appender);
			updateLoggers(appender, context.getConfiguration());
		}

		private static void updateLoggers(final Appender appender, final Configuration config) {
			final Level level = null;
			final Filter filter = null;
			for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
				loggerConfig.addAppender(appender, level, filter);
			}
			config.getRootLogger().addAppender(appender, level, filter);
		}

	}
}
