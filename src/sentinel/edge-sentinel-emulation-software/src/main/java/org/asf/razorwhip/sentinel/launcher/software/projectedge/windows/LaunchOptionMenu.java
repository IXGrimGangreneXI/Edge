package org.asf.razorwhip.sentinel.launcher.software.projectedge.windows;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.asf.razorwhip.sentinel.launcher.AssetManager;
import org.asf.razorwhip.sentinel.launcher.software.projectedge.EdgeEmulationSoftware;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.event.ActionEvent;

public class LaunchOptionMenu extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();

	/**
	 * Create the dialog.
	 */
	public LaunchOptionMenu() {
		setTitle("Launch options");
		setResizable(false);
		initialize();
	}

	private void initialize() {
		setBounds(100, 100, 340, 329);
		getContentPane().setLayout(new BorderLayout());
		setModal(true);
		setLocationRelativeTo(null);
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			JPanel panel = new JPanel();
			panel.setPreferredSize(new Dimension(320, 280));
			contentPanel.add(panel);
			panel.setLayout(null);

			JButton btnNewButton = new JButton("Launch normally");
			btnNewButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						EdgeEmulationSoftware.updating = false;
						JsonObject conf = JsonParser.parseString(Files.readString(Path.of("edgelauncher.json")))
								.getAsJsonObject();
						conf.addProperty("launchMode", "normal");
						Files.writeString(Path.of("edgelauncher.json"), conf.toString());
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}
					dispose();
				}
			});
			btnNewButton.setBounds(12, 12, 296, 27);
			panel.add(btnNewButton);

			JButton btnNewButtonL = new JButton("Launch normally (with log)");
			btnNewButtonL.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						EdgeEmulationSoftware.updating = false;
						EdgeEmulationSoftware.showLog = true;
						JsonObject conf = JsonParser.parseString(Files.readString(Path.of("edgelauncher.json")))
								.getAsJsonObject();
						conf.addProperty("launchMode", "normal");
						Files.writeString(Path.of("edgelauncher.json"), conf.toString());
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}
					dispose();
				}
			});
			btnNewButtonL.setBounds(12, 47, 296, 27);
			panel.add(btnNewButtonL);

			JButton btnNewButton_1 = new JButton("Launch client (local server)");
			btnNewButton_1.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						EdgeEmulationSoftware.updating = false;
						JsonObject conf = JsonParser.parseString(Files.readString(Path.of("edgelauncher.json")))
								.getAsJsonObject();
						conf.addProperty("launchMode", "local-client");
						Files.writeString(Path.of("edgelauncher.json"), conf.toString());
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}
					dispose();
				}
			});
			btnNewButton_1.setBounds(12, 83, 296, 27);
			panel.add(btnNewButton_1);

			JButton btnNewButton_2 = new JButton("Launch client (remote server)");
			btnNewButton_2.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					RemoteServerConfigWindow window = new RemoteServerConfigWindow();
					if (window.showDialog())
						dispose();
				}
			});
			btnNewButton_2.setBounds(12, 118, 296, 27);
			panel.add(btnNewButton_2);

			JButton btnNewButton_3 = new JButton("Launch server");
			btnNewButton_3.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						EdgeEmulationSoftware.updating = false;
						JsonObject conf = JsonParser.parseString(Files.readString(Path.of("edgelauncher.json")))
								.getAsJsonObject();
						conf.addProperty("launchMode", "server");
						Files.writeString(Path.of("edgelauncher.json"), conf.toString());
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}
					dispose();
				}
			});
			btnNewButton_3.setBounds(12, 153, 296, 27);
			panel.add(btnNewButton_3);

			JButton btnNewButton_3_1 = new JButton("Select client version");
			btnNewButton_3_1.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						AssetManager.showClientSelector(false);
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}
				}
			});
			btnNewButton_3_1.setBounds(12, 204, 296, 27);
			panel.add(btnNewButton_3_1);

			JButton btnNewButton_3_1_1 = new JButton("More options...");
			btnNewButton_3_1_1.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					AdvancedLaunchOptionMenu window = new AdvancedLaunchOptionMenu();
					if (window.showDialog())
						dispose();
				}
			});
			btnNewButton_3_1_1.setBounds(12, 242, 296, 27);
			panel.add(btnNewButton_3_1_1);
		}
	}
}
