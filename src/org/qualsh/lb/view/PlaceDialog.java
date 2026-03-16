package org.qualsh.lb.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.qualsh.lb.MainWin;
import org.qualsh.lb.place.Place;
import org.qualsh.lb.view.field.CoordinateTextField;

/**
 * Dialog for creating or editing a user station location (Place).
 * Supports manual coordinate entry, geocode search, and map picking.
 */
public class PlaceDialog extends JDialog {

	private static final long serialVersionUID = 3912748361927483619L;

	private JTextField textName;
	private CoordinateTextField textLatitude;
	private CoordinateTextField textLongitude;
	private JTextField textGeoSearch;
	private JLabel lblGeoStatus;
	private Place resultPlace = null;
	private final Place editingPlace;

	/**
	 * @param frame        owner frame
	 * @param editingPlace existing Place to edit, or null to create a new one
	 */
	public PlaceDialog(JFrame frame, Place editingPlace) {
		super(frame, editingPlace == null ? "Add My Location" : "Edit My Location", true);
		this.editingPlace = editingPlace;

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int winW = (int) (screenSize.getWidth() * 0.28);
		int winH = (int) (screenSize.getHeight() * 0.35);
		setSize(Math.max(winW, 360), Math.max(winH, 300));
		setLocation(
			(int) ((screenSize.getWidth() - getWidth()) / 2),
			(int) ((screenSize.getHeight() - getHeight()) / 2)
		);
		setMinimumSize(new Dimension(340, 280));

		JPanel root = new JPanel(new BorderLayout(0, 0));
		root.setBorder(new EmptyBorder(8, 8, 8, 8));
		getContentPane().add(root, BorderLayout.CENTER);

		GridBagLayout gbl = new GridBagLayout();
		gbl.columnWidths = new int[]{0, 0, 0};
		gbl.rowHeights = new int[]{0, 0, 0, 0};
		gbl.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl.rowWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
		root.setLayout(gbl);

		// --- Name ---
		JLabel lblName = new JLabel("Name");
		lblName.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbcLblName = new GridBagConstraints();
		gbcLblName.anchor = GridBagConstraints.EAST;
		gbcLblName.insets = new Insets(0, 0, 8, 6);
		gbcLblName.gridx = 0;
		gbcLblName.gridy = 0;
		root.add(lblName, gbcLblName);

		textName = new JTextField();
		GridBagConstraints gbcName = new GridBagConstraints();
		gbcName.fill = GridBagConstraints.HORIZONTAL;
		gbcName.insets = new Insets(0, 0, 8, 0);
		gbcName.gridx = 1;
		gbcName.gridy = 0;
		root.add(textName, gbcName);

		// --- Coordinates panel ---
		JPanel coordPanel = new JPanel();
		coordPanel.setBorder(new CompoundBorder(
			new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
				"Coordinates", TitledBorder.LEADING, TitledBorder.TOP,
				new Font("Tahoma", Font.BOLD, 11), null),
			new EmptyBorder(4, 6, 6, 6)));
		GridBagConstraints gbcCoord = new GridBagConstraints();
		gbcCoord.gridwidth = 2;
		gbcCoord.fill = GridBagConstraints.BOTH;
		gbcCoord.insets = new Insets(0, 0, 8, 0);
		gbcCoord.gridx = 0;
		gbcCoord.gridy = 1;
		root.add(coordPanel, gbcCoord);

		GridBagLayout gblCoord = new GridBagLayout();
		gblCoord.columnWidths = new int[]{0, 0, 0, 0};
		gblCoord.rowHeights = new int[]{0, 0, 0, 0, 0};
		gblCoord.columnWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
		gblCoord.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		coordPanel.setLayout(gblCoord);

