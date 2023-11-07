package org.asf.razorwhip.sentinel.launcher.software.projectedge.windows;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class LaunchProfileConfigWindow extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private boolean wasCancelled = true;
	private JTextField textField;
	private JTextField textField_1;
	private JTextField textField_2;
	private JTextField textField_3;
	private JTextField textField_4;
	private JTextField textField_5;
	private JTextField textField_6;

	private String profileID;
	private JsonObject profileBeingEdited;

	public boolean showDialog() {
		setVisible(true);
		return !wasCancelled;
	}

	public String resultID() {
		return profileID;
	}

	public LaunchProfileConfigWindow(JDialog parent, JsonObject profileBeingEdited) {
		super(parent);
		this.profileBeingEdited = profileBeingEdited;
		setLocationRelativeTo(parent);
		initialize();
	}

	public LaunchProfileConfigWindow(JDialog parent) {
		super(parent);
		setLocationRelativeTo(parent);
		initialize();
	}

	/**
	 * Create the dialog.
	 */
	public LaunchProfileConfigWindow(JsonObject profileBeingEdited) {
		setResizable(false);
		initialize();
		this.profileBeingEdited = profileBeingEdited;
	}

	/**
	 * Create the dialog.
	 */
	public LaunchProfileConfigWindow() {
		setResizable(false);
		initialize();
	}

	private void initialize() {
		setTitle("Launch profile");
		setBounds(100, 100, 536, 449);
		getContentPane().setLayout(new BorderLayout());
		setModal(true);
		setLocationRelativeTo(null);
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			JPanel panel = new JPanel();
			panel.setPreferredSize(new Dimension(520, 400));
			contentPanel.add(panel);
			panel.setLayout(null);

			JLabel lblName = new JLabel("Profile name");
			lblName.setBounds(12, 12, 197, 17);
			panel.add(lblName);

			textField_6 = new JTextField();
			textField_6.setColumns(10);
			textField_6.setBounds(12, 32, 496, 21);
			panel.add(textField_6);

			JLabel lblHost = new JLabel("Host");
			lblHost.setBounds(12, 75, 60, 17);
			panel.add(lblHost);

			textField = new JTextField();
			textField.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					String host = "127.0.0.1";
					if (!textField.getText().isBlank())
						host = textField.getText();

					// Apply
					textField_1.setText(replaceHost(textField_1.getText(), host));
					textField_2.setText(replaceHost(textField_2.getText(), host));
					textField_3.setText(replaceHost(textField_3.getText(), host));
					textField_4.setText(host);
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
			textField.setBounds(12, 95, 496, 21);
			panel.add(textField);
			textField.setColumns(10);

			JLabel lblGameplayEndpoint = new JLabel("Gameplay API server");
			lblGameplayEndpoint.setBounds(12, 128, 496, 17);
			panel.add(lblGameplayEndpoint);

			textField_1 = new JTextField();
			textField_1.setColumns(10);
			textField_1.setBounds(12, 148, 496, 21);
			panel.add(textField_1);

			JLabel lblCommonApiServer = new JLabel("Common API server");
			lblCommonApiServer.setBounds(12, 181, 496, 17);
			panel.add(lblCommonApiServer);

			textField_2 = new JTextField();
			textField_2.setColumns(10);
			textField_2.setBounds(12, 201, 496, 21);
			panel.add(textField_2);

			JLabel lblSocialApiServer = new JLabel("Social API server");
			lblSocialApiServer.setBounds(12, 234, 496, 17);
			panel.add(lblSocialApiServer);

			textField_3 = new JTextField();
			textField_3.setColumns(10);
			textField_3.setBounds(12, 254, 496, 21);
			panel.add(textField_3);

			JLabel lblSmartfoxServers = new JLabel("Smartfox server");
			lblSmartfoxServers.setBounds(12, 287, 496, 17);
			panel.add(lblSmartfoxServers);

			textField_4 = new JTextField();
			textField_4.setColumns(10);
			textField_4.setBounds(12, 307, 375, 21);
			panel.add(textField_4);

			textField_5 = new JTextField();
			textField_5.setBounds(394, 307, 114, 21);
			panel.add(textField_5);
			textField_5.setColumns(10);

			JButton btnNewButton = new JButton("Save");
			btnNewButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// Check name
					if (textField_6.getText().trim().isEmpty()) {
						JOptionPane.showMessageDialog(LaunchProfileConfigWindow.this,
								"Please choose a server profile name", "Invalid profile name",
								JOptionPane.ERROR_MESSAGE);
						return;
					}

					// Check port
					if (!textField_5.getText().matches("^[0-9]+$")) {
						JOptionPane.showMessageDialog(LaunchProfileConfigWindow.this, "Smartfox port is not valid",
								"Invalid port", JOptionPane.ERROR_MESSAGE);
						return;
					}

					// Save
					try {
						// Load config
						JsonObject conf = JsonParser.parseString(Files.readString(Path.of("edgelauncher.json")))
								.getAsJsonObject();

						// Find profile
						JsonObject profile = new JsonObject();
						JsonObject launchProfiles = new JsonObject();
						if (conf.has("launchProfiles")) {
							// Find profile
							launchProfiles = conf.get("launchProfiles").getAsJsonObject();
							if (profileID != null) {
								// Retrieve profile
								if (launchProfiles.has(profileID)) {
									profile = launchProfiles.get(profileID).getAsJsonObject();
								}
							}
						} else {
							// Add list
							conf.add("launchProfiles", launchProfiles);
						}

						// Generate ID if needed
						if (profileID == null) {
							// Generate ID
							profileID = UUID.randomUUID().toString();
							while (launchProfiles.has(profileID))
								profileID = UUID.randomUUID().toString();

							// Add profile
							launchProfiles.add(profileID, profile);
						}

						// Update profile
						profile.addProperty("profileID", profileID);
						profile.addProperty("profileName", textField_6.getText());
						profile.addProperty("gameplay", textField_1.getText());
						profile.addProperty("common", textField_2.getText());
						profile.addProperty("social", textField_3.getText());
						profile.addProperty("smartfoxHost", textField_4.getText());
						profile.addProperty("smartfoxPort", Integer.parseInt(textField_5.getText()));

						// Save
						Files.writeString(Path.of("edgelauncher.json"), conf.toString());
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}

					// Close
					wasCancelled = false;
					dispose();
				}
			});
			btnNewButton.setBounds(403, 361, 105, 27);
			panel.add(btnNewButton);

			JButton btnCancel = new JButton("Cancel");
			btnCancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});
			btnCancel.setBounds(282, 361, 105, 27);
			panel.add(btnCancel);

			// Load profile
			if (profileBeingEdited != null) {
				// Populate with profile
				profileID = profileBeingEdited.get("profileID").getAsString();
				textField_6.setText(profileBeingEdited.get("profileName").getAsString());
				textField.setText(profileBeingEdited.get("smartfoxHost").getAsString());
				textField_1.setText(profileBeingEdited.get("gameplay").getAsString());
				textField_2.setText(profileBeingEdited.get("common").getAsString());
				textField_3.setText(profileBeingEdited.get("social").getAsString());
				textField_4.setText(profileBeingEdited.get("smartfoxHost").getAsString());
				textField_5.setText(Integer.toString(profileBeingEdited.get("smartfoxPort").getAsInt()));
			} else {
				// Populate with defaults
				textField.setText("127.0.0.1");
				textField_1.setText("http://127.0.0.1:16520/");
				textField_2.setText("http://127.0.0.1:16521/");
				textField_3.setText("http://127.0.0.1:16522/");
				textField_4.setText("127.0.0.1");
				textField_5.setText("16523");
			}
		}
	}
}
