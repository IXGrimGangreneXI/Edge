package org.asf.edge.common.util;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.asf.connective.tasks.AsyncTaskManager;

import javax.swing.JTextArea;
import javax.swing.JTextField;

import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class LogWindow {
	public JFrame frm;
	public static boolean shown = false;
	private static ArrayList<String> commandHistory = new ArrayList<String>();
	private static int commandIndex = -1;

	public static Consumer<String> commandCallback = null;

	public static class WindowAppender extends AbstractAppender {

		@SuppressWarnings("deprecation")
		protected WindowAppender(PatternLayout layout) {
			super("Window Appender", null, layout);
			start();
		}

		@Override
		public void append(LogEvent event) {
			if (frame == null)
				return;
			if (!event.getLevel().isInRange(Level.FATAL, Level.INFO))
				return;
			String message = new String(getLayout().toByteArray(event));
			if (message.endsWith("\n"))
				message = message.substring(0, message.length() - 1);

			// Generate prefix
			Calendar cal = Calendar.getInstance();
			int h = cal.get(Calendar.HOUR_OF_DAY);
			int m = cal.get(Calendar.MINUTE);
			int s = cal.get(Calendar.SECOND);
			String timeStr = (h >= 10 ? "" : "0") + Integer.toString(h) + ":" + (m >= 10 ? "" : "0") + m + ":"
					+ (s >= 10 ? "" : "0") + s;
			String pref = "[" + timeStr;
			pref += " ";
			if (event.getLevel().toString().length() == 4)
				pref += " ";
			pref += event.getLevel().toString();
			pref += "] - ";

			// Write message
			if (message.contains("\n"))
				for (String ln : message.split("\n"))
					frame.log(pref + ln);
			else
				frame.log(pref + message);
		}

		public static LogWindow frame = new LogWindow();

		public static void showWindow() {
			shown = true;
			frame = new LogWindow();
			final LoggerContext context = LoggerContext.getContext(false);
			final Configuration config = context.getConfiguration();
			final PatternLayout layout = PatternLayout.createDefaultLayout();
			final Appender appender = new WindowAppender(layout);
			config.addAppender(appender);
			updateLoggers(appender, context.getConfiguration());
			frame.frm.setVisible(true);
			frame.textField.grabFocus();
		}

		public static void closeWindow() {
			if (!shown)
				return;
			shown = false;
			commandHistory.clear();
			commandIndex = -1;
			if (frame != null) {
				frame.frm.dispose();
				frame = null;
			}
			LoggerContext.getContext(false).getConfiguration().getRootLogger().removeAppender("Window Appender");
		}

		private static void updateLoggers(final Appender appender, final Configuration config) {
			final Level level = null;
			final Filter filter = null;
			for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
				loggerConfig.addAppender(appender, level, filter);
			}
			config.getRootLogger().addAppender(appender, level, filter);
		}

		public static void log(String message) {
			if (frame == null)
				return;
			frame.log(message);
		}

	}

	private JTextArea textArea = new JTextArea();

	public void log(String message) {
		if (!shown)
			return;

		textArea.setText(textArea.getText() + message + "\n");
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}

	private JPanel contentPane;
	private JTextField textField;

	/**
	 * Create the frame.
	 */
	public LogWindow() {
		if (shown) {
			frm = new JFrame();
			frm.setTitle("Project Edge");
			frm.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frm.setBounds(100, 100, 900, 500);
			frm.setResizable(false);
			frm.setLocationRelativeTo(null);
		}
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));

		if (shown)
			frm.setContentPane(contentPane);

		textArea.setEditable(false);
		JScrollPane pane = new JScrollPane(textArea);
		pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		DefaultCaret caret = (DefaultCaret) textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		textField = new JTextField();
		textField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					// Down
					if (commandIndex >= 0) {
						commandIndex--;
						if (commandIndex >= 0)
							textField.setText(commandHistory.get(commandIndex));
						else
							textField.setText("");
					} else
						textField.setText("");
					e.consume();
				} else if (e.getKeyCode() == KeyEvent.VK_UP) {
					// Up
					if (commandIndex < commandHistory.size()) {
						commandIndex++;
						if (commandIndex < commandHistory.size())
							textField.setText(commandHistory.get(commandIndex));
						else
							commandIndex--;
					}
					e.consume();
				}
			}
		});
		textField.addActionListener(new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				String cmd = textField.getText();
				textField.setText("");
				commandIndex = -1;
				if (!cmd.isEmpty()) {
					commandHistory.add(0, cmd);
					if (commandHistory.size() >= 500)
						commandHistory.remove(commandHistory.size() - 1);

					// Run command
					AsyncTaskManager.runAsync(() -> {
						if (commandCallback != null)
							commandCallback.accept(cmd);
					});
				}
			}
		});
		textField.setColumns(10);

		contentPane.add(textField, BorderLayout.SOUTH);
		contentPane.add(pane, BorderLayout.CENTER);

		if (frm != null)
			frm.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					if (shown) {
						shown = false;
						commandHistory.clear();
						commandIndex = -1;
						frm.dispose();
						System.exit(0);
					}
				}
			});
	}
}
