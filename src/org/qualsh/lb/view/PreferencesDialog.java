package org.qualsh.lb.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jxmapviewer.viewer.GeoPosition;
import org.qualsh.lb.MainWin;
import org.qualsh.lb.place.Place;
import org.qualsh.lb.util.Preferences;
import org.qualsh.lb.view.field.CoordinateTextField;

public class PreferencesDialog extends JDialog {

	private static final long serialVersionUID = -3263330177361727514L;

	private static final String[] THEME_NAMES = {
		"System Default",
		"Flat Light",
		"Flat Dark",
		"Flat IntelliJ",
		"Flat Darcula"
	};

	private static final String[] THEME_CLASSES = {
		"system",
		"com.formdev.flatlaf.FlatLightLaf",
		"com.formdev.flatlaf.FlatDarkLaf",
		"com.formdev.flatlaf.FlatIntelliJLaf",
		"com.formdev.flatlaf.FlatDarculaLaf"
	};

	private CoordinateTextField textLatitude;
	private CoordinateTextField textLongitude;
	private JButton btnSave;
	private JButton btnCancel;
	private JTextField textFindLocation;
	private JTextField textName;
	private JButton btnFindLocation;
	private JLabel lblCurrentLocation;
	private JButton btnReset;
	private LogInteraction logInteraction;
	private JComboBox<String> themeComboBox;
	private JTextField textDbPath;
	private JLabel lblCurrentDbPath;

