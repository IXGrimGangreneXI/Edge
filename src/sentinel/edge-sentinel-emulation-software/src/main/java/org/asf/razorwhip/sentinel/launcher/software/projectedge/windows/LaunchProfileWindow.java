package org.asf.razorwhip.sentinel.launcher.software.projectedge.windows;

import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListDataListener;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.JList;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionListener;
import org.asf.razorwhip.sentinel.launcher.software.projectedge.EdgeEmulationSoftware;

import javax.swing.event.ListSelectionEvent;

public class LaunchProfileWindow extends JDialog {

	private static final long serialVersionUID = 1l;

	private JButton btnAdd;
	private JButton btnRemove;
	private JButton btnEdit;
	private JButton btnCancel;
	private JButton btnOk;

	private JList<ProfileEntry> profilesList;
	private HashMap<String, ProfileEntry> profiles = new HashMap<String, ProfileEntry>();

	private boolean wasCancelled = true;

	private class ProfileEntry {
		public String profileID;
		public String profileName;
		public JsonObject profileData;

		@Override
		public String toString() {
			return profileName;
		}
	}

	/**
	 * Create the application.
	 */
	public LaunchProfileWindow() {
		initialize();
	}

	public LaunchProfileWindow(Window parent) {
		super(parent);
		initialize();
		setLocationRelativeTo(parent);
	}

	public boolean showDialog() throws IOException {
		// Load
		loadProfiles();

		// Show
		setVisible(true);

		// Return
		return !wasCancelled;
	}

	private void loadProfiles() throws IOException {
		// Clear
		profiles.clear();

		// Load profiles
		File profileFile = new File("edgelauncher.json");
		JsonObject launcherSettings = new JsonObject();
		if (profileFile.exists()) {
			// Load
			launcherSettings = JsonParser.parseString(Files.readString(profileFile.toPath())).getAsJsonObject();
			if (launcherSettings.has("launchProfiles")) {
				JsonObject profiles = launcherSettings.get("launchProfiles").getAsJsonObject();
				for (String id : profiles.keySet()) {
					JsonObject profile = profiles.get(id).getAsJsonObject();
					ProfileEntry prof = new ProfileEntry();
					prof.profileID = id;
					prof.profileName = profile.get("profileName").getAsString();
					prof.profileData = profile;
					this.profiles.put(id, prof);
				}
			}
		}

		// Load profile list
		refreshProfileList();

		// Check
		if (launcherSettings.has("activeProfile")) {
			String id = launcherSettings.get("activeProfile").getAsString();
			profilesList.setSelectedValue(profiles.get(id), true);
		}
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		setTitle("Launch profiles");
		setBounds(100, 100, 600, 515);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setLocationRelativeTo(null);
		setResizable(false);
		setModal(true);
		getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(590, 475));
		getContentPane().add(panel);
		panel.setLayout(null);

		btnOk = new JButton("Launch");
		btnOk.setBounds(473, 436, 105, 27);
		panel.add(btnOk);

		btnCancel = new JButton("Cancel");
		btnCancel.setBounds(12, 436, 105, 27);
		panel.add(btnCancel);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setBounds(12, 33, 566, 352);
		panel.add(scrollPane);

		profilesList = new JList<ProfileEntry>();
		profilesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPane.setViewportView(profilesList);

		JLabel lblNewLabel = new JLabel("Profiles");
		lblNewLabel.setBounds(12, 12, 566, 17);
		panel.add(lblNewLabel);

		btnAdd = new JButton("Add profile");
		btnAdd.setBounds(473, 390, 105, 27);
		panel.add(btnAdd);

		btnRemove = new JButton("Delete");
		btnRemove.setEnabled(false);
		btnRemove.setBounds(12, 390, 105, 27);
		panel.add(btnRemove);

		btnEdit = new JButton("Edit profile");
		btnEdit.setEnabled(false);
		btnEdit.setBounds(121, 390, 105, 27);
		panel.add(btnEdit);

		// ADd events
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		btnOk.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Close
				doSave(true);
			}
		});
		profilesList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				btnRemove.setEnabled(profilesList.getSelectedValue() != null);
				btnEdit.setEnabled(profilesList.getSelectedValue() != null);
				btnOk.setEnabled(profilesList.getSelectedValue() != null);
			}
		});
		btnAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Create window
				LaunchProfileConfigWindow window = new LaunchProfileConfigWindow(LaunchProfileWindow.this);
				if (window.showDialog()) {
					// Reload
					try {
						loadProfiles();
						profilesList.setSelectedValue(profiles.get(window.resultID()), true);
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}
				}
			}
		});
		btnEdit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Create window
				LaunchProfileConfigWindow window = new LaunchProfileConfigWindow(LaunchProfileWindow.this,
						profilesList.getSelectedValue().profileData);
				if (window.showDialog()) {
					// Reload
					try {
						loadProfiles();
						profilesList.setSelectedValue(profiles.get(window.resultID()), true);
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}
				}
			}
		});
		btnRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (JOptionPane.showConfirmDialog(LaunchProfileWindow.this,
						"Are you sure you want to remove this profile?", "Warning", JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
					return;
				}

				// Remove
				ProfileEntry entry = (ProfileEntry) profilesList.getSelectedValue();
				profiles.remove(entry.profileID);
				refreshProfileList();

				// Save
				try {
					saveSettings(false);
				} catch (IOException e1) {
					throw new RuntimeException(e1);
				}
			}
		});

		// Set
		btnOk.setEnabled(profilesList.getSelectedValue() != null);
	}

	private void refreshProfileList() {
		ProfileEntry[] entries = profiles.values().toArray(t -> new ProfileEntry[t]);
		profilesList.setModel(new ListModel<ProfileEntry>() {

			@Override
			public int getSize() {
				return entries.length;
			}

			@Override
			public ProfileEntry getElementAt(int index) {
				return entries[index];
			}

			@Override
			public void addListDataListener(ListDataListener l) {
			}

			@Override
			public void removeListDataListener(ListDataListener l) {
			}

		});
		btnOk.setEnabled(entries.length != 0);
	}

	private void saveSettings(boolean saveActive) throws IOException {
		// Load config
		JsonObject conf = JsonParser.parseString(Files.readString(Path.of("edgelauncher.json"))).getAsJsonObject();
		JsonObject launchProfiles = new JsonObject();

		// Populate list
		for (String id : profiles.keySet())
			launchProfiles.add(id, profiles.get(id).profileData);

		// Add list
		conf.add("launchProfiles", launchProfiles);

		// Add active profile
		if (saveActive) {
			// Save
			conf.addProperty("activeProfile", profilesList.getSelectedValue().profileID);
			conf.addProperty("launchMode", "remote-client");
			EdgeEmulationSoftware.updating = false;
		} else if (conf.has("activeProfile") && !profiles.containsKey(conf.get("activeProfile").getAsString()))
			conf.remove("activeProfile");

		// Save
		Files.writeString(Path.of("edgelauncher.json"), conf.toString());
	}

	private void doSave(boolean saveActive) {
		try {
			saveSettings(saveActive);
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}

		// Close
		wasCancelled = false;
		dispose();
	}
}