		// Search row
		JLabel lblSearch = new JLabel("Search");
		GridBagConstraints gbcLblSearch = new GridBagConstraints();
		gbcLblSearch.anchor = GridBagConstraints.EAST;
		gbcLblSearch.insets = new Insets(0, 0, 4, 5);
		gbcLblSearch.gridx = 0;
		gbcLblSearch.gridy = 0;
		coordPanel.add(lblSearch, gbcLblSearch);

		textGeoSearch = new JTextField();
		textGeoSearch.setToolTipText("Enter a place name to look up its coordinates");
		GridBagConstraints gbcGeoSearch = new GridBagConstraints();
		gbcGeoSearch.insets = new Insets(0, 0, 4, 5);
		gbcGeoSearch.fill = GridBagConstraints.HORIZONTAL;
		gbcGeoSearch.gridx = 1;
		gbcGeoSearch.gridy = 0;
		coordPanel.add(textGeoSearch, gbcGeoSearch);

		JButton btnFind = new JButton("Find");
		GridBagConstraints gbcFind = new GridBagConstraints();
		gbcFind.insets = new Insets(0, 0, 4, 0);
		gbcFind.gridx = 2;
		gbcFind.gridy = 0;
		coordPanel.add(btnFind, gbcFind);

		// Status label
		lblGeoStatus = new JLabel(" ");
		lblGeoStatus.setFont(new Font("Tahoma", Font.ITALIC, 10));
		GridBagConstraints gbcStatus = new GridBagConstraints();
		gbcStatus.gridwidth = 3;
		gbcStatus.anchor = GridBagConstraints.WEST;
		gbcStatus.insets = new Insets(0, 0, 4, 0);
		gbcStatus.gridx = 0;
		gbcStatus.gridy = 1;
		coordPanel.add(lblGeoStatus, gbcStatus);

		// Pick from Map button
		JButton btnPickMap = new JButton("Pick from Map");
		btnPickMap.setToolTipText("Click a point on the map to set coordinates");
		GridBagConstraints gbcPickMap = new GridBagConstraints();
		gbcPickMap.gridwidth = 3;
		gbcPickMap.anchor = GridBagConstraints.WEST;
		gbcPickMap.insets = new Insets(0, 0, 4, 0);
		gbcPickMap.gridx = 0;
		gbcPickMap.gridy = 2;
		coordPanel.add(btnPickMap, gbcPickMap);

		// Latitude
		JLabel lblLat = new JLabel("Latitude");
		GridBagConstraints gbcLblLat = new GridBagConstraints();
		gbcLblLat.anchor = GridBagConstraints.EAST;
		gbcLblLat.insets = new Insets(0, 0, 4, 5);
		gbcLblLat.gridx = 0;
		gbcLblLat.gridy = 3;
		coordPanel.add(lblLat, gbcLblLat);

		textLatitude = new CoordinateTextField();
		GridBagConstraints gbcLat = new GridBagConstraints();
		gbcLat.gridwidth = 2;
		gbcLat.fill = GridBagConstraints.HORIZONTAL;
		gbcLat.insets = new Insets(0, 0, 4, 0);
		gbcLat.gridx = 1;
		gbcLat.gridy = 3;
		coordPanel.add(textLatitude, gbcLat);

		// Longitude
		JLabel lblLon = new JLabel("Longitude");
		GridBagConstraints gbcLblLon = new GridBagConstraints();
		gbcLblLon.anchor = GridBagConstraints.EAST;
		gbcLblLon.insets = new Insets(0, 0, 0, 5);
		gbcLblLon.gridx = 0;
		gbcLblLon.gridy = 4;
		coordPanel.add(lblLon, gbcLblLon);

		textLongitude = new CoordinateTextField();
		((CoordinateTextField) textLongitude).setIsLongitude(true);
		GridBagConstraints gbcLon = new GridBagConstraints();
		gbcLon.gridwidth = 2;
		gbcLon.fill = GridBagConstraints.HORIZONTAL;
		gbcLon.gridx = 1;
		gbcLon.gridy = 4;
		coordPanel.add(textLongitude, gbcLon);

