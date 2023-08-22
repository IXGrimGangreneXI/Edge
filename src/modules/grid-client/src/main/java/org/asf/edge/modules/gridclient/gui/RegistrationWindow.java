package org.asf.edge.modules.gridclient.gui;

import javax.swing.JFrame;
import java.awt.FlowLayout;
import javax.swing.JPanel;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;

import java.awt.event.ActionListener;
import java.util.function.BiFunction;
import java.awt.event.ActionEvent;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.asf.connective.tasks.AsyncTaskManager;

import java.awt.Font;

public class RegistrationWindow {

	public JDialog frmRegister;
	private JTextField usernameField;
	private JTextField passwordField;
	private JTextField passwordField2;
	private JLabel lbl;

	private RegistrationResult result;
	private BiFunction<String, String, RegistrationResult> registrationCallback;

	public static class RegistrationResult {
		public String username;
		public String refreshToken;

		public String errorMessage;
		public boolean success;
	}

	/**
	 * Create the application.
	 */
	public RegistrationWindow() {
		initialize();
	}

	public RegistrationResult show(JFrame parent, BiFunction<String, String, RegistrationResult> registrationCallback) {
		frmRegister.dispose();
		frmRegister = new JDialog();
		initialize();
		frmRegister.setModal(true);
		frmRegister.setLocationRelativeTo(null);
		this.registrationCallback = registrationCallback;
		frmRegister.setLocationRelativeTo(parent);
		frmRegister.setVisible(true);
		return result;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmRegister = new JDialog();
		frmRegister.setTitle("Grid account registration");
		frmRegister.setBounds(100, 100, 537, 350);
		frmRegister.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		frmRegister.getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		frmRegister.setLocationRelativeTo(null);
		frmRegister.setResizable(false);

		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(500, 300));
		frmRegister.getContentPane().add(panel);
		panel.setLayout(null);

		lbl = new JLabel("Welcome to account registration for the Multiplayer Grid!");
		lbl.setFont(new Font("Dialog", Font.BOLD, 14));
		lbl.setHorizontalAlignment(SwingConstants.CENTER);
		lbl.setBounds(12, 12, 476, 35);
		panel.add(lbl);

		JLabel lblUser = new JLabel("Grid account username");
		lblUser.setBounds(12, 81, 476, 17);
		panel.add(lblUser);

		usernameField = new JTextField();
		usernameField.setBounds(12, 99, 476, 21);
		panel.add(usernameField);

		JLabel lblPass = new JLabel("Grid account password");
		lblPass.setBounds(12, 130, 476, 17);
		panel.add(lblPass);

		passwordField = new JPasswordField();
		passwordField.setBounds(12, 148, 476, 21);
		panel.add(passwordField);

		JLabel lblPassR = new JLabel("Repeat password");
		lblPassR.setBounds(12, 179, 476, 17);
		panel.add(lblPassR);

		passwordField2 = new JPasswordField();
		passwordField2.setBounds(12, 197, 476, 21);
		panel.add(passwordField2);

		JButton btnNewButton = new JButton("Register");
		passwordField.addActionListener(new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				btnNewButton.doClick();
			}
		});
		passwordField2.addActionListener(new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				btnNewButton.doClick();
			}
		});
		usernameField.addActionListener(new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				btnNewButton.doClick();
			}
		});
		btnNewButton.setBounds(346, 261, 142, 27);
		panel.add(btnNewButton);

		JButton btnNewButton_1 = new JButton("Cancel");
		btnNewButton_1.setBounds(229, 261, 105, 27);
		panel.add(btnNewButton_1);

		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frmRegister.dispose();
			}
		});
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Check
				if (!passwordField2.getText().equals(passwordField.getText())) {
					JOptionPane.showMessageDialog(frmRegister, "Passwords do not match!", "Registration failure",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Register
				usernameField.setEnabled(false);
				passwordField.setEnabled(false);
				passwordField2.setEnabled(false);
				btnNewButton.setEnabled(false);
				btnNewButton.setText("Registering...");
				btnNewButton_1.setEnabled(false);
				AsyncTaskManager.runAsync(() -> {
					// Set result
					RegistrationResult res = registrationCallback.apply(usernameField.getText(),
							passwordField.getText());
					if (!res.success) {
						SwingUtilities.invokeLater(() -> {
							btnNewButton.setText("Register");
							usernameField.setEnabled(true);
							passwordField.setEnabled(true);
							passwordField2.setEnabled(true);
							btnNewButton.setEnabled(true);
							btnNewButton_1.setEnabled(true);
						});
						JOptionPane.showMessageDialog(frmRegister, res.errorMessage, "Registration failure",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					result = res;
					JOptionPane.showMessageDialog(frmRegister,
							"Account registered successfully!\n\nPlease note the password somewhere down as there is presently no way to reset passwords!",
							"Registration success", JOptionPane.INFORMATION_MESSAGE);
					SwingUtilities.invokeLater(() -> {
						frmRegister.dispose();
					});
				});
			}
		});
	}
}
