package org.asf.edge.commonapi.tools;

import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Point;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListDataListener;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.JButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.JList;
import javax.swing.JOptionPane;

import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.awt.event.ActionEvent;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.ListSelectionListener;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.event.ListSelectionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class SnifferViewer {

	private JTabbedPane tabbedPane;
	private JFrame frmEdgeSnifferLog;
	private ArrayList<SnifferDataBlock> sniffData = new ArrayList<SnifferDataBlock>();
	private FileInputStream lastSniffData = null;

	private JScrollPane scrollPane_1;
	private JScrollPane scrollPane_2;
	private JTextPane textRequestInfo;
	private JTextPane textResponseInfo;

	private JList<SnifferDataBlock> list;
	private JTextField urlBox;
	private JTextField methodBox;
	private JTextField statusBox;
	private JTextField statusMessageBox;
	private JTextField searchBox;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			try {
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
					| UnsupportedLookAndFeelException e1) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e1) {
		}

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					SnifferViewer window = new SnifferViewer();
					window.frmEdgeSnifferLog.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public SnifferViewer() {
		initialize();
	}

	private class SnifferDataBlock {
		// URL
		public String url;

		// Position in file
		public long position;

		public String toString() {
			URL u;
			try {
				u = new URL(url);
			} catch (MalformedURLException e) {
				return url;
			}
			String str = u.getPath();
			if (u.getQuery() != null) {
				str += "?" + u.getQuery();
			}
			str += " (" + u.getHost() + ")";
			return str;
		}
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmEdgeSnifferLog = new JFrame();
		frmEdgeSnifferLog.setTitle("EDGE Sniffer Log Viewer");
		frmEdgeSnifferLog.setBounds(100, 100, 1003, 670);
		frmEdgeSnifferLog.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmEdgeSnifferLog.setLocationRelativeTo(null);

		JPanel panel = new JPanel();
		panel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		panel.setPreferredSize(new Dimension(350, 10));
		frmEdgeSnifferLog.getContentPane().add(panel, BorderLayout.WEST);
		panel.setLayout(new BorderLayout(0, 0));

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		panel.add(scrollPane, BorderLayout.CENTER);

		list = new JList<SnifferDataBlock>();
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting())
					return;
				setSelectedBlock(list.getSelectedValue());
			}
		});
		scrollPane.setViewportView(list);

		JPanel panel_3 = new JPanel();
		panel_3.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		panel.add(panel_3, BorderLayout.SOUTH);

		JButton btnNewButton = new JButton("Load file...");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Open file chooser
				FileDialog chooser = new FileDialog(frmEdgeSnifferLog, "Open log file", FileDialog.LOAD);
				chooser.setFilenameFilter((dir, name) -> name.endsWith(".log") || new File(dir, name).isDirectory());
				chooser.setMultipleMode(false);
				chooser.setVisible(true);
				if (chooser.getFile() != null) {
					// Load file
					loadSniffFile(chooser.getFiles()[0]);
				}
			}
		});
		panel_3.add(btnNewButton);

		JPanel panel_7 = new JPanel();
		panel_7.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		panel.add(panel_7, BorderLayout.NORTH);
		panel_7.setLayout(new BorderLayout(0, 0));

		JLabel lblNewLabel = new JLabel("Packets");
		panel_7.add(lblNewLabel, BorderLayout.NORTH);

		searchBox = new JTextField();
		searchBox.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				updateList();
			}
		});
		panel_7.add(searchBox, BorderLayout.SOUTH);
		searchBox.setColumns(10);

		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		frmEdgeSnifferLog.getContentPane().add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new BorderLayout(0, 0));

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		panel_1.add(tabbedPane, BorderLayout.CENTER);

		JPanel panelRequest = new JPanel();
		panelRequest.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		tabbedPane.addTab("Request", null, panelRequest, null);
		panelRequest.setLayout(new BorderLayout(0, 0));

		JPanel panel_2 = new JPanel();
		panel_2.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		panelRequest.add(panel_2, BorderLayout.NORTH);
		panel_2.setLayout(new BorderLayout(0, 0));

		JPanel panel_4 = new JPanel();
		panel_2.add(panel_4, BorderLayout.NORTH);
		panel_4.setLayout(new BorderLayout(0, 0));

		JLabel lblNewLabel_1 = new JLabel("Method and URL");
		panel_4.add(lblNewLabel_1, BorderLayout.NORTH);

		methodBox = new JTextField();
		methodBox.setEditable(false);
		methodBox.setColumns(10);
		panel_4.add(methodBox, BorderLayout.WEST);

		urlBox = new JTextField();
		urlBox.setEditable(false);
		panel_4.add(urlBox, BorderLayout.CENTER);
		urlBox.setColumns(10);

		JPanel panel_5 = new JPanel();
		panel_2.add(panel_5, BorderLayout.CENTER);

		scrollPane_1 = new JScrollPane();
		scrollPane_1.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		scrollPane_1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		panelRequest.add(scrollPane_1, BorderLayout.CENTER);

		textRequestInfo = new JTextPane();
		textRequestInfo.setEditable(false);
		scrollPane_1.setViewportView(textRequestInfo);

		JPanel panelResponse = new JPanel();
		panelResponse.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		tabbedPane.addTab("Response", null, panelResponse, null);
		panelResponse.setLayout(new BorderLayout(0, 0));

		JPanel panel_2_1 = new JPanel();
		panel_2_1.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		panelResponse.add(panel_2_1, BorderLayout.NORTH);
		panel_2_1.setLayout(new BorderLayout(0, 0));

		JLabel lblNewLabel_2 = new JLabel("Response status");
		panel_2_1.add(lblNewLabel_2, BorderLayout.NORTH);

		JPanel panel_6 = new JPanel();
		panel_2_1.add(panel_6, BorderLayout.SOUTH);

		statusBox = new JTextField();
		statusBox.setEditable(false);
		statusBox.setColumns(10);
		panel_2_1.add(statusBox, BorderLayout.WEST);

		statusMessageBox = new JTextField();
		statusMessageBox.setEditable(false);
		panel_2_1.add(statusMessageBox, BorderLayout.CENTER);
		statusMessageBox.setColumns(10);

		scrollPane_2 = new JScrollPane();
		scrollPane_2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane_2.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		panelResponse.add(scrollPane_2, BorderLayout.CENTER);

		textResponseInfo = new JTextPane();
		textResponseInfo.setEditable(false);
		scrollPane_2.setViewportView(textResponseInfo);
	}

	@SuppressWarnings("resource")
	private void loadSniffFile(File file) {
		searchBox.setText("");

		// Attempt to load file
		boolean validFile = true;
		FileInputStream sourceFile = null;
		try {
			sourceFile = new FileInputStream(file);
			try {
				// Read file
				String buffer = "";
				boolean inBlock = false;
				while (true) {
					int b = sourceFile.read();
					if (b == -1)
						break;

					// Convert to byte
					byte bb = (byte) b;

					// Verify character
					char ch = (char) bb;
					if (ch == '\r')
						continue;
					else {
						if (!inBlock) {
							// Check character
							if (ch == '\n')
								continue;
							if (ch == '{')
								inBlock = true;
							else
								throw new IOException("Invalid file");
						}

						// Check character
						if (ch == '\n') {
							// Done, verify JSON
							JsonObject obj = JsonParser.parseString(buffer).getAsJsonObject();
							buffer = "";

							// Check fields
							if (!obj.has("time") || !obj.has("request") || !obj.has("response")
									|| !obj.get("request").getAsJsonObject().has("url")
									|| !obj.get("request").getAsJsonObject().has("method")
									|| !obj.get("request").getAsJsonObject().has("headers")
									|| !obj.get("request").getAsJsonObject().has("hasBody")
									|| !obj.get("response").getAsJsonObject().has("status")
									|| !obj.get("response").getAsJsonObject().has("statusMessage")
									|| !obj.get("response").getAsJsonObject().has("headers")
									|| !obj.get("response").getAsJsonObject().has("responseBody"))
								throw new IOException("Invalid file");
							break;
						}

						// Read to buffer
						buffer += ch;
					}
				}

				// Move back
				sourceFile.getChannel().position(0);
			} catch (Exception e) {
				try {
					sourceFile.close();
				} catch (IOException e2) {
				}
				throw e;
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(frmEdgeSnifferLog,
					"The file you selected is not a valid packet recording file.", "Error", JOptionPane.ERROR_MESSAGE);
			if (sourceFile != null)
				try {
					sourceFile.close();
				} catch (IOException e2) {
				}
			return;
		}
		if (!validFile) {
			JOptionPane.showMessageDialog(frmEdgeSnifferLog,
					"The file you selected is not a valid packet recording file.", "Error", JOptionPane.ERROR_MESSAGE);
			if (sourceFile != null)
				try {
					sourceFile.close();
				} catch (IOException e2) {
				}
			return;
		}

		// Clear sniff
		sniffData.clear();
		if (lastSniffData != null)
			try {
				lastSniffData.close();
			} catch (IOException e) {
			}

		// Set sniff data source
		lastSniffData = sourceFile;

		// Read data into memory
		try {
			// Read file
			String buffer = "";
			long blockStart = -1;
			while (true) {
				int b = sourceFile.read();
				if (b == -1)
					break;

				// Convert to byte
				byte bb = (byte) b;

				// Verify character
				char ch = (char) bb;
				if (ch == '\r')
					continue;
				else if (ch == '\n')
					continue;
				else if (ch == '{') {
					// Check character
					blockStart = sourceFile.getChannel().position();
					buffer = Character.toString(ch);

					// Read until end of block
					while (true) {
						byte[] bufferD = new byte[20480];
						int read = sourceFile.read(bufferD);
						if (read <= 0)
							break;

						// Go over buffer data
						String bData = new String(bufferD, "UTF-8");
						if (bData.contains("\n")) {
							buffer += bData.substring(0, bData.indexOf("\n"));
							sourceFile.getChannel()
									.position(sourceFile.getChannel().position() - (read - bData.indexOf("\n")));
							break;
						} else
							buffer += bData;
					}

					// Done, verify JSON
					JsonObject obj = JsonParser.parseString(buffer).getAsJsonObject();

					// Check fields
					if (!obj.has("time") || !obj.has("request") || !obj.has("response")
							|| !obj.get("request").getAsJsonObject().has("url")
							|| !obj.get("request").getAsJsonObject().has("method")
							|| !obj.get("request").getAsJsonObject().has("headers")
							|| !obj.get("request").getAsJsonObject().has("hasBody")
							|| !obj.get("response").getAsJsonObject().has("status")
							|| !obj.get("response").getAsJsonObject().has("statusMessage")
							|| !obj.get("response").getAsJsonObject().has("headers")
							|| !obj.get("response").getAsJsonObject().has("responseBody"))
						throw new IOException("Invalid file");

					// Load object into memory
					SnifferDataBlock block = new SnifferDataBlock();
					block.position = blockStart;
					block.url = obj.get("request").getAsJsonObject().get("url").getAsString();
					sniffData.add(block);
				} else
					throw new IOException("Invalid file");
			}

			// Move back
			sourceFile.getChannel().position(0);
		} catch (Exception e) {
			String stackTrace = "";
			for (StackTraceElement ele : e.getStackTrace())
				stackTrace += "\n    at: " + ele;
			JOptionPane.showMessageDialog(frmEdgeSnifferLog,
					"An error occured while loading the packet log file: " + e.getClass().getTypeName()
							+ (e.getMessage() != null ? ": " + e.getMessage() : "") + "\n" + stackTrace,
					"Error", JOptionPane.ERROR_MESSAGE);
		}

		// Update
		updateList();
	}

	private void updateList() {
		ArrayList<SnifferDataBlock> filteredData = new ArrayList<SnifferDataBlock>();
		for (SnifferDataBlock block : sniffData) {
			String filter = searchBox.getText();
			filter = filter.trim();
			if (filter.isEmpty())
				filteredData.add(block);
			else if (block.url.toLowerCase().contains(filter.toLowerCase()))
				filteredData.add(block);
		}
		list.setModel(new ListModel<SnifferDataBlock>() {

			@Override
			public int getSize() {
				return filteredData.size();
			}

			@Override
			public SnifferDataBlock getElementAt(int index) {
				return filteredData.get(index);
			}

			@Override
			public void addListDataListener(ListDataListener l) {
			}

			@Override
			public void removeListDataListener(ListDataListener l) {
			}

		});
		list.repaint();
		list.setSelectedValue(null, false);
		setSelectedBlock(null);
	}

	private void setSelectedBlock(SnifferDataBlock block) {
		// Unset data
		tabbedPane.setSelectedIndex(0);
		textRequestInfo.setText("");
		textResponseInfo.setText("");
		urlBox.setText("");
		methodBox.setText("");
		statusBox.setText("");
		statusMessageBox.setText("");

		// Load info
		if (block != null) {
			// Load from file
			textRequestInfo.setText("Decoding from disk...");
			textRequestInfo.updateUI();
			SwingUtilities.invokeLater(() -> scrollPane_1.getViewport().setViewPosition(new Point(0, 0)));
			SwingUtilities.invokeLater(() -> scrollPane_2.getViewport().setViewPosition(new Point(0, 0)));
			SwingUtilities.invokeLater(() -> {
				try {
					// Seek
					lastSniffData.getChannel().position(block.position);

					// Parse data
					String buffer = "{";

					// Read until end of block
					while (true) {
						byte[] bufferD = new byte[20480];
						int read = lastSniffData.read(bufferD);
						if (read <= 0)
							break;

						// Go over buffer data
						String bData = new String(bufferD, "UTF-8");
						if (bData.contains("\n")) {
							buffer += bData.substring(0, bData.indexOf("\n"));
							lastSniffData.getChannel()
									.position(lastSniffData.getChannel().position() - (read - bData.indexOf("\n")));
							break;
						} else
							buffer += bData;
					}

					// Parse json
					JsonObject requestData = JsonParser.parseString(buffer).getAsJsonObject();

					// Set data
					urlBox.setText(block.url);
					methodBox.setText(requestData.get("request").getAsJsonObject().get("method").getAsString());
					statusBox.setText(requestData.get("response").getAsJsonObject().get("status").getAsString());
					statusMessageBox
							.setText(requestData.get("response").getAsJsonObject().get("statusMessage").getAsString());

					// Build request string
					JsonObject rq = requestData.get("request").getAsJsonObject();
					URL u = new URL(rq.get("url").getAsString());
					String requestInfo = rq.get("method").getAsString() + " " + u.getPath();
					requestInfo += "\n";
					if (u.getQuery() != null) {
						requestInfo += "?" + u.getQuery();
					}

					// Add headers
					requestInfo += "\n";
					requestInfo += "Host: " + u.getHost() + (u.getPort() == -1 ? "" : ":" + u.getPort());
					JsonObject headers = rq.get("headers").getAsJsonObject();
					for (String headerName : headers.keySet()) {
						if (headerName.equalsIgnoreCase("host"))
							continue;
						for (JsonElement value : headers.get(headerName).getAsJsonArray()) {
							requestInfo += "\n";
							requestInfo += headerName + ": " + value.getAsString();
						}
					}

					// Add body
					if (rq.get("hasBody").getAsBoolean()) {
						requestInfo += "\n";
						requestInfo += "\n";

						// Decode
						byte[] dec = Base64.getDecoder().decode(rq.get("requestBody").getAsString());

						// Check encoding
						if (headers.has("Content-Encoding")
								&& headers.get("Content-Encoding").getAsString().equalsIgnoreCase("gzip")) {
							GZIPInputStream strm = new GZIPInputStream(new ByteArrayInputStream(dec));
							dec = strm.readAllBytes();
							strm.close();
						}

						// Set body string
						String body = new String(dec, "UTF-8");

						// Attempt decode
						if (headers.has("Content-Type") && headers.get("Content-Type").getAsString()
								.equalsIgnoreCase("application/x-www-form-urlencoded")) {
							// Decode form
							Map<String, String> form = parseForm(body.replace("\r", "\n"));
							body = "";
							for (String key : form.keySet()) {
								if (!body.isEmpty())
									body += "\n";
								else if (form.get(key).contains("\n"))
									body += "\n";
								if (form.get(key).contains("\n"))
									body += key + ":\n" + form.get(key).replace("\n", "\n ");
								else
									body += key + ": " + form.get(key);
								if (form.get(key).contains("\n"))
									body += "\n";
							}
						}
						requestInfo += body;
					}

					// Set text
					textRequestInfo.setText(requestInfo);

					// Build response string
					JsonObject rs = requestData.get("response").getAsJsonObject();
					String responseInfo = rs.get("status").getAsString() + " " + rs.get("statusMessage").getAsString();
					responseInfo += "\n";

					// Add headers
					headers = rs.get("headers").getAsJsonObject();
					for (String headerName : headers.keySet()) {
						if (headerName.equalsIgnoreCase("host"))
							continue;
						for (JsonElement value : headers.get(headerName).getAsJsonArray()) {
							responseInfo += "\n";
							responseInfo += headerName + ": " + value.getAsString();
						}
					}

					// Add body
					responseInfo += "\n";
					responseInfo += "\n";
					byte[] dec = Base64.getDecoder().decode(rs.get("responseBody").getAsString());

					// Check encoding
					if (headers.has("Content-Encoding")
							&& headers.get("Content-Encoding").getAsString().equalsIgnoreCase("gzip")) {
						GZIPInputStream strm = new GZIPInputStream(new ByteArrayInputStream(dec));
						dec = strm.readAllBytes();
						strm.close();
					}

					// Set body
					responseInfo += new String(dec, "UTF-8");

					// Set text
					textResponseInfo.setText(responseInfo);
				} catch (IOException e) {
					String stackTrace = "";
					for (StackTraceElement ele : e.getStackTrace())
						stackTrace += "\n    at: " + ele;
					JOptionPane.showMessageDialog(frmEdgeSnifferLog,
							"An error occured while loading the packet log file: " + e.getClass().getTypeName()
									+ (e.getMessage() != null ? ": " + e.getMessage() : "") + "\n" + stackTrace,
							"Error", JOptionPane.ERROR_MESSAGE);

					// Clear sniff data
					sniffData.clear();
					if (lastSniffData != null)
						try {
							lastSniffData.close();
						} catch (IOException e2) {
						}
					lastSniffData = null;
					searchBox.setText("");
					updateList();
					return;
				}

				// Set text
				urlBox.setText(block.url);

				// Update UI
				scrollPane_1.updateUI();
				scrollPane_2.updateUI();
				SwingUtilities.invokeLater(() -> scrollPane_1.getViewport().setViewPosition(new Point(0, 0)));
				SwingUtilities.invokeLater(() -> scrollPane_2.getViewport().setViewPosition(new Point(0, 0)));
			});
		} else {
			// Update UI
			scrollPane_1.updateUI();
			scrollPane_2.updateUI();
			SwingUtilities.invokeLater(() -> scrollPane_1.getViewport().setViewPosition(new Point(0, 0)));
			SwingUtilities.invokeLater(() -> scrollPane_2.getViewport().setViewPosition(new Point(0, 0)));
		}
	}

	/**
	 * Tool to decode URL-encoded forms
	 * 
	 * @param payload Form payload
	 * @return Map containing the form data
	 */
	public Map<String, String> parseForm(String payload) {
		HashMap<String, String> frm = new HashMap<String, String>();
		String key = "";
		String value = "";
		boolean isKey = true;
		for (int i = 0; i < payload.length(); i++) {
			char ch = payload.charAt(i);
			if (ch == '&') {
				if (isKey && !key.isEmpty()) {
					frm.put(key, "");
					key = "";
				} else if (!isKey && !key.isEmpty()) {
					try {
						frm.put(key, URLDecoder.decode(value, "UTF-8"));
					} catch (Exception e) {
						frm.put(key, value);
					}
					isKey = true;
					key = "";
					value = "";
				}
			} else if (ch == '=') {
				isKey = !isKey;
			} else {
				if (isKey) {
					key += ch;
				} else {
					value += ch;
				}
			}
		}
		if (!key.isEmpty() || !value.isEmpty()) {
			try {
				frm.put(key, URLDecoder.decode(value, "UTF-8"));
			} catch (Exception e) {
				frm.put(key, value);
			}
		}
		return frm;
	}

}