		// --- Buttons ---
		JPanel btnPanel = new JPanel();
		GridBagConstraints gbcBtnPanel = new GridBagConstraints();
		gbcBtnPanel.gridwidth = 2;
		gbcBtnPanel.anchor = GridBagConstraints.EAST;
		gbcBtnPanel.gridx = 0;
		gbcBtnPanel.gridy = 2;
		root.add(btnPanel, gbcBtnPanel);

		JButton btnSave = new JButton("Save");
		JButton btnCancel = new JButton("Cancel");
		btnPanel.add(btnSave);
		btnPanel.add(btnCancel);

		// Pre-fill if editing
		if (editingPlace != null) {
			textName.setText(editingPlace.getPlaceName());
			if (editingPlace.getLatitude() != null) textLatitude.setText(editingPlace.getLatitude());
			if (editingPlace.getLongitude() != null) textLongitude.setText(editingPlace.getLongitude());
		}

		// --- Geocode search action ---
		Runnable doSearch = () -> {
			String q = textGeoSearch.getText().trim();
			if (q.isEmpty()) return;
			SwingUtilities.invokeLater(() -> lblGeoStatus.setText("Searching\u2026"));
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
						SwingUtilities.invokeLater(() -> lblGeoStatus.setText("No results found"));
						return;
					}
					JSONObject first = (JSONObject) results.get(0);
					String lat = String.format("%.5f", Double.parseDouble((String) first.get("lat")));
					String lon = String.format("%.5f", Double.parseDouble((String) first.get("lon")));
					String name = (String) first.get("display_name");
					String shortName = name != null ? name.split(",")[0] : "Found";
					SwingUtilities.invokeLater(() -> {
						textLatitude.setText(lat);
						textLongitude.setText(lon);
						lblGeoStatus.setText(shortName);
					});
				} catch (Exception ex) {
					SwingUtilities.invokeLater(() -> lblGeoStatus.setText("Search failed"));
				}
			}, "PlaceDlg-geocode").start();
		};

		btnFind.addActionListener(e -> doSearch.run());
		textGeoSearch.addActionListener(e -> doSearch.run());

		// --- Pick from Map action ---
		btnPickMap.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (PlaceDialog.this.getOwner() instanceof MainWin) {
					MainWin mw = (MainWin) PlaceDialog.this.getOwner();
					MapPanel mp = mw.getMapPanel();
					PlaceDialog.this.setVisible(false);
					mp.setPickingMode(true, pos -> {
						mp.setPickingMode(false, null);
						String lat = String.format("%.5f", pos.getLatitude());
						String lon = String.format("%.5f", pos.getLongitude());
						SwingUtilities.invokeLater(() -> {
							textLatitude.setText(lat);
							textLongitude.setText(lon);
							lblGeoStatus.setText(lat + ", " + lon);
							PlaceDialog.this.setVisible(true);
						});
					});
				}
			}
		});

		// --- Save action ---
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String name = textName.getText().trim();
				String lat = textLatitude.getText().trim();
				String lon = textLongitude.getText().trim();

				if (name.isEmpty()) {
					lblGeoStatus.setText("A name is required.");
					return;
				}
				if (lat.isEmpty() || lon.isEmpty()) {
					lblGeoStatus.setText("Coordinates are required.");
					return;
				}

				Place p = (editingPlace != null) ? editingPlace : new Place();
				p.setPlaceName(name);
				p.setLatitude(lat);
				p.setLongitude(lon);

				if (editingPlace != null) {
					p.update();
					resultPlace = p;
				} else {
					int newId = p.insert();
					p.setId(newId);
					resultPlace = p;
				}

				PlaceDialog.this.setVisible(false);
			}
		});

		// --- Cancel action ---
		btnCancel.addActionListener(e -> PlaceDialog.this.setVisible(false));
	}

	/** Returns the saved/updated Place, or null if the dialog was cancelled. */
	public Place getResultPlace() {
		return resultPlace;
	}

}
