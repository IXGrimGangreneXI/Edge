package org.asf.edge.mmoserver.tools;

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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.awt.event.ActionEvent;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.ListSelectionListener;

import org.asf.edge.mmoserver.networking.sfs.SmartfoxNetworkObjectUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.event.ListSelectionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class SnifferViewer {

	private JFrame frmEdgeSnifferLog;
	private ArrayList<SnifferDataBlock> sniffData = new ArrayList<SnifferDataBlock>();
	private FileInputStream lastSniffData = null;

	private JScrollPane scrollPane_1;
	private JTextPane textRequestInfo;

	private JList<SnifferDataBlock> list;
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
		public String type;

		// Host
		public String host;

		// Port
		public int port;

		// Side
		public String side;

		// Time
		public long time;

		// Data
		public byte[] data;

		private String srStr;

		public String dataString() {
			if (srStr != null)
				return srStr;

			srStr = "Host: " + host + "\n";
			srStr += "Port: " + port + "\n";
			srStr += "Side: " + side + "\n";
			srStr += "Type: " + type + "\n";
			srStr += "\n";

			try {
				srStr += new ObjectMapper().writerWithDefaultPrettyPrinter()
						.writeValueAsString(SmartfoxNetworkObjectUtil.parseSfsObject(data));
			} catch (IOException e) {
				srStr += bytesToHex(data);
			}

			return srStr;
		}

		private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

		public static String bytesToHex(byte[] bytes) {
			char[] hexChars = new char[bytes.length * 2];
			for (int j = 0; j < bytes.length; j++) {
				int v = bytes[j] & 0xFF;
				hexChars[j * 2] = HEX_ARRAY[v >>> 4];
				hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
			}
			return new String(hexChars);
		}

		public String toString() {
			String str = "[" + type + "] ";
			str += side + ": ";
			str += new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new Date(time));
			str += " ([" + host + "]:" + port + ")";
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

		JPanel panelSniff = new JPanel();
		panelSniff.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		panelSniff.setLayout(new BorderLayout(0, 0));
		panel_1.add(panelSniff, BorderLayout.CENTER);

		scrollPane_1 = new JScrollPane();
		scrollPane_1.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		scrollPane_1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		panelSniff.add(scrollPane_1, BorderLayout.CENTER);

		textRequestInfo = new JTextPane();
		textRequestInfo.setEditable(false);
		scrollPane_1.setViewportView(textRequestInfo);
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
							if ((!obj.has("time") || !obj.has("host") || !obj.has("port") || !obj.has("side")
									|| !obj.has("data"))
									&& (obj.has("type") && !obj.get("type").getAsString().equals("http")))
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
					if ((!obj.has("time") || !obj.has("host") || !obj.has("port") || !obj.has("side")
							|| !obj.has("data")) && (obj.has("type") && !obj.get("type").getAsString().equals("http")))
						throw new IOException("Invalid file");
					if (obj.has("type") && !obj.get("type").getAsString().equals("bstcp")
							&& !obj.get("type").getAsString().equals("udp"))
						continue;

					// Load object into memory
					SnifferDataBlock block = new SnifferDataBlock();
					block.type = obj.get("type").getAsString().equals("udp") ? "UDP" : "TCP";
					block.host = obj.get("host").getAsString();
					block.port = obj.get("port").getAsInt();
					block.side = obj.get("side").getAsString();
					block.time = obj.get("time").getAsLong();
					block.data = Base64.getDecoder().decode(obj.get("data").getAsString());
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
			else if (block.dataString().toLowerCase().contains(filter.toLowerCase()))
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
		textRequestInfo.setText("");

		// Load info
		if (block != null) {
			// Load from file
			textRequestInfo.setText("Decoding from disk...");
			textRequestInfo.updateUI();
			SwingUtilities.invokeLater(() -> scrollPane_1.getViewport().setViewPosition(new Point(0, 0)));
			SwingUtilities.invokeLater(() -> {
				// Set text
				textRequestInfo.setText(block.dataString());

				// Update UI
				scrollPane_1.updateUI();
				SwingUtilities.invokeLater(() -> scrollPane_1.getViewport().setViewPosition(new Point(0, 0)));
			});
		} else {
			// Update UI
			scrollPane_1.updateUI();
			SwingUtilities.invokeLater(() -> scrollPane_1.getViewport().setViewPosition(new Point(0, 0)));
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
