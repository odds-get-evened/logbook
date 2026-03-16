package org.qualsh.lb.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.qualsh.lb.MainWin;
import org.qualsh.lb.TextDocument;
import org.qualsh.lb.data.Data;
import org.qualsh.lb.data.LanguageListModel;
import org.qualsh.lb.data.StationComboModel;
import org.qualsh.lb.data.ViewLocationsModel;
import org.qualsh.lb.data.ViewStationsModel;
import org.qualsh.lb.language.Language;
import org.qualsh.lb.location.Location;
import org.qualsh.lb.station.Station;
import org.qualsh.lb.util.FormError;
import org.qualsh.lb.util.Utilities;
import org.qualsh.lb.view.field.CoordinateTextField;
import org.qualsh.lb.view.field.FrequencyTextField;
import org.qualsh.lb.view.field.GenericTimeField;

import org.jxmapviewer.viewer.GeoPosition;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import javax.swing.SwingUtilities;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class NewLocationDialog extends JDialog {

	private static final long serialVersionUID = 5688646621779509239L;
	private JTextField textName;
	private JLabel lblStation;
	private JLabel lblName;
	private JLabel lblFrequency;
	private FrequencyTextField textFrequency;
	private JPanel panel_1;
	private JLabel lblStart;
	private JTextField textStartTime;
	private JLabel lblStop;
	private JTextField textStopTime;
	private JPanel panel_2;
	private JLabel lblLatitude;
	private JLabel lblLongitude;
	private JTextField textLatitude;
	private JTextField textLongitude;
	private JPanel panel_3;
	private JButton btnOK;
	private JButton btnCancel;
	private JComboBox<Station> comboBoxStations;
	private JLabel lblLanguage;
	private JComboBox<Language> comboLanguage;
	private JPanel errorPanel;

	public NewLocationDialog(JFrame frame) {
		super(frame);
		
		this.setMinimumSize(new Dimension(300, 400));
		this.setTitle("New Location");
		this.setModal(true);
		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int winW = (int) (screenSize.getWidth() * 0.25);
		int winH = (int) (screenSize.getHeight() * 0.3);
		setSize(winW, winH);
		
		int winX = (int) ((screenSize.getWidth() - getSize().getWidth()) / 2);
		int winY = (int) ((screenSize.getHeight() - getSize().getHeight()) / 2);
		setLocation(winX, winY);
		
		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(panel, BorderLayout.CENTER);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{0, 0, 0};
		gbl_panel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
		gbl_panel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, Double.MIN_VALUE};
		panel.setLayout(gbl_panel);
		
		errorPanel = new JPanel();
		errorPanel.setVisible(false);
		GridBagConstraints gbc_errorPanel = new GridBagConstraints();
		gbc_errorPanel.insets = new Insets(0, 0, 5, 0);
		gbc_errorPanel.fill = GridBagConstraints.BOTH;
		gbc_errorPanel.gridx = 1;
		gbc_errorPanel.gridy = 0;
		panel.add(errorPanel, gbc_errorPanel);
		
		lblStation = new JLabel("Station");
		lblStation.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblStation = new GridBagConstraints();
		gbc_lblStation.anchor = GridBagConstraints.EAST;
		gbc_lblStation.insets = new Insets(0, 0, 5, 5);
		gbc_lblStation.gridx = 0;
		gbc_lblStation.gridy = 1;
		panel.add(lblStation, gbc_lblStation);
		
		comboBoxStations = new JComboBox<Station>();
		StationComboModel stnComboModel = new StationComboModel();
		stnComboModel.setAllStations();
		comboBoxStations.setModel(stnComboModel);
		comboBoxStations.setSelectedIndex(0);
		GridBagConstraints gbc_comboBoxStations = new GridBagConstraints();
		gbc_comboBoxStations.insets = new Insets(0, 0, 5, 0);
		gbc_comboBoxStations.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxStations.gridx = 1;
		gbc_comboBoxStations.gridy = 1;
		panel.add(comboBoxStations, gbc_comboBoxStations);
		
		lblName = new JLabel("Name");
		lblName.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblName = new GridBagConstraints();
		gbc_lblName.insets = new Insets(0, 0, 5, 5);
		gbc_lblName.anchor = GridBagConstraints.EAST;
		gbc_lblName.gridx = 0;
		gbc_lblName.gridy = 2;
		panel.add(lblName, gbc_lblName);
		
		textName = new JTextField();
		GridBagConstraints gbc_textName = new GridBagConstraints();
		gbc_textName.insets = new Insets(0, 0, 5, 0);
		gbc_textName.fill = GridBagConstraints.HORIZONTAL;
		gbc_textName.gridx = 1;
		gbc_textName.gridy = 2;
		panel.add(textName, gbc_textName);
		textName.setColumns(10);
		
		lblFrequency = new JLabel("Frequency");
		lblFrequency.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblFrequency = new GridBagConstraints();
		gbc_lblFrequency.anchor = GridBagConstraints.EAST;
		gbc_lblFrequency.insets = new Insets(0, 0, 5, 5);
		gbc_lblFrequency.gridx = 0;
		gbc_lblFrequency.gridy = 3;
		panel.add(lblFrequency, gbc_lblFrequency);
		
		textFrequency = new FrequencyTextField();
		GridBagConstraints gbc_textFrequency = new GridBagConstraints();
		gbc_textFrequency.insets = new Insets(0, 0, 5, 0);
		gbc_textFrequency.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFrequency.gridx = 1;
		gbc_textFrequency.gridy = 3;
		panel.add(textFrequency, gbc_textFrequency);
		
		lblLanguage = new JLabel("Language");
		lblLanguage.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblLanguage = new GridBagConstraints();
		gbc_lblLanguage.anchor = GridBagConstraints.EAST;
		gbc_lblLanguage.insets = new Insets(0, 0, 5, 5);
		gbc_lblLanguage.gridx = 0;
		gbc_lblLanguage.gridy = 4;
		panel.add(lblLanguage, gbc_lblLanguage);
		
		comboLanguage = new JComboBox<Language>();
		LanguageListModel languageModel = new LanguageListModel();
		languageModel.setAllLanguages();
		comboLanguage.setModel(languageModel);
		comboLanguage.setSelectedIndex(0);
		GridBagConstraints gbc_comboLanguage = new GridBagConstraints();
		gbc_comboLanguage.insets = new Insets(0, 0, 5, 0);
		gbc_comboLanguage.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboLanguage.gridx = 1;
		gbc_comboLanguage.gridy = 4;
		panel.add(comboLanguage, gbc_comboLanguage);
		
		panel_1 = new JPanel();
		panel_1.setBorder(new CompoundBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Times", TitledBorder.LEADING, TitledBorder.TOP, new Font("Tahoma", Font.BOLD, 11), null), new EmptyBorder(5, 5, 5, 5)));
		GridBagConstraints gbc_panel_1 = new GridBagConstraints();
		gbc_panel_1.insets = new Insets(0, 0, 5, 0);
		gbc_panel_1.fill = GridBagConstraints.BOTH;
		gbc_panel_1.gridx = 1;
		gbc_panel_1.gridy = 5;
		panel.add(panel_1, gbc_panel_1);
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.columnWidths = new int[]{0, 0, 0};
		gbl_panel_1.rowHeights = new int[]{0, 0, 0};
		gbl_panel_1.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_panel_1.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		panel_1.setLayout(gbl_panel_1);
		
		lblStart = new JLabel("Start");
		GridBagConstraints gbc_lblStart = new GridBagConstraints();
		gbc_lblStart.insets = new Insets(0, 0, 5, 5);
		gbc_lblStart.anchor = GridBagConstraints.EAST;
		gbc_lblStart.gridx = 0;
		gbc_lblStart.gridy = 0;
		panel_1.add(lblStart, gbc_lblStart);
		
		textStartTime = new GenericTimeField();
		textStartTime.setText("00:00");
		GridBagConstraints gbc_textStartTime = new GridBagConstraints();
		gbc_textStartTime.insets = new Insets(0, 0, 5, 0);
		gbc_textStartTime.fill = GridBagConstraints.HORIZONTAL;
		gbc_textStartTime.gridx = 1;
		gbc_textStartTime.gridy = 0;
		panel_1.add(textStartTime, gbc_textStartTime);
		textStartTime.setColumns(10);
		
		lblStop = new JLabel("Stop");
		GridBagConstraints gbc_lblStop = new GridBagConstraints();
		gbc_lblStop.anchor = GridBagConstraints.EAST;
		gbc_lblStop.insets = new Insets(0, 0, 0, 5);
		gbc_lblStop.gridx = 0;
		gbc_lblStop.gridy = 1;
		panel_1.add(lblStop, gbc_lblStop);
		
		textStopTime = new GenericTimeField();
		textStopTime.setText("00:00");
		GridBagConstraints gbc_textStopTime = new GridBagConstraints();
		gbc_textStopTime.fill = GridBagConstraints.HORIZONTAL;
		gbc_textStopTime.gridx = 1;
		gbc_textStopTime.gridy = 1;
		panel_1.add(textStopTime, gbc_textStopTime);
		textStopTime.setColumns(10);
		
		panel_2 = new JPanel();
		panel_2.setBorder(new CompoundBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Coordinates", TitledBorder.LEADING, TitledBorder.TOP, new Font("Tahoma", Font.BOLD, 11), null), new EmptyBorder(5, 5, 5, 5)));
		GridBagConstraints gbc_panel_2 = new GridBagConstraints();
		gbc_panel_2.insets = new Insets(0, 0, 5, 0);
		gbc_panel_2.fill = GridBagConstraints.BOTH;
		gbc_panel_2.gridx = 1;
		gbc_panel_2.gridy = 6;
		panel.add(panel_2, gbc_panel_2);
		GridBagLayout gbl_panel_2 = new GridBagLayout();
		gbl_panel_2.columnWidths = new int[]{0, 0, 0, 0};
		gbl_panel_2.rowHeights = new int[]{0, 0, 0, 0, 0};
		gbl_panel_2.columnWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
		gbl_panel_2.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		panel_2.setLayout(gbl_panel_2);

		// Geocode search row
		JLabel lblSearchCoord = new JLabel("Search");
		GridBagConstraints gbc_lblSearchCoord = new GridBagConstraints();
		gbc_lblSearchCoord.anchor = GridBagConstraints.EAST;
		gbc_lblSearchCoord.insets = new Insets(0, 0, 5, 5);
		gbc_lblSearchCoord.gridx = 0;
		gbc_lblSearchCoord.gridy = 0;
		panel_2.add(lblSearchCoord, gbc_lblSearchCoord);

		JTextField textGeoSearch = new JTextField();
		textGeoSearch.setToolTipText("Enter a place name to look up its coordinates");
		GridBagConstraints gbc_textGeoSearch = new GridBagConstraints();
		gbc_textGeoSearch.insets = new Insets(0, 0, 5, 5);
		gbc_textGeoSearch.fill = GridBagConstraints.HORIZONTAL;
		gbc_textGeoSearch.gridx = 1;
		gbc_textGeoSearch.gridy = 0;
		panel_2.add(textGeoSearch, gbc_textGeoSearch);
		textGeoSearch.setColumns(10);

		JButton btnGeoSearch = new JButton("Find");
		GridBagConstraints gbc_btnGeoSearch = new GridBagConstraints();
		gbc_btnGeoSearch.insets = new Insets(0, 0, 5, 0);
		gbc_btnGeoSearch.gridx = 2;
		gbc_btnGeoSearch.gridy = 0;
		panel_2.add(btnGeoSearch, gbc_btnGeoSearch);

		JLabel lblGeoStatus = new JLabel(" ");
		lblGeoStatus.setFont(new Font("Tahoma", Font.ITALIC, 10));
		GridBagConstraints gbc_lblGeoStatus = new GridBagConstraints();
		gbc_lblGeoStatus.gridwidth = 3;
		gbc_lblGeoStatus.anchor = GridBagConstraints.WEST;
		gbc_lblGeoStatus.insets = new Insets(0, 0, 5, 0);
		gbc_lblGeoStatus.gridx = 0;
		gbc_lblGeoStatus.gridy = 1;
		panel_2.add(lblGeoStatus, gbc_lblGeoStatus);

		Runnable doGeoSearch = () -> {
			String q = textGeoSearch.getText().trim();
			if (q.isEmpty()) return;
			SwingUtilities.invokeLater(() -> lblGeoStatus.setText("Searching…"));
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
					String lat = (String) first.get("lat");
					String lon = (String) first.get("lon");
					String name = (String) first.get("display_name");
					String shortName = name != null ? name.split(",")[0] : "Found";
					SwingUtilities.invokeLater(() -> {
						textLatitude.setText(String.format("%.5f", Double.parseDouble(lat)));
						textLongitude.setText(String.format("%.5f", Double.parseDouble(lon)));
						lblGeoStatus.setText(shortName);
					});
				} catch (Exception ex) {
					SwingUtilities.invokeLater(() -> lblGeoStatus.setText("Search failed: " + ex.getMessage()));
				}
			}, "NewLocDlg-geocode").start();
		};
		btnGeoSearch.addActionListener(e -> doGeoSearch.run());
		textGeoSearch.addActionListener(e -> doGeoSearch.run());

		// Pick from map button
		JButton btnPickFromMap = new JButton("Pick from Map");
		btnPickFromMap.setToolTipText("Click on the map to set coordinates");
		GridBagConstraints gbc_btnPickFromMap = new GridBagConstraints();
		gbc_btnPickFromMap.gridwidth = 3;
		gbc_btnPickFromMap.anchor = GridBagConstraints.WEST;
		gbc_btnPickFromMap.insets = new Insets(0, 0, 5, 0);
		gbc_btnPickFromMap.gridx = 0;
		gbc_btnPickFromMap.gridy = 2;
		panel_2.add(btnPickFromMap, gbc_btnPickFromMap);
		btnPickFromMap.addActionListener(e -> {
			if (NewLocationDialog.this.getOwner() instanceof MainWin) {
				MainWin mw = (MainWin) NewLocationDialog.this.getOwner();
				MapPanel mp = mw.getMapPanel();
				NewLocationDialog.this.setVisible(false);
				mp.setPickingMode(true, pos -> {
					mp.setPickingMode(false, null);
					textLatitude.setText(String.format("%.5f", pos.getLatitude()));
					textLongitude.setText(String.format("%.5f", pos.getLongitude()));
					lblGeoStatus.setText(String.format("%.4f°, %.4f°", pos.getLatitude(), pos.getLongitude()));
					SwingUtilities.invokeLater(() -> NewLocationDialog.this.setVisible(true));
				});
			}
		});

		lblLatitude = new JLabel("Latitude");
		GridBagConstraints gbc_lblLatitude = new GridBagConstraints();
		gbc_lblLatitude.anchor = GridBagConstraints.EAST;
		gbc_lblLatitude.insets = new Insets(0, 0, 5, 5);
		gbc_lblLatitude.gridx = 0;
		gbc_lblLatitude.gridy = 3;
		panel_2.add(lblLatitude, gbc_lblLatitude);
		
		textLatitude = new CoordinateTextField();
		textLatitude.setDocument(new TextDocument(10));
		GridBagConstraints gbc_textLatitude = new GridBagConstraints();
		gbc_textLatitude.gridwidth = 2;
		gbc_textLatitude.insets = new Insets(0, 0, 5, 0);
		gbc_textLatitude.fill = GridBagConstraints.HORIZONTAL;
		gbc_textLatitude.gridx = 1;
		gbc_textLatitude.gridy = 3;
		panel_2.add(textLatitude, gbc_textLatitude);
		textLatitude.setColumns(10);
		
		lblLongitude = new JLabel("Longitude");
		GridBagConstraints gbc_lblLongitude = new GridBagConstraints();
		gbc_lblLongitude.anchor = GridBagConstraints.EAST;
		gbc_lblLongitude.insets = new Insets(0, 0, 0, 5);
		gbc_lblLongitude.gridx = 0;
		gbc_lblLongitude.gridy = 4;
		panel_2.add(lblLongitude, gbc_lblLongitude);
		
		textLongitude = new CoordinateTextField();
		((CoordinateTextField) textLongitude).setIsLongitude(true);
		textLongitude.setDocument(new TextDocument(11));
		GridBagConstraints gbc_textLongitude = new GridBagConstraints();
		gbc_textLongitude.gridwidth = 2;
		gbc_textLongitude.fill = GridBagConstraints.HORIZONTAL;
		gbc_textLongitude.gridx = 1;
		gbc_textLongitude.gridy = 4;
		panel_2.add(textLongitude, gbc_textLongitude);
		textLongitude.setColumns(10);
		
		panel_3 = new JPanel();
		GridBagConstraints gbc_panel_3 = new GridBagConstraints();
		gbc_panel_3.anchor = GridBagConstraints.EAST;
		gbc_panel_3.fill = GridBagConstraints.VERTICAL;
		gbc_panel_3.gridx = 1;
		gbc_panel_3.gridy = 7;
		panel.add(panel_3, gbc_panel_3);
		panel_3.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		btnOK = new JButton("OK");
		btnOK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				process();
			}
		});
		panel_3.add(btnOK);
		
		btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				resetFields();
				NewLocationDialog.this.setVisible(false);
			}
			
		});
		panel_3.add(btnCancel);
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (InstantiationException e1) {
			e1.printStackTrace();
		} catch (IllegalAccessException e1) {
			e1.printStackTrace();
		} catch (UnsupportedLookAndFeelException e1) {
			e1.printStackTrace();
		}
	}
	
	private ArrayList<FormError> getErrors() {
		ArrayList<FormError> errors = new ArrayList<FormError>();
		
		// Location name is required
		if(textName.getText().isEmpty()) {
			errors.add(new FormError(textName, "A location name is required"));
		}
		
		if(!textFrequency.getText().matches("\\d{3,6}(\\.\\d{1,2})?")) {
			errors.add(new FormError(textFrequency, "Not a valid frequency."));
		}
		
		if(!Utilities.isValidDate(textStartTime.getText(), "HH:mm")) {
			errors.add(new FormError(textStartTime, "Invalid start time."));
		}
		
		if(!Utilities.isValidDate(textStopTime.getText(), "HH:mm")) {
			errors.add(new FormError(textStopTime, "Invlaid stop time."));
		}
		
		if(!textLatitude.getText().matches(CoordinateTextField.getRegexlat())) {
			errors.add(new FormError(textLatitude, "Not a valid latitude coordinate."));
		}
		
		if(!textLongitude.getText().matches(CoordinateTextField.getRegexLng())) {
			errors.add(new FormError(textLongitude, "Not a valid longitude coordinate."));
		}
		
		return errors;
	}
	
	protected void process() {
		ArrayList<FormError> errors = getErrors();
		
		getErrorPanel().removeAll();
		getErrorPanel().setVisible(false);
		
		if(errors.isEmpty()) {
			Station stn = (Station) this.comboBoxStations.getSelectedItem();
			Language lang = (Language) this.comboLanguage.getSelectedItem();
			
			Location loc = new Location();
			loc.setStationId(stn.getId());
			if(lang != null) {
				loc.setLanguage(lang.getIso());
			}
			loc.setLocationName(this.textName.getText());
			loc.setStrFrequency(this.textFrequency.getText());
			loc.setStrLatitude(this.textLatitude.getText());
			loc.setStrLongitude(this.textLongitude.getText());
			loc.setStrTimeOff(this.textStopTime.getText());
			loc.setStrTimeOn(this.textStartTime.getText());
					
			Connection conn = Data.getConnection();
			PreparedStatement ps = null;
			try {
				ps = conn.prepareStatement("INSERT INTO locations (lang, lng, lat, time_off, time_on, frequency, location, station_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
				if(loc.getLanguage() == null) {
					ps.setNull(1, Types.VARCHAR);
				} else {
					ps.setString(1, loc.getLanguage());
				}
				
				if(loc.getStrLongitude().isEmpty()) {
					ps.setNull(2, Types.VARCHAR);
				} else {
					ps.setString(2, loc.getStrLongitude());
				}
				
				if(loc.getStrLatitude().isEmpty()) {
					ps.setNull(3, Types.VARCHAR);
				} else {
					ps.setString(3, loc.getStrLatitude());
				}
				
				if(loc.getStrTimeOff().isEmpty()) {
					ps.setNull(4, Types.VARCHAR);
				} else {
					ps.setString(4, loc.getStrTimeOff());
				}
				
				if(loc.getStrTimeOn().isEmpty()) {
					ps.setNull(5, Types.VARCHAR);
				} else {
					ps.setString(5, loc.getStrTimeOn());
				}
				
				ps.setString(6, loc.getStrFrequency());
				ps.setString(7, loc.getLocationName());
				ps.setInt(8, loc.getStationId());
				
				ps.execute();
				
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			
			resetFields();
			updateLists();
			setVisible(false);
		} else {
			getErrorPanel().setVisible(true);
			for(FormError fe : errors) {
				JLabel errLabel = new JLabel(fe.getMessage());
				errLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
				errLabel.setForeground(Color.RED);
				getErrorPanel().add(errLabel);
			}
		}

	}

	private void updateLists() {
		// LocationsTab now manages user station locations (places); no station/location list to refresh here.
	}

	private void resetFields() {
		this.textFrequency.setText("");
		this.textLatitude.setText("");
		this.textLongitude.setText("");
		this.textName.setText("");
		this.textStartTime.setText("");
		this.textStopTime.setText("");
		this.comboBoxStations.setSelectedIndex(0);
		this.comboLanguage.setSelectedIndex(0);
	}

	public NewLocationDialog(Frame owner) {
		super(owner);
	}

	public JPanel getErrorPanel() {
		return errorPanel;
	}

	public void setErrorPanel(JPanel errorPanel) {
		this.errorPanel = errorPanel;
	}

}
