package org.asf.razorwhip.sentinel.launcher.software.projectedge.windows;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import org.asf.razorwhip.sentinel.launcher.AssetManager;
import org.asf.razorwhip.sentinel.launcher.PayloadManager;
import org.asf.razorwhip.sentinel.launcher.experiments.SentinelExperimentManager;

import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.awt.event.ActionEvent;

public class AdvancedLaunchOptionMenu extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private boolean wasCancelled = true;

	public boolean showDialog() {
		setVisible(true);
		return !wasCancelled;
	}

	public AdvancedLaunchOptionMenu(JDialog parent) {
		super(parent);
		setTitle("Launch options");
		setResizable(false);
		initialize();
		setLocationRelativeTo(parent);
	}

	/**
	 * Create the dialog.
	 */
	public AdvancedLaunchOptionMenu() {
		setTitle("Launch options");
		setResizable(false);
		initialize();
	}

	private void initialize() {
		setBounds(100, 100, 340, 408);
		getContentPane().setLayout(new BorderLayout());
		setModal(true);
		setLocationRelativeTo(null);
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			JPanel panel = new JPanel();
			panel.setPreferredSize(new Dimension(320, 360));
			contentPanel.add(panel);
			panel.setLayout(null);

			JButton btnNewButton = new JButton("Open client version manager");
			btnNewButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						AssetManager.showVersionManager(false);
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}
				}
			});
			btnNewButton.setBounds(12, 12, 296, 27);
			btnNewButton.setEnabled(AssetManager.isAssetOnlineManagementAvailable());
			panel.add(btnNewButton);

			JButton btnNewButton_1 = new JButton("Open payload manager");
			btnNewButton_1.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						PayloadManager.showPayloadManagementWindow();
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}
				}
			});
			btnNewButton_1.setBounds(12, 47, 296, 27);
			panel.add(btnNewButton_1);

			JButton btnNewButton_2 = new JButton("Open server configuration");
			btnNewButton_2.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ServerConfigWindow window = new ServerConfigWindow();
					window.showDialog();
				}
			});
			btnNewButton_2.setBounds(12, 83, 296, 27);
			panel.add(btnNewButton_2);

			JButton btnNewButton_3_2 = new JButton("Save data transfer (WIP)");
			btnNewButton_3_2.setEnabled(false);
			btnNewButton_3_2.setBounds(12, 118, 296, 27);
			panel.add(btnNewButton_3_2);

			JButton btnNewButton_3 = new JButton("Manual game descriptor update (WIP)");
			btnNewButton_3.setEnabled(false);
			btnNewButton_3.setBounds(12, 153, 296, 27);
			panel.add(btnNewButton_3);

			JButton btnNewButton_3_1 = new JButton("Manual emulation software update (WIP)");
			btnNewButton_3_1.setEnabled(false);
			btnNewButton_3_1.setBounds(12, 188, 296, 27);
			panel.add(btnNewButton_3_1);

			JButton btnNewButton_3_3 = new JButton("Experimental features");
			btnNewButton_3_3.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					SentinelExperimentManager.showExperimentManagerWindow();
				}
			});
			btnNewButton_3_3.setBounds(12, 223, 296, 27);
			panel.add(btnNewButton_3_3);

			JButton btnOpenPlayerLog = new JButton("Open Player.log");
			btnOpenPlayerLog.setBounds(12, 258, 296, 27);
			btnOpenPlayerLog.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						if (System.getProperty("os.name").toLowerCase().contains("windows"))
							Desktop.getDesktop().open(new File(System.getenv("APPDATA") +"/../LocalLow/JumpStart/SoD/Player.log"));
						else
							Desktop.getDesktop().open(new File("wineprefix/drive_c/users/" + System.getProperty("user.name") + "/AppData/LocalLow/JumpStart/SoD/Player.log"));
					} catch (IOException ex) {
						JOptionPane.showMessageDialog(panel,
								"Player.log could not be opened.",
								"Error",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			});
			panel.add(btnOpenPlayerLog);

			JButton btnNewButton_3_1_1 = new JButton("Back");
			btnNewButton_3_1_1.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});
			btnNewButton_3_1_1.setBounds(12, 321, 296, 27);
			panel.add(btnNewButton_3_1_1);
		}
	}
}
