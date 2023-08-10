package org.asf.edge.modules.accountimporter;

import javax.swing.JFrame;
import java.awt.FlowLayout;
import javax.swing.JPanel;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.asf.edge.common.services.accounts.AccountManager;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class PasswordCreationWindow {

	public JDialog frmCreateAPassword;
	private JTextField passwordField;
	private JTextField passwordField_1;
	private JLabel lbl;
	private String result;

	/**
	 * Create the application.
	 */
	public PasswordCreationWindow() {
		initialize();
	}

	public String show(String name, JFrame parent) {
		frmCreateAPassword.dispose();
		frmCreateAPassword = new JDialog(parent);
		frmCreateAPassword.setModal(true);
		initialize();
		lbl.setText("Create a password for " + name);
		frmCreateAPassword.setVisible(true);
		return result;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		if (frmCreateAPassword == null)
			frmCreateAPassword = new JDialog();
		frmCreateAPassword.setTitle("Create a password");
		frmCreateAPassword.setBounds(100, 100, 537, 249);
		frmCreateAPassword.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		frmCreateAPassword.setLocationRelativeTo(null);
		frmCreateAPassword.setResizable(false);
		frmCreateAPassword.getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(500, 200));
		frmCreateAPassword.getContentPane().add(panel);
		panel.setLayout(null);

		lbl = new JLabel("Create a password for abcdefghijklmnopqrstuvwxyz");
		lbl.setBounds(12, 12, 476, 17);
		panel.add(lbl);

		JLabel lblPassword = new JLabel("Password");
		lblPassword.setBounds(12, 51, 476, 17);
		panel.add(lblPassword);

		passwordField = new JPasswordField();
		passwordField.setBounds(12, 69, 476, 21);
		panel.add(passwordField);

		JLabel lblRepeatPassword = new JLabel("Repeat password");
		lblRepeatPassword.setBounds(12, 100, 476, 17);
		panel.add(lblRepeatPassword);

		passwordField_1 = new JPasswordField();
		passwordField_1.setBounds(12, 118, 476, 21);
		JButton btnNewButton = new JButton("OK");
		passwordField_1.addActionListener(new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				btnNewButton.doClick();
			}
		});
		panel.add(passwordField_1);

		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Check
				if (!passwordField_1.getText().equals(passwordField.getText())) {
					JOptionPane.showMessageDialog(frmCreateAPassword, "Passwords do not match", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Verify validity
				if (!AccountManager.getInstance().isValidPassword(passwordField.getText())) {
					JOptionPane.showMessageDialog(frmCreateAPassword,
							"Password is invalid, please make sure it is at least 6 characters in length", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Set result
				result = passwordField.getText();
				frmCreateAPassword.dispose();
			}
		});
		btnNewButton.setBounds(383, 161, 105, 27);
		panel.add(btnNewButton);

		JButton btnNewButton_1 = new JButton("Cancel");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frmCreateAPassword.dispose();
			}
		});
		btnNewButton_1.setBounds(266, 161, 105, 27);
		panel.add(btnNewButton_1);
	}
}