	public PreferencesDialog(JFrame frame) {
		super(frame);

		this.setMinimumSize(new Dimension(420, 320));
		this.setTitle("Preferences");
		this.setModal(true);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int winW = (int) (screenSize.getWidth() * 0.3);
		int winH = (int) (screenSize.getHeight() * 0.3);
		setSize(winW, winH);

		int winX = (int) ((screenSize.getWidth() - getSize().getWidth()) / 2);
		int winY = (int) ((screenSize.getHeight() - getSize().getHeight()) / 2);
		setLocation(winX, winY);

		JPanel outerPanel = new JPanel(new BorderLayout(0, 5));
		outerPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(outerPanel, BorderLayout.CENTER);

		// Tabbed pane
		JTabbedPane tabbedPane = new JTabbedPane();
		outerPanel.add(tabbedPane, BorderLayout.CENTER);

		// ── Tab 1: Location ──────────────────────────────────────────────────
		JPanel locationTab = new JPanel(new BorderLayout());
		tabbedPane.addTab("Location", locationTab);

		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new CompoundBorder(
				new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
						"Your Location", TitledBorder.LEADING, TitledBorder.TOP, null, null),
				new EmptyBorder(5, 5, 5, 5)));
		locationTab.add(panel_1, BorderLayout.CENTER);

		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.columnWidths = new int[]{0, 0, 0};
		gbl_panel_1.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
		gbl_panel_1.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_panel_1.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		panel_1.setLayout(gbl_panel_1);

		JLabel lblCurrentLocation_1 = new JLabel("Current location");
		lblCurrentLocation_1.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblCurrentLocation_1 = new GridBagConstraints();
		gbc_lblCurrentLocation_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblCurrentLocation_1.gridx = 0;
		gbc_lblCurrentLocation_1.gridy = 0;
		panel_1.add(lblCurrentLocation_1, gbc_lblCurrentLocation_1);

		lblCurrentLocation = new JLabel("No location set.");
		GridBagConstraints gbc_lblCurrentLocation = new GridBagConstraints();
		gbc_lblCurrentLocation.anchor = GridBagConstraints.WEST;
		gbc_lblCurrentLocation.insets = new Insets(0, 0, 5, 0);
		gbc_lblCurrentLocation.gridx = 1;
		gbc_lblCurrentLocation.gridy = 0;
		panel_1.add(lblCurrentLocation, gbc_lblCurrentLocation);

		JLabel lblName = new JLabel("Name");
		lblName.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblName = new GridBagConstraints();
		gbc_lblName.insets = new Insets(0, 0, 5, 5);
		gbc_lblName.gridx = 0;
		gbc_lblName.gridy = 1;
		panel_1.add(lblName, gbc_lblName);

		textName = new JTextField();
		GridBagConstraints gbc_textName = new GridBagConstraints();
		gbc_textName.fill = GridBagConstraints.HORIZONTAL;
		gbc_textName.insets = new Insets(0, 0, 5, 0);
		gbc_textName.gridx = 1;
		gbc_textName.gridy = 1;
		panel_1.add(textName, gbc_textName);
		textName.setColumns(10);

		JLabel lblFindLocation = new JLabel("Find location");
		lblFindLocation.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblFindLocation = new GridBagConstraints();
		gbc_lblFindLocation.insets = new Insets(0, 0, 5, 5);
		gbc_lblFindLocation.gridx = 0;
		gbc_lblFindLocation.gridy = 2;
		panel_1.add(lblFindLocation, gbc_lblFindLocation);

		JPanel panel_4 = new JPanel();
		GridBagConstraints gbc_panel_4 = new GridBagConstraints();
		gbc_panel_4.insets = new Insets(0, 0, 5, 0);
		gbc_panel_4.fill = GridBagConstraints.BOTH;
		gbc_panel_4.gridx = 1;
		gbc_panel_4.gridy = 2;
		panel_1.add(panel_4, gbc_panel_4);
		GridBagLayout gbl_panel_4 = new GridBagLayout();
		gbl_panel_4.columnWidths = new int[]{0, 0, 0};
		gbl_panel_4.rowHeights = new int[]{0, 0};
		gbl_panel_4.columnWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
		gbl_panel_4.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		panel_4.setLayout(gbl_panel_4);

		textFindLocation = new JTextField();
		GridBagConstraints gbc_textFindLocation = new GridBagConstraints();
		gbc_textFindLocation.insets = new Insets(0, 0, 0, 5);
		gbc_textFindLocation.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFindLocation.gridx = 0;
		gbc_textFindLocation.gridy = 0;
		panel_4.add(textFindLocation, gbc_textFindLocation);
		textFindLocation.setColumns(10);

		btnFindLocation = new JButton("Find");
		btnFindLocation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String q = PreferencesDialog.this.getTextFindLocation().getText().trim();
				if (q.isEmpty()) return;
				PreferencesDialog.this.getLblCurrentLocation().setText("Searching…");
				new Thread(() -> {
					try {
						String encoded = URLEncoder.encode(q, StandardCharsets.UTF_8);
						String url = "https://nominatim.openstreetmap.org/search?q=" + encoded + "&format=json&limit=1";
						HttpClient client = HttpClient.newHttpClient();
						HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url))
								.header("User-Agent", "LogBook/1.0 radio-log-application").build();
						HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
						JSONArray results = (JSONArray) new JSONParser().parse(resp.body());
						if (results == null || results.isEmpty()) {
							SwingUtilities.invokeLater(() -> PreferencesDialog.this.getLblCurrentLocation().setText("No results found"));
							return;
						}
						JSONObject first = (JSONObject) results.get(0);
						String lat = String.format("%.5f", Double.parseDouble((String) first.get("lat")));
						String lon = String.format("%.5f", Double.parseDouble((String) first.get("lon")));
						String name = (String) first.get("display_name");
						String shortName = name != null ? name.split(",")[0] : "Found";
						SwingUtilities.invokeLater(() -> {
							PreferencesDialog.this.getTextLatitude().setText(lat);
							PreferencesDialog.this.getTextLongitude().setText(lon);
							PreferencesDialog.this.getLblCurrentLocation().setText(shortName);
							if (PreferencesDialog.this.getTextName().getText().trim().isEmpty()) {
								PreferencesDialog.this.getTextName().setText(shortName);
							}
						});
					} catch (Exception ex) {
						SwingUtilities.invokeLater(() -> PreferencesDialog.this.getLblCurrentLocation().setText("Search failed"));
					}
				}, "PrefDlg-geocode").start();
			}
		});
		GridBagConstraints gbc_btnFindLocation = new GridBagConstraints();
		gbc_btnFindLocation.gridx = 1;
		gbc_btnFindLocation.gridy = 0;
		panel_4.add(btnFindLocation, gbc_btnFindLocation);

		JLabel lblLatitude = new JLabel("Latitude");
		lblLatitude.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblLatitude = new GridBagConstraints();
		gbc_lblLatitude.insets = new Insets(0, 0, 5, 5);
		gbc_lblLatitude.gridx = 0;
		gbc_lblLatitude.gridy = 3;
		panel_1.add(lblLatitude, gbc_lblLatitude);

		textLatitude = new CoordinateTextField();
		GridBagConstraints gbc_textLatitude = new GridBagConstraints();
		gbc_textLatitude.fill = GridBagConstraints.HORIZONTAL;
		gbc_textLatitude.insets = new Insets(0, 0, 5, 0);
		gbc_textLatitude.gridx = 1;
		gbc_textLatitude.gridy = 3;
		panel_1.add(textLatitude, gbc_textLatitude);

		JLabel lblLongitude = new JLabel("Longitude");
		lblLongitude.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblLongitude = new GridBagConstraints();
		gbc_lblLongitude.insets = new Insets(0, 0, 5, 5);
		gbc_lblLongitude.gridx = 0;
		gbc_lblLongitude.gridy = 4;
		panel_1.add(lblLongitude, gbc_lblLongitude);

		textLongitude = new CoordinateTextField();
		GridBagConstraints gbc_textLongitude = new GridBagConstraints();
		gbc_textLongitude.insets = new Insets(0, 0, 5, 0);
		gbc_textLongitude.fill = GridBagConstraints.HORIZONTAL;
		gbc_textLongitude.gridx = 1;
		gbc_textLongitude.gridy = 4;
		panel_1.add(textLongitude, gbc_textLongitude);

		// Pick from map button
		JButton btnPickFromMap = new JButton("Pick from Map");
		btnPickFromMap.setToolTipText("Click on the map to set your RX location");
		GridBagConstraints gbc_btnPickFromMap = new GridBagConstraints();
		gbc_btnPickFromMap.anchor = GridBagConstraints.WEST;
		gbc_btnPickFromMap.insets = new Insets(0, 0, 5, 0);
		gbc_btnPickFromMap.gridx = 1;
		gbc_btnPickFromMap.gridy = 5;
		panel_1.add(btnPickFromMap, gbc_btnPickFromMap);
		btnPickFromMap.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (PreferencesDialog.this.getOwner() instanceof MainWin) {
					MainWin mw = (MainWin) PreferencesDialog.this.getOwner();
					MapPanel mp = mw.getMapPanel();
					PreferencesDialog.this.setVisible(false);
					mp.setPickingMode(true, pos -> {
						mp.setPickingMode(false, null);
						String lat = String.format("%.5f", pos.getLatitude());
						String lon = String.format("%.5f", pos.getLongitude());
						SwingUtilities.invokeLater(() -> {
							PreferencesDialog.this.getTextLatitude().setText(lat);
							PreferencesDialog.this.getTextLongitude().setText(lon);
							PreferencesDialog.this.getLblCurrentLocation().setText(lat + ", " + lon);
							if (PreferencesDialog.this.getTextName().getText().trim().isEmpty()) {
								PreferencesDialog.this.getTextName().setText(lat + ", " + lon);
							}
							PreferencesDialog.this.setVisible(true);
						});
					});
				}
			}
		});

		// reset user's location preference
		btnReset = new JButton("Reset");
		btnReset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Preferences.remove(Preferences.PREF_NAME_MY_PLACE);
				PreferencesDialog.this.getLblCurrentLocation().setText("No location set.");
				PreferencesDialog.this.getTextFindLocation().setText("");
				PreferencesDialog.this.getTextName().setText("");
				PreferencesDialog.this.getTextLatitude().setText("");
				PreferencesDialog.this.getTextLongitude().setText("");
			}
		});
		GridBagConstraints gbc_btnReset = new GridBagConstraints();
		gbc_btnReset.anchor = GridBagConstraints.EAST;
		gbc_btnReset.gridx = 1;
		gbc_btnReset.gridy = 6;
		panel_1.add(btnReset, gbc_btnReset);

		// ── Tab 2: Appearance ─────────────────────────────────────────────────
		JPanel appearanceTab = new JPanel(new BorderLayout());
		tabbedPane.addTab("Appearance", appearanceTab);

		JPanel themePanel = new JPanel();
		themePanel.setBorder(new CompoundBorder(
				new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
						"UI Look & Feel", TitledBorder.LEADING, TitledBorder.TOP, null, null),
				new EmptyBorder(10, 10, 10, 10)));
		appearanceTab.add(themePanel, BorderLayout.NORTH);

		GridBagLayout gbl_themePanel = new GridBagLayout();
		gbl_themePanel.columnWidths = new int[]{0, 0, 0};
		gbl_themePanel.rowHeights = new int[]{0, 0, 0};
		gbl_themePanel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_themePanel.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		themePanel.setLayout(gbl_themePanel);

		JLabel lblTheme = new JLabel("Theme");
		lblTheme.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblTheme = new GridBagConstraints();
		gbc_lblTheme.insets = new Insets(0, 0, 8, 10);
		gbc_lblTheme.gridx = 0;
		gbc_lblTheme.gridy = 0;
		themePanel.add(lblTheme, gbc_lblTheme);

		themeComboBox = new JComboBox<>(THEME_NAMES);
		GridBagConstraints gbc_themeComboBox = new GridBagConstraints();
		gbc_themeComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_themeComboBox.insets = new Insets(0, 0, 8, 0);
		gbc_themeComboBox.gridx = 1;
		gbc_themeComboBox.gridy = 0;
		themePanel.add(themeComboBox, gbc_themeComboBox);

		JLabel lblThemeNote = new JLabel("Theme changes apply immediately when saved.");
		lblThemeNote.setFont(new Font("Tahoma", Font.ITALIC, 10));
		GridBagConstraints gbc_lblThemeNote = new GridBagConstraints();
		gbc_lblThemeNote.anchor = GridBagConstraints.WEST;
		gbc_lblThemeNote.gridwidth = 2;
		gbc_lblThemeNote.gridx = 0;
		gbc_lblThemeNote.gridy = 1;
		themePanel.add(lblThemeNote, gbc_lblThemeNote);

		// ── Tab 3: General ────────────────────────────────────────────────────
		JPanel generalTab = new JPanel(new BorderLayout());
		tabbedPane.addTab("General", generalTab);

		JPanel dbPanel = new JPanel();
		dbPanel.setBorder(new CompoundBorder(
				new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
						"Database", TitledBorder.LEADING, TitledBorder.TOP, null, null),
				new EmptyBorder(10, 10, 10, 10)));
		generalTab.add(dbPanel, BorderLayout.NORTH);

		GridBagLayout gbl_dbPanel = new GridBagLayout();
		gbl_dbPanel.columnWidths = new int[]{0, 0, 0, 0};
		gbl_dbPanel.rowHeights = new int[]{0, 0, 0, 0};
		gbl_dbPanel.columnWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
		gbl_dbPanel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		dbPanel.setLayout(gbl_dbPanel);

		JLabel lblCurrentDb = new JLabel("Current");
		lblCurrentDb.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblCurrentDb = new GridBagConstraints();
		gbc_lblCurrentDb.insets = new Insets(0, 0, 8, 10);
		gbc_lblCurrentDb.gridx = 0;
		gbc_lblCurrentDb.gridy = 0;
		dbPanel.add(lblCurrentDb, gbc_lblCurrentDb);

		lblCurrentDbPath = new JLabel();
		lblCurrentDbPath.setFont(new Font("Tahoma", Font.PLAIN, 11));
		GridBagConstraints gbc_lblCurrentDbPath = new GridBagConstraints();
		gbc_lblCurrentDbPath.anchor = GridBagConstraints.WEST;
		gbc_lblCurrentDbPath.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblCurrentDbPath.insets = new Insets(0, 0, 8, 0);
		gbc_lblCurrentDbPath.gridwidth = 2;
		gbc_lblCurrentDbPath.gridx = 1;
		gbc_lblCurrentDbPath.gridy = 0;
		dbPanel.add(lblCurrentDbPath, gbc_lblCurrentDbPath);

		JLabel lblDbPath = new JLabel("Move to");
		lblDbPath.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblDbPath = new GridBagConstraints();
		gbc_lblDbPath.insets = new Insets(0, 0, 8, 10);
		gbc_lblDbPath.gridx = 0;
		gbc_lblDbPath.gridy = 1;
		dbPanel.add(lblDbPath, gbc_lblDbPath);

		textDbPath = new JTextField();
		GridBagConstraints gbc_textDbPath = new GridBagConstraints();
		gbc_textDbPath.fill = GridBagConstraints.HORIZONTAL;
		gbc_textDbPath.insets = new Insets(0, 0, 8, 5);
		gbc_textDbPath.gridx = 1;
		gbc_textDbPath.gridy = 1;
		dbPanel.add(textDbPath, gbc_textDbPath);

		JButton btnBrowseDb = new JButton("Browse…");
		GridBagConstraints gbc_btnBrowseDb = new GridBagConstraints();
		gbc_btnBrowseDb.insets = new Insets(0, 0, 8, 0);
		gbc_btnBrowseDb.gridx = 2;
		gbc_btnBrowseDb.gridy = 1;
		dbPanel.add(btnBrowseDb, gbc_btnBrowseDb);
		btnBrowseDb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser();
				fc.setDialogTitle("Migrate Database To…");
				fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				String current = textDbPath.getText().trim();
				if (!current.isEmpty()) {
					File f = new File(current);
					fc.setCurrentDirectory(f.getParentFile() != null ? f.getParentFile() : f);
					fc.setSelectedFile(f);
				}
				int result = fc.showSaveDialog(PreferencesDialog.this);
				if (result == JFileChooser.APPROVE_OPTION) {
					textDbPath.setText(fc.getSelectedFile().getAbsolutePath());
				}
			}
		});

		JLabel lblDbNote = new JLabel("Saving will copy your database to the new location immediately.");
		lblDbNote.setFont(new Font("Tahoma", Font.ITALIC, 10));
		GridBagConstraints gbc_lblDbNote = new GridBagConstraints();
		gbc_lblDbNote.anchor = GridBagConstraints.WEST;
		gbc_lblDbNote.gridwidth = 3;
		gbc_lblDbNote.gridx = 0;
		gbc_lblDbNote.gridy = 2;
		dbPanel.add(lblDbNote, gbc_lblDbNote);

		// ── Save / Cancel buttons (shared across tabs) ────────────────────────
		JPanel panel_3 = new JPanel();
		outerPanel.add(panel_3, BorderLayout.SOUTH);

		btnSave = new JButton("Save");
		this.getBtnSave().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PreferencesDialog.this.save();
				PreferencesDialog.this.setVisible(false);
			}
		});
		panel_3.add(btnSave);

		btnCancel = new JButton("Cancel");
		this.getBtnCancel().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PreferencesDialog.this.setVisible(false);
			}
		});
		panel_3.add(btnCancel);

		this.addWindowListener(new WindowListener() {

			public void windowOpened(WindowEvent e) {
				System.out.println("Preferences dialog opened");
				String pref = Preferences.getOne(Preferences.PREF_NAME_MY_PLACE);
				System.out.println("Preferences dialog was opened; User preferences; my_place ID: " + pref);
				if (pref != null) {
					int placeId = Integer.parseInt(pref);
					Place place = Place.getOne(placeId);
					PreferencesDialog.this.getLblCurrentLocation().setText(place.getPlaceName());
					PreferencesDialog.this.getTextName().setText(place.getPlaceName());
					PreferencesDialog.this.getTextLatitude().setText(place.getLatitude());
					PreferencesDialog.this.getTextLongitude().setText(place.getLongitude());
				}
				// Initialize theme combo to saved value
				String savedTheme = Preferences.getOne(Preferences.PREF_NAME_THEME);
				if (savedTheme != null) {
					for (int i = 0; i < THEME_CLASSES.length; i++) {
						if (THEME_CLASSES[i].equals(savedTheme)) {
							themeComboBox.setSelectedIndex(i);
							break;
						}
					}
				} else {
					themeComboBox.setSelectedIndex(0); // System Default
				}
				// Initialize DB path fields
				String activeDbPath = org.qualsh.lb.data.Data.getDbPath();
				lblCurrentDbPath.setText(activeDbPath);
				textDbPath.setText(activeDbPath);
			}

			public void windowClosing(WindowEvent e) {
				System.out.println("Preferences dialog closing");
			}

			public void windowClosed(WindowEvent e) {
				System.out.println("Preferences dialog closed");
			}

			public void windowIconified(WindowEvent e) {
				System.out.println("Preferences dialog iconified");
			}

			public void windowDeiconified(WindowEvent e) {
				System.out.println("Preferences dialog deiconified");
			}

			public void windowActivated(WindowEvent e) {
				System.out.println("Preferences dialog activated");
			}

			public void windowDeactivated(WindowEvent e) {
				System.out.println("Preferences dialog deactivated");
				String pref = Preferences.getOne(Preferences.PREF_NAME_MY_PLACE);
				System.out.println("Preferences dialog is closing; User preferences; my_place ID: " + pref);
				if (pref != null) {
					int placeId = Integer.parseInt(pref);
					Place place = Place.getOne(placeId);
					PreferencesDialog.this.getLogInteraction().getLblPlaceName().setText(place.getPlaceName());
					PreferencesDialog.this.getLogInteraction().getLblRxCoordinates().setText(place.getLatitude() + ", " + place.getLongitude());
				} else {
					PreferencesDialog.this.getLogInteraction().getLblPlaceName().setText("No location set.");
					PreferencesDialog.this.getLogInteraction().getLblRxCoordinates().setText("");
				}
			}

		});
	}

	protected void save() {
		this.saveMyPlace();
		this.saveTheme();
		this.saveDbPath();
		if (logInteraction != null) {
			logInteraction.getLocationsTab().refreshList();
		}
	}

	private void saveDbPath() {
		String path = textDbPath.getText().trim();
		if (path.isEmpty()) return;
		String current = org.qualsh.lb.data.Data.getDbPath();
		if (path.equals(current)) return;
		boolean success = org.qualsh.lb.data.Data.migrateDatabase(path);
		if (success) {
			lblCurrentDbPath.setText(path);
			JOptionPane.showMessageDialog(
				this,
				"Database migrated to:\n" + path,
				"Migration Complete",
				JOptionPane.INFORMATION_MESSAGE
			);
		} else {
			textDbPath.setText(current);
			JOptionPane.showMessageDialog(
				this,
				"Failed to migrate database to the new location.\nThe original database is still in use.",
				"Migration Failed",
				JOptionPane.ERROR_MESSAGE
			);
		}
	}

	private void saveMyPlace() {
		if (!this.getTextLatitude().getText().isEmpty() &&
				!this.getTextLongitude().getText().isEmpty()) {
			String name = this.getTextName().getText().trim();
			if (name.isEmpty()) {
				name = this.getLblCurrentLocation().getText();
			}
			Place place = new Place();
			place.setLatitude(this.getTextLatitude().getText());
			place.setLongitude(this.getTextLongitude().getText());
			place.setPlaceName(name);
			int placeId = place.insert();
			place.setId(placeId);
			Preferences.saveMyPlace(place);
		}
	}

	private void saveTheme() {
		int idx = themeComboBox.getSelectedIndex();
		String themeClass = THEME_CLASSES[idx];
		Preferences.save(Preferences.PREF_NAME_THEME, themeClass);
		try {
			if ("system".equals(themeClass)) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} else {
				UIManager.setLookAndFeel(themeClass);
			}
			for (Window w : Window.getWindows()) {
				SwingUtilities.updateComponentTreeUI(w);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public CoordinateTextField getTextLatitude() {
		return textLatitude;
	}

	public void setTextLatitude(CoordinateTextField textLatitude) {
		this.textLatitude = textLatitude;
	}

	public CoordinateTextField getTextLongitude() {
		return textLongitude;
	}

	public void setTextLongitude(CoordinateTextField textLongitude) {
		this.textLongitude = textLongitude;
	}

	public JButton getBtnSave() {
		return btnSave;
	}

	public void setBtnSave(JButton btnSave) {
		this.btnSave = btnSave;
	}

	public JButton getBtnCancel() {
		return btnCancel;
	}

	public void setBtnCancel(JButton btnCancel) {
		this.btnCancel = btnCancel;
	}

	public JTextField getTextFindLocation() {
		return textFindLocation;
	}

	public void setTextFindLocation(JTextField textFindLocation) {
		this.textFindLocation = textFindLocation;
	}

	public JButton getBtnFindLocation() {
		return btnFindLocation;
	}

	public void setBtnFindLocation(JButton btnFindLocation) {
		this.btnFindLocation = btnFindLocation;
	}

	public JLabel getLblCurrentLocation() {
		return lblCurrentLocation;
	}

	public void setLblCurrentLocation(JLabel lblCurrentLocation) {
		this.lblCurrentLocation = lblCurrentLocation;
	}

	public JButton getBtnReset() {
		return btnReset;
	}

	public void setBtnReset(JButton btnReset) {
		this.btnReset = btnReset;
	}

	public LogInteraction getLogInteraction() {
		return logInteraction;
	}

	public void setLogInteraction(LogInteraction logInteraction) {
		this.logInteraction = logInteraction;
	}

	public JTextField getTextName() {
		return textName;
	}

	public void setTextName(JTextField textName) {
		this.textName = textName;
	}

}
