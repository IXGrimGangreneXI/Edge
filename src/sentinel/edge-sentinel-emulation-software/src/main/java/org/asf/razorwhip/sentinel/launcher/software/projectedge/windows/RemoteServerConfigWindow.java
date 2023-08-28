package org.asf.razorwhip.sentinel.launcher.software.projectedge.windows;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.asf.razorwhip.sentinel.launcher.software.projectedge.EdgeEmulationSoftware;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class RemoteServerConfigWindow extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private boolean wasCancelled = true;
	private JTextField textField;
	private JTextField textField_1;
	private JTextField textField_2;
	private JTextField textField_3;
	private JTextField textField_4;
	private JTextField textField_5;

	public boolean showDialog() {
		setVisible(true);
		return !wasCancelled;
	}

	public RemoteServerConfigWindow(JDialog parent) {
		super(parent);
		setLocationRelativeTo(parent);
		initialize();
	}

	/**
	 * Create the dialog.
	 */
	public RemoteServerConfigWindow() {
		setResizable(false);
		initialize();
	}

	private void initialize() {
		setTitle("Remote server");
		setBounds(100, 100, 536, 401);
		getContentPane().setLayout(new BorderLayout());
		setModal(true);
		setLocationRelativeTo(null);
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			JPanel panel = new JPanel();
			panel.setPreferredSize(new Dimension(520, 350));
			contentPanel.add(panel);
			panel.setLayout(null);

			JLabel lblHost = new JLabel("Host");
			lblHost.setBounds(12, 12, 60, 17);
			panel.add(lblHost);

			textField = new JTextField();
			textField.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					String host = "localhost";
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
			textField.setBounds(12, 32, 496, 21);
			panel.add(textField);
			textField.setColumns(10);

			JLabel lblGameplayEndpoint = new JLabel("Gameplay API server");
			lblGameplayEndpoint.setBounds(12, 65, 496, 17);
			panel.add(lblGameplayEndpoint);

			textField_1 = new JTextField();
			textField_1.setColumns(10);
			textField_1.setBounds(12, 84, 496, 21);
			panel.add(textField_1);

			JLabel lblCommonApiServer = new JLabel("Common API server");
			lblCommonApiServer.setBounds(12, 117, 496, 17);
			panel.add(lblCommonApiServer);

			textField_2 = new JTextField();
			textField_2.setColumns(10);
			textField_2.setBounds(12, 136, 496, 21);
			panel.add(textField_2);

			JLabel lblSocialApiServer = new JLabel("Social API server");
			lblSocialApiServer.setBounds(12, 169, 496, 17);
			panel.add(lblSocialApiServer);

			textField_3 = new JTextField();
			textField_3.setColumns(10);
			textField_3.setBounds(12, 188, 496, 21);
			panel.add(textField_3);

			JLabel lblSmartfoxServers = new JLabel("Smartfox server");
			lblSmartfoxServers.setBounds(12, 221, 496, 17);
			panel.add(lblSmartfoxServers);

			textField_4 = new JTextField();
			textField_4.setColumns(10);
			textField_4.setBounds(12, 240, 375, 21);
			panel.add(textField_4);

			textField_5 = new JTextField();
			textField_5.setBounds(394, 240, 114, 21);
			panel.add(textField_5);
			textField_5.setColumns(10);

			JButton btnNewButton = new JButton("Launch");
			btnNewButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// Check port
					if (!textField_5.getText().matches("^[0-9]+$")) {
						JOptionPane.showMessageDialog(RemoteServerConfigWindow.this, "Smartfox port is not valid",
								"Invalid port", JOptionPane.ERROR_MESSAGE);
						return;
					}

					// Save
					try {
						JsonObject conf = JsonParser.parseString(Files.readString(Path.of("edgelauncher.json")))
								.getAsJsonObject();
						JsonObject remoteEndpoints = conf.get("remoteEndpoints").getAsJsonObject();
						remoteEndpoints.addProperty("gameplay", textField_1.getText());
						remoteEndpoints.addProperty("common", textField_2.getText());
						remoteEndpoints.addProperty("social", textField_3.getText());
						remoteEndpoints.addProperty("smartfoxHost", textField_4.getText());
						remoteEndpoints.addProperty("smartfoxPort", Integer.parseInt(textField_5.getText()));
						conf.addProperty("launchMode", "remote-client");
						EdgeEmulationSoftware.updating = false;
						Files.writeString(Path.of("edgelauncher.json"), conf.toString());
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}

					// Close
					wasCancelled = false;
					dispose();
				}
			});
			btnNewButton.setBounds(403, 311, 105, 27);
			panel.add(btnNewButton);

			JButton btnCancel = new JButton("Cancel");
			btnCancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});
			btnCancel.setBounds(282, 311, 105, 27);
			panel.add(btnCancel);

			try {
				JsonObject conf = JsonParser.parseString(Files.readString(Path.of("edgelauncher.json")))
						.getAsJsonObject();
				JsonObject remoteEndpoints = conf.get("remoteEndpoints").getAsJsonObject();
				textField.setText(remoteEndpoints.get("smartfoxHost").getAsString());
				textField_1.setText(remoteEndpoints.get("gameplay").getAsString());
				textField_2.setText(remoteEndpoints.get("common").getAsString());
				textField_3.setText(remoteEndpoints.get("social").getAsString());
				textField_4.setText(remoteEndpoints.get("smartfoxHost").getAsString());
				textField_5.setText(Integer.toString(remoteEndpoints.get("smartfoxPort").getAsInt()));
			} catch (IOException e1) {
				throw new RuntimeException(e1);
			}
		}
	}
}
