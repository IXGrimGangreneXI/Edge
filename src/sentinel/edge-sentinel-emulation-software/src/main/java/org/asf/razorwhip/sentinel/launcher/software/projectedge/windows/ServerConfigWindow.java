package org.asf.razorwhip.sentinel.launcher.software.projectedge.windows;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

public class ServerConfigWindow extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private boolean wasCancelled = true;
	private JTextField textField_1;
	private JTextField textField_2;
	private JTextField textField_3;
	private JTextField textField_4;
	private JTextField textField_5;
	private JTextField textField_6;
	private JTextField textField_7;
	private JTextField textField_8;
	private JTextField textField_9;
	private JTextField textField_10;
	private JTextField textField_11;
	private JTextField textField_12;
	private JTextField textField_13;
	private JTextField textField_14;
	private JTextField textField_15;

	public boolean showDialog() {
		setVisible(true);
		return !wasCancelled;
	}

	public ServerConfigWindow(JDialog parent) {
		super(parent);
		setLocationRelativeTo(parent);
		initialize();
	}

	/**
	 * Create the dialog.
	 */
	public ServerConfigWindow() {
		setResizable(false);
		initialize();
	}

	private void initialize() {
		setTitle("Server configuration");
		setBounds(100, 100, 536, 578);
		getContentPane().setLayout(new BorderLayout());
		setModal(true);
		setLocationRelativeTo(null);
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			JPanel panel = new JPanel();
			panel.setPreferredSize(new Dimension(520, 525));
			contentPanel.add(panel);
			panel.setLayout(null);

			JLabel lblGameplayEndpoint = new JLabel("Gameplay API server");
			lblGameplayEndpoint.setBounds(12, 12, 496, 17);
			panel.add(lblGameplayEndpoint);

			textField_1 = new JTextField();
			textField_1.setText("0.0.0.0");
			textField_1.setColumns(10);
			textField_1.setBounds(12, 31, 375, 21);
			panel.add(textField_1);

			textField_6 = new JTextField();
			textField_6.setText("5320");
			textField_6.setBounds(394, 31, 114, 21);
			panel.add(textField_6);
			textField_6.setColumns(10);

			JLabel lblCommonApiServer = new JLabel("Common API server");
			lblCommonApiServer.setBounds(12, 64, 496, 17);
			panel.add(lblCommonApiServer);

			textField_2 = new JTextField();
			textField_2.setText("0.0.0.0");
			textField_2.setColumns(10);
			textField_2.setBounds(12, 83, 375, 21);
			panel.add(textField_2);

			textField_7 = new JTextField();
			textField_7.setText("5321");
			textField_7.setBounds(394, 83, 114, 21);
			panel.add(textField_7);
			textField_7.setColumns(10);

			JLabel lblSocialApiServer = new JLabel("Social API server");
			lblSocialApiServer.setBounds(12, 116, 496, 17);
			panel.add(lblSocialApiServer);

			textField_3 = new JTextField();
			textField_3.setText("0.0.0.0");
			textField_3.setToolTipText("");
			textField_3.setColumns(10);
			textField_3.setBounds(12, 135, 375, 21);
			panel.add(textField_3);

			textField_8 = new JTextField();
			textField_8.setText("5322");
			textField_8.setBounds(394, 135, 114, 21);
			panel.add(textField_8);
			textField_8.setColumns(10);

			JLabel lblSmartfoxServers = new JLabel("Smartfox server");
			lblSmartfoxServers.setBounds(12, 168, 496, 17);
			panel.add(lblSmartfoxServers);

			textField_4 = new JTextField();
			textField_4.setText("0.0.0.0");
			textField_4.setColumns(10);
			textField_4.setBounds(12, 187, 375, 21);
			textField_4.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					// Apply
					textField_11.setText(textField_4.getText().equals("0.0.0.0") ? "localhost" : textField_4.getText());
				}
			});
			panel.add(textField_4);

			textField_5 = new JTextField();
			textField_5.setText("5323");
			textField_5.setBounds(394, 187, 114, 21);
			textField_5.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					// Apply
					textField_12.setText(textField_5.getText());
				}
			});
			panel.add(textField_5);
			textField_5.setColumns(10);

			JLabel lblSmartfoxDiscovery = new JLabel("Smartfox discovery");
			lblSmartfoxDiscovery.setBounds(12, 220, 496, 17);
			panel.add(lblSmartfoxDiscovery);

			textField_11 = new JTextField();
			textField_11.setText("localhost");
			textField_11.setColumns(10);
			textField_11.setBounds(12, 239, 375, 21);
			panel.add(textField_11);

			textField_12 = new JTextField();
			textField_12.setText("5323");
			textField_12.setBounds(394, 239, 114, 21);
			panel.add(textField_12);
			textField_12.setColumns(10);

			JLabel lblInternalApi = new JLabel("Internal API server");
			lblInternalApi.setBounds(12, 272, 496, 17);
			panel.add(lblInternalApi);

			textField_13 = new JTextField();
			textField_13.setText("127.0.0.1");
			textField_13.setColumns(10);
			textField_13.setBounds(12, 291, 375, 21);
			textField_13.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					String host = "127.0.0.1";
					if (!textField_13.getText().isBlank())
						host = textField_13.getText();

					// Apply
					textField_15.setText(replaceHost(textField_15.getText(), host));
				}

				private String replaceHost(String url, String host) {
					try {
						URL u = new URL(url);
						return new URL(u.getProtocol(), host, u.getPort(), u.getPath()).toString();
					} catch (IOException e) {
					}
					return url;
				}
			});
			panel.add(textField_13);

			textField_14 = new JTextField();
			textField_14.setText("5324");
			textField_14.setBounds(394, 291, 114, 21);
			textField_14.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					int port = 5324;
					if (!textField_14.getText().isBlank() && textField_14.getText().matches("^[0-9]+$"))
						port = Integer.parseInt(textField_14.getText());

					// Apply
					textField_15.setText(replacePort(textField_15.getText(), port));
				}

				private String replacePort(String url, int port) {
					try {
						URL u = new URL(url);
						return new URL(u.getProtocol(), u.getHost(), port, u.getPath()).toString();
					} catch (IOException e) {
					}
					return url;
				}
			});
			panel.add(textField_14);
			textField_14.setColumns(10);

			JLabel lblDiscoveryURL = new JLabel("Common API uplink URL");
			lblDiscoveryURL.setBounds(12, 324, 496, 17);
			panel.add(lblDiscoveryURL);

			textField_15 = new JTextField();
			textField_15.setText("http://127.0.0.1:5324/");
			textField_15.setColumns(10);
			textField_15.setBounds(12, 343, 496, 21);
			panel.add(textField_15);

			JLabel lblJvm = new JLabel("Additional JVM arguments");
			lblJvm.setBounds(12, 386, 496, 17);
			panel.add(lblJvm);

			textField_9 = new JTextField();
			textField_9.setText("");
			textField_9.setColumns(10);
			textField_9.setBounds(12, 405, 496, 21);
			panel.add(textField_9);

			JLabel lblProg = new JLabel("Program arguments");
			lblProg.setBounds(12, 434, 496, 17);
			panel.add(lblProg);

			textField_10 = new JTextField();
			textField_10.setText("");
			textField_10.setColumns(10);
			textField_10.setBounds(12, 453, 496, 21);
			panel.add(textField_10);

			File edgeConfig = new File("server/server.json");
			File socialSrvJar = new File("server/libs/socialserver.jar");
			JButton btnNewButton = new JButton("Save");
			btnNewButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// Check port
					if (!textField_6.getText().matches("^[0-9]+$")) {
						JOptionPane.showMessageDialog(ServerConfigWindow.this, "Gamplay API port is not valid",
								"Invalid port", JOptionPane.ERROR_MESSAGE);
						return;
					}
					if (!textField_7.getText().matches("^[0-9]+$")) {
						JOptionPane.showMessageDialog(ServerConfigWindow.this, "Common API port is not valid",
								"Invalid port", JOptionPane.ERROR_MESSAGE);
						return;
					}
					if (!textField_8.getText().matches("^[0-9]+$")) {
						JOptionPane.showMessageDialog(ServerConfigWindow.this, "Social API port is not valid",
								"Invalid port", JOptionPane.ERROR_MESSAGE);
						return;
					}
					if (!textField_5.getText().matches("^[0-9]+$")) {
						JOptionPane.showMessageDialog(ServerConfigWindow.this, "Smartfox port is not valid",
								"Invalid port", JOptionPane.ERROR_MESSAGE);
						return;
					}
					if (!textField_12.getText().matches("^[0-9]+$")) {
						JOptionPane.showMessageDialog(ServerConfigWindow.this, "Smartfox discovery port is not valid",
								"Invalid port", JOptionPane.ERROR_MESSAGE);
						return;
					}
					if (!textField_14.getText().matches("^[0-9]+$")) {
						JOptionPane.showMessageDialog(ServerConfigWindow.this, "Internal API port is not valid",
								"Invalid port", JOptionPane.ERROR_MESSAGE);
						return;
					}

					// Save
					try {

						JsonObject configData;
						if (!edgeConfig.exists()) {
							Files.writeString(edgeConfig.toPath(), "{\n" //
									+ "\n" //
									+ "    \"contentServer\": {\n" //
									+ "        \"disabled\": false,\n" // defines if the server is disabled
									+ "        \"listenAddress\": \"0.0.0.0\",\n" // listen address
									+ "        \"listenPort\": 5319,\n" // port to listen on
									+ "        \"contentRequestListenPath\": \"/\",\n" // URI to listen on
									+ "        \"contentDataPath\": \"./data/contentserver/asset-data\",\n" // Content
																											// data path
									+ "        \"allowIndexingAssets\": true,\n" // Defines if indexing is enabled
									+ "\n" //
									+ "        \"serverTestEndpoint\": null,\n" // test endpoint
									+ "        \"fallbackAssetServerEndpoint\": null,\n" // proxy endpoint
									+ "        \"fallbackAssetServerManifestModifications\": {},\n" // proxy
																									// modifications
									+ "\n" //
									+ "        \"storeFallbackAssetDownloads\": false," // downloading fallback to disk
									+ "\n" //
									+ "        \"https\": false,\n" // use https?
									+ "        \"tlsKeystore\": null,\n" // keystore file
									+ "        \"tlsKeystorePassword\": null\n" // keystore password
									+ "    },\n" //
									+ "\n" //
									+ "    \"gameplayApiServer\": {\n" //
									+ "        \"disabled\": false,\n" // defines if the server is disabled
									+ "        \"listenAddress\": \"0.0.0.0\",\n" // listen address
									+ "        \"listenPort\": 5320,\n" // port to listen on
									+ "        \"apiRequestListenPath\": \"/\",\n" // URI to listen on
									+ "\n" //
									+ "        \"https\": false,\n" // use https?
									+ "        \"tlsKeystore\": null,\n" // keystore file
									+ "        \"tlsKeystorePassword\": null\n" // keystore password
									+ "    },\n" //
									+ "\n" //
									+ "    \"commonApiServer\": {\n" //
									+ "        \"disabled\": false,\n" // defines if the server is disabled
									+ "        \"listenAddress\": \"0.0.0.0\",\n" // listen address
									+ "        \"listenPort\": 5321,\n" // port to listen on
									+ "        \"apiRequestListenPath\": \"/\",\n" // URI to listen on
									+ "\n" //
									+ "        \"https\": false,\n" // use https?
									+ "        \"tlsKeystore\": null,\n" // keystore file
									+ "        \"tlsKeystorePassword\": null,\n" // keystore password
									+ "\n" //
									+ "        \"internalListenAddress\": \"127.0.0.1\",\n" // listen address
									+ "        \"internalListenPort\": 5324,\n" // port to listen on
									+ "\n" //
									+ "        \"httpsInternal\": false,\n" // use https?
									+ "        \"tlsKeystoreInternal\": null,\n" // keystore file
									+ "        \"tlsKeystorePasswordInternal\": null\n" // keystore password
									+ "    },\n" //
									+ "\n" //
									+ "    \"mmoServer\": {\n" //
									+ "        \"disabled\": false,\n" // defines if the server is disabled
									+ "        \"listenAddress\": \"0.0.0.0\",\n" // listen address
									+ "        \"listenPort\": 5323,\n" // port to listen on
									+ "\n" //
									+ "        \"discoveryAddress\": \"localhost\",\n" // discovery Address
									+ "        \"discoveryPort\": 5323,\n" // discovery port
									+ "        \"discoveryRootZone\": \"JumpStart\",\n" // MMO root zone
									+ "        \"isBackupServer\": false,\n" // is this a backup MMO server?
									+ "\n" //
									+ "        \"commonApiUplinkURL\": \"http://127.0.0.1:5324/\",\n" // uplink URL
									+ "\n" //
									+ "        \"roomUserLimit\": 30,\n" //
									+ "        \"roomUserLimits\": {\n" //
									+ "            \"HubSchoolDO\": 40" //
									+ "        }\n" //
									+ "    },\n" //
									+ "\n" //
									+ "    \"modules\": {\n" //
									+ "    }\n" //
									+ "\n" //
									+ "}");
						}
						configData = JsonParser.parseString(Files.readString(edgeConfig.toPath())).getAsJsonObject();

						// Apply
						JsonObject gApiJson = configData.get("gameplayApiServer").getAsJsonObject();
						JsonObject cApiJson = configData.get("commonApiServer").getAsJsonObject();
						JsonObject sfs = configData.get("mmoServer").getAsJsonObject();
						gApiJson.addProperty("listenAddress", textField_1.getText());
						cApiJson.addProperty("listenAddress", textField_2.getText());
						cApiJson.addProperty("internalListenAddress", textField_13.getText());
						sfs.addProperty("listenAddress", textField_4.getText());
						gApiJson.addProperty("listenPort", Integer.parseInt(textField_6.getText()));
						cApiJson.addProperty("listenPort", Integer.parseInt(textField_7.getText()));
						cApiJson.addProperty("internalListenPort", Integer.parseInt(textField_14.getText()));
						sfs.addProperty("listenPort", Integer.parseInt(textField_5.getText()));
						sfs.addProperty("discoveryAddress", textField_11.getText());
						sfs.addProperty("discoveryPort", Integer.parseInt(textField_12.getText()));
						sfs.addProperty("commonApiUplinkURL", textField_15.getText());

						// Social server
						if (socialSrvJar.exists()) {
							// Create social service API if not present
							if (!configData.has("socialApiServer")) {
								JsonObject socialApi = new JsonObject();
								socialApi.addProperty("disabled", false);
								socialApi.addProperty("listenAddress", "0.0.0.0");
								socialApi.addProperty("listenPort", 5322);
								socialApi.addProperty("https", false);
								socialApi.add("tlsKeystore", JsonNull.INSTANCE);
								socialApi.add("tlsKeystorePassword", JsonNull.INSTANCE);
								configData.add("socialApiServer", socialApi);
							}
							JsonObject sApiJson = configData.get("socialApiServer").getAsJsonObject();
							sApiJson.addProperty("listenAddress", textField_3.getText());
							sApiJson.addProperty("listenPort", Integer.parseInt(textField_8.getText()));
						}

						// Save
						Files.writeString(edgeConfig.toPath(),
								new GsonBuilder().setPrettyPrinting().create().toJson(configData));

						// Write JVM and program arguments
						JsonObject argsJ = new JsonObject();
						argsJ.addProperty("jvm", textField_9.getText());
						argsJ.addProperty("program", textField_10.getText());
						Files.writeString(Path.of("server/commandline.json"),
								new GsonBuilder().setPrettyPrinting().create().toJson(argsJ));
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}

					// Close
					wasCancelled = false;
					dispose();
				}
			});
			btnNewButton.setBounds(403, 486, 105, 27);
			panel.add(btnNewButton);

			JButton btnCancel = new JButton("Cancel");
			btnCancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});
			btnCancel.setBounds(282, 486, 105, 27);
			panel.add(btnCancel);

			if (edgeConfig.exists()) {
				try {
					JsonObject configData = JsonParser.parseString(Files.readString(edgeConfig.toPath()))
							.getAsJsonObject();

					// Load
					JsonObject gApiJson = configData.get("gameplayApiServer").getAsJsonObject();
					textField_1.setText(gApiJson.get("listenAddress").getAsString());
					textField_6.setText(Integer.toString(gApiJson.get("listenPort").getAsInt()));
					JsonObject cApiJson = configData.get("commonApiServer").getAsJsonObject();
					textField_2.setText(cApiJson.get("listenAddress").getAsString());
					textField_7.setText(Integer.toString(cApiJson.get("listenPort").getAsInt()));
					textField_13.setText(cApiJson.get("internalListenAddress").getAsString());
					textField_14.setText(Integer.toString(cApiJson.get("internalListenPort").getAsInt()));
					if (configData.has("socialApiServer")) {
						JsonObject sApiJson = configData.get("socialApiServer").getAsJsonObject();
						textField_3.setText(sApiJson.get("listenAddress").getAsString());
						textField_8.setText(Integer.toString(sApiJson.get("listenPort").getAsInt()));
					} else if (!socialSrvJar.exists()) {
						lblSocialApiServer.setText("Social API server (unsupported in this Edge version)");
						textField_3.setEnabled(false);
						textField_8.setEnabled(false);
					}
					JsonObject sfs = configData.get("mmoServer").getAsJsonObject();
					textField_4.setText(sfs.get("listenAddress").getAsString());
					textField_5.setText(Integer.toString(sfs.get("listenPort").getAsInt()));
					textField_11.setText(sfs.get("discoveryAddress").getAsString());
					textField_12.setText(Integer.toString(sfs.get("discoveryPort").getAsInt()));
					textField_15.setText(sfs.get("commonApiUplinkURL").getAsString());
				} catch (IOException e1) {
					throw new RuntimeException(e1);
				}
			} else if (!socialSrvJar.exists()) {
				lblSocialApiServer.setText("Social API server (unsupported in this Edge version)");
				textField_3.setEnabled(false);
				textField_8.setEnabled(false);
			}

			if (new File("server/commandline.json").exists()) {
				try {
					JsonObject configData = JsonParser.parseString(Files.readString(Path.of("server/commandline.json")))
							.getAsJsonObject();

					// Load
					textField_9.setText(configData.get("jvm").getAsString());
					textField_10.setText(configData.get("program").getAsString());
				} catch (IOException e1) {
					throw new RuntimeException(e1);
				}
			}
		}
	}
}