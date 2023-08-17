package org.asf.edge.common.util;

import java.awt.BorderLayout;

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
import javax.swing.JTextArea;

public class LogWindow {
	public JFrame frm;
	public static boolean shown = false;

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

			if (message.contains("\n"))
				for (String ln : message.split("\n"))
					frame.log("[" + event.getLevel() + "] " + ln);
			else
				frame.log("[" + event.getLevel() + "] " + message);
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
		}

		public static void closeWindow() {
			if (!shown)
				return;
			shown = false;
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

	/**
	 * Create the frame.
	 */
	public LogWindow() {
		if (shown) {
			frm = new JFrame();
			frm.setTitle("Project Edge");
			frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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

		contentPane.add(pane, BorderLayout.CENTER);
	}
}
