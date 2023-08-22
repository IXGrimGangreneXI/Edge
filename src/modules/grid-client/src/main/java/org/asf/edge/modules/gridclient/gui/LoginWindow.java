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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.BiFunction;
import java.awt.event.ActionEvent;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.modules.gridclient.gui.RegistrationWindow.RegistrationResult;
import org.asf.edge.modules.gridclient.phoenix.auth.PhoenixSession;

import java.awt.Font;

public class LoginWindow {

	public JFrame frmLogin;
	private JTextField usernameField;
	private JTextField passwordField;
	private JLabel lbl;

	private LoginResult result;
	private BiFunction<String, String, LoginResult> loginCallback;
	private BiFunction<String, String, RegistrationResult> registrationCallback;
	private BiFunction<String, String, LoginResult> authRefreshCallback;

	public static class LoginResult {
		public PhoenixSession session;
		public String refreshToken;

		public String errorMessage;
		public boolean success;
	}

	/**
	 * Create the application.
	 */
	public LoginWindow() {
		initialize();
	}

	private class Holder {
		public boolean b;
	}

	public LoginResult show(String username, BiFunction<String, String, LoginResult> loginCallback,
			BiFunction<String, String, RegistrationResult> registrationCallback,
			BiFunction<String, String, LoginResult> authRefreshCallback) {
		Holder h = new Holder();
		AsyncTaskManager.runAsync(() -> {
			frmLogin.dispose();
			frmLogin = new JFrame();
			initialize();
			frmLogin.setLocationRelativeTo(null);
			this.loginCallback = loginCallback;
			this.registrationCallback = registrationCallback;
			this.authRefreshCallback = authRefreshCallback;
			if (username != null) {
				usernameField.setText(username);
				frmLogin.pack();
				usernameField.requestFocusInWindow();
			}
			frmLogin.setVisible(true);
			h.b = true;
		});
		while (!h.b)
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		while (frmLogin.isVisible())
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		return result;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmLogin = new JFrame();
		frmLogin.setTitle("Grid login");
		frmLogin.setBounds(100, 100, 537, 350);
		frmLogin.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		frmLogin.getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		frmLogin.setLocationRelativeTo(null);
		frmLogin.setResizable(false);
		frmLogin.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				int res = JOptionPane.showConfirmDialog(frmLogin, "Are you sure you want to close the Edge server?",
						"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (res != JOptionPane.YES_OPTION) {
					return;
				}
				System.exit(0);
			}

		});

		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(500, 300));
		frmLogin.getContentPane().add(panel);
		panel.setLayout(null);

		lbl = new JLabel("Welcome to the Multiplayer Grid system of Project Edge!");
		lbl.setFont(new Font("Dialog", Font.BOLD, 14));
		lbl.setHorizontalAlignment(SwingConstants.CENTER);
		lbl.setBounds(12, 12, 476, 35);
		panel.add(lbl);

		JLabel lblUser = new JLabel("Grid account username");
		lblUser.setBounds(12, 91, 476, 17);
		panel.add(lblUser);

		usernameField = new JTextField();
		usernameField.setBounds(12, 109, 476, 21);
		panel.add(usernameField);

		JLabel lblPass = new JLabel("Grid account password");
		lblPass.setBounds(12, 140, 476, 17);
		panel.add(lblPass);

		passwordField = new JPasswordField();
		passwordField.setBounds(12, 158, 476, 21);
		JButton btnNewButton = new JButton("Log in");
		passwordField.addActionListener(new AbstractAction() {
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
		panel.add(passwordField);
		btnNewButton.setBounds(346, 261, 142, 27);
		panel.add(btnNewButton);

		JButton btnNewButton_1 = new JButton("Cancel");
		btnNewButton_1.setBounds(229, 261, 105, 27);
		panel.add(btnNewButton_1);

		JButton btnNewButton_2 = new JButton("Register");
		btnNewButton_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Open window
				RegistrationWindow win = new RegistrationWindow();
				RegistrationResult res = win.show(frmLogin, registrationCallback);
				if (res != null) {
					LoginResult r = authRefreshCallback.apply(res.refreshToken, res.username);
					if (!r.success) {
						JOptionPane.showMessageDialog(frmLogin, r.errorMessage, "Login failure",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					result = r;
					frmLogin.dispose();
				}
			}
		});
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String msg = "Are you sure you wish to cancel login?\n\n";
				msg += "If you do, Edge will proceed in offline mode.\n";
//				msg += "Note that in offline mode you will NOT be able to use Grid saves!";
				msg += "Note that in offline mode your progress will NOT sync to the Grid servers!";
				int res = JOptionPane.showConfirmDialog(frmLogin, msg, "Warning", JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE);
				if (res != JOptionPane.YES_OPTION) {
					return;
				}
				frmLogin.dispose();
			}
		});
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Set result
				usernameField.setEnabled(false);
				passwordField.setEnabled(false);
				btnNewButton.setEnabled(false);
				btnNewButton.setText("Logging in...");
				btnNewButton_1.setEnabled(false);
				btnNewButton_2.setEnabled(false);
				AsyncTaskManager.runAsync(() -> {
					LoginResult res = loginCallback.apply(usernameField.getText(), passwordField.getText());
					if (!res.success) {
						SwingUtilities.invokeLater(() -> {
							btnNewButton.setText("Log in");
							usernameField.setEnabled(true);
							passwordField.setEnabled(true);
							btnNewButton.setEnabled(true);
							btnNewButton_1.setEnabled(true);
							btnNewButton_2.setEnabled(true);
						});
						JOptionPane.showMessageDialog(frmLogin, res.errorMessage, "Login failure",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					result = res;
					SwingUtilities.invokeLater(() -> {
						frmLogin.dispose();
					});
				});
			}
		});
		btnNewButton_2.setBounds(12, 261, 105, 27);
		panel.add(btnNewButton_2);
	}
}
