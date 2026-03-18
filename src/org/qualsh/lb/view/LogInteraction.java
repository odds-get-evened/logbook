package org.qualsh.lb.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;

import org.qualsh.lb.MainWin;
import org.qualsh.lb.data.LogsModel;
import org.qualsh.lb.log.Log;
import org.qualsh.lb.place.Place;
import org.qualsh.lb.rig.RigController;
import org.qualsh.lb.util.FormError;
import org.qualsh.lb.util.Preferences;
import org.qualsh.lb.util.TextNote;
import org.qualsh.lb.util.Utilities;
import org.qualsh.lb.view.field.FrequencyTextField;
import org.qualsh.lb.view.field.ModesComboBox;
import org.qualsh.lb.view.field.TimeOnField;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import java.awt.Dimension;

public class LogInteraction extends JPanel {

	private static final long serialVersionUID = 1538060743735791627L;
	private Timer timeOnTimer;
	private MainWin mainWin;
	private LogsTable logTable;
	private JTextField textInfoFreq;
	private JTextField textInfoDateOn;
	private TextNote textInfoDesc;
	private JButton btnDeleteLog;
	private JTabbedPane tabbedPane;
	private JButton btnEditLog;
	private Log currentLog = null;
	private ViewLocationPanel viewLocationPanel;
	private JPanel locationsPanel;
	private LocationsTab locationsTab;
	private PreferencesDialog preferencesDialog;
	private ViewRXPanel viewRXPanel;
	private JPanel detailsPanel;
	private JPanel logEntryPanel;
	private JPanel logEntryForm;
	private JLabel label;
	private JLabel label_1;
	private ModesComboBox modesComboBox;
	private JLabel label_2;
	private JLabel lblTime;
	private JLabel label_3;
	private JLabel label_4;
	private JLabel lblPlaceName;
	private JLabel lblRxCoordinates;
	private JButton btnLocation;
	private JButton saveLogBtn;
	private JButton btnCancelLogEntry;
	private JButton btnRemoveLocation;
	private FrequencyTextField textFrequency;
	private JFormattedTextField textDateOn;
	private JTextArea textDescription;
	private TimeOnField textTimeOn;
	private JButton btnChangeLocation;
	private JScrollPane scrollPane;
	private EditLocationPanel editLocationPanel;
	private JPanel formErrorPanel;
	private Place selectedMyPlace = null;
	private JLabel lblRigStatus;
	private JButton btnFromRadio;
	private DXClusterPanel dxClusterPanel;

	public LogInteraction() {
		setLayout(new BorderLayout(0, 0));
		
		setTabbedPane(new JTabbedPane(JTabbedPane.TOP));
		this.getTabbedPane().addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				JTabbedPane source = (JTabbedPane) e.getSource();
				int selectedNum = source.getSelectedIndex();
				
				if(selectedNum == 1) { // log entry panel
					String placeId = Preferences.getOne(Preferences.PREF_NAME_MY_PLACE);
					if(placeId != null) {
						Place place = Place.getOne(Integer.parseInt(placeId));
						LogInteraction.this.getLblPlaceName().setText(place.getPlaceName());
						LogInteraction.this.getLblRxCoordinates().setText(place.getLatitude()+", "+place.getLongitude());
					} else {
						LogInteraction.this.getLblPlaceName().setText("No location set.");
						LogInteraction.this.getLblRxCoordinates().setText("");
					}
				}
			}
			
		});
		add(getTabbedPane(), BorderLayout.CENTER);
		
		DateFormatter dateFormatter = new DateFormatter(new SimpleDateFormat("MM/dd/yyyy"));
		DefaultFormatterFactory dateFormatterFactory = new DefaultFormatterFactory(dateFormatter, dateFormatter, dateFormatter);
		
		JPanel currentLogPanel = new JPanel();
		currentLogPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		currentLogPanel.setLayout(new BorderLayout(0, 0));
		
		getTabbedPane().addTab("Info", null, currentLogPanel, null);
		
		JPanel logInfoPanel = new JPanel();
		currentLogPanel.add(logInfoPanel, BorderLayout.CENTER);
		GridBagLayout gbl_logInfoPanel = new GridBagLayout();
		gbl_logInfoPanel.columnWidths = new int[]{0, 108, 0, 0, 0, 0};
		gbl_logInfoPanel.rowHeights = new int[]{0, 0, 0, 0};
		gbl_logInfoPanel.columnWeights = new double[]{1.0, 1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_logInfoPanel.rowWeights = new double[]{1.0, 0.0, 1.0, Double.MIN_VALUE};
		logInfoPanel.setLayout(gbl_logInfoPanel);
		
		detailsPanel = new JPanel();
		detailsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		GridBagConstraints gbc_detailsPanel = new GridBagConstraints();
		gbc_detailsPanel.gridwidth = 5;
		gbc_detailsPanel.insets = new Insets(0, 0, 5, 0);
		gbc_detailsPanel.fill = GridBagConstraints.BOTH;
		gbc_detailsPanel.gridx = 0;
		gbc_detailsPanel.gridy = 0;
		logInfoPanel.add(detailsPanel, gbc_detailsPanel);
		GridBagLayout gbl_detailsPanel = new GridBagLayout();
		gbl_detailsPanel.columnWidths = new int[]{0, 0, 0};
		gbl_detailsPanel.rowHeights = new int[]{0, 0, 0, 0};
		gbl_detailsPanel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_detailsPanel.rowWeights = new double[]{0.0, 0.0, 1.0, Double.MIN_VALUE};
		detailsPanel.setLayout(gbl_detailsPanel);
		
		JLabel lblFrequency_1 = new JLabel("Frequency (kHz):");
		GridBagConstraints gbc_lblFrequency_1 = new GridBagConstraints();
		gbc_lblFrequency_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblFrequency_1.gridx = 0;
		gbc_lblFrequency_1.gridy = 0;
		detailsPanel.add(lblFrequency_1, gbc_lblFrequency_1);
		lblFrequency_1.setFont(new Font("Tahoma", Font.BOLD, 12));
		
		textInfoFreq = new JTextField();
		textInfoFreq.setFont(new Font("Tahoma", Font.PLAIN, 12));
		GridBagConstraints gbc_textInfoFreq = new GridBagConstraints();
		gbc_textInfoFreq.fill = GridBagConstraints.HORIZONTAL;
		gbc_textInfoFreq.insets = new Insets(0, 0, 5, 0);
		gbc_textInfoFreq.gridx = 1;
		gbc_textInfoFreq.gridy = 0;
		detailsPanel.add(textInfoFreq, gbc_textInfoFreq);
		textInfoFreq.setBorder(new EmptyBorder(0, 0, 0, 0));
		textInfoFreq.setEditable(false);
		textInfoFreq.setColumns(10);
		
		JLabel lblDate_1 = new JLabel("Date:");
		GridBagConstraints gbc_lblDate_1 = new GridBagConstraints();
		gbc_lblDate_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblDate_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblDate_1.gridx = 0;
		gbc_lblDate_1.gridy = 1;
		detailsPanel.add(lblDate_1, gbc_lblDate_1);
		lblDate_1.setFont(new Font("Tahoma", Font.BOLD, 12));
		
		textInfoDateOn = new JTextField();
		textInfoDateOn.setFont(new Font("Tahoma", Font.PLAIN, 12));
		GridBagConstraints gbc_textInfoDateOn = new GridBagConstraints();
		gbc_textInfoDateOn.fill = GridBagConstraints.HORIZONTAL;
		gbc_textInfoDateOn.insets = new Insets(0, 0, 5, 0);
		gbc_textInfoDateOn.gridx = 1;
		gbc_textInfoDateOn.gridy = 1;
		detailsPanel.add(textInfoDateOn, gbc_textInfoDateOn);
		textInfoDateOn.setBorder(new EmptyBorder(0, 0, 0, 0));
		textInfoDateOn.setEditable(false);
		textInfoDateOn.setColumns(10);
		
		JLabel lblDescription_1 = new JLabel("Description:");
		GridBagConstraints gbc_lblDescription_1 = new GridBagConstraints();
		gbc_lblDescription_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblDescription_1.anchor = GridBagConstraints.NORTH;
		gbc_lblDescription_1.insets = new Insets(0, 0, 0, 5);
		gbc_lblDescription_1.gridx = 0;
		gbc_lblDescription_1.gridy = 2;
		detailsPanel.add(lblDescription_1, gbc_lblDescription_1);
		lblDescription_1.setFont(new Font("Tahoma", Font.BOLD, 12));
		
		JScrollPane scrollPane_1 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
		gbc_scrollPane_1.anchor = GridBagConstraints.NORTH;
		gbc_scrollPane_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_scrollPane_1.gridx = 1;
		gbc_scrollPane_1.gridy = 2;
		detailsPanel.add(scrollPane_1, gbc_scrollPane_1);
		scrollPane_1.setBorder(new EmptyBorder(0, 0, 0, 0));
		
		textInfoDesc = new TextNote();
		textInfoDesc.setFont(new Font("Tahoma", Font.PLAIN, 12));
		textInfoDesc.setRows(5);
		scrollPane_1.setViewportView(textInfoDesc);
		
		GridBagConstraints gbc_panel_2 = new GridBagConstraints();
		gbc_panel_2.gridwidth = 5;
		gbc_panel_2.fill = GridBagConstraints.BOTH;
		gbc_panel_2.gridx = 0;
		gbc_panel_2.gridy = 2;
	
		viewLocationPanel = new ViewLocationPanel();
		
		viewRXPanel = new ViewRXPanel();
		
		JSplitPane txRxPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, viewLocationPanel, viewRXPanel);
		txRxPanel.setDividerLocation(220);
		txRxPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		logInfoPanel.add(txRxPanel, gbc_panel_2);
		
		JPanel logInfoBtnPanel = new JPanel();
		currentLogPanel.add(logInfoBtnPanel, BorderLayout.SOUTH);
		GridBagLayout gbl_logInfoBtnPanel = new GridBagLayout();
		gbl_logInfoBtnPanel.columnWidths = new int[] {0, 70};
		gbl_logInfoBtnPanel.rowHeights = new int[] {23};
		gbl_logInfoBtnPanel.columnWeights = new double[]{0.0, 0.0};
		gbl_logInfoBtnPanel.rowWeights = new double[]{0.0};
		logInfoBtnPanel.setLayout(gbl_logInfoBtnPanel);
		
		btnDeleteLog = new JButton("Delete");
		btnDeleteLog.setEnabled(false);
		btnDeleteLog.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				LogsModel lm = (LogsModel) getLogTable().getModel();
				Log log = lm.getData().get(getLogTable().convertRowIndexToModel(getLogTable().getSelectedRow()));
				lm.delete(log);
				/**
				 * @todo also update map
				 */
			}
			
		});
		
		btnEditLog = new JButton("Edit");
		btnEditLog.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				LogsModel lm = (LogsModel) getLogTable().getModel();
				Log log = lm.getData().get(getLogTable().convertRowIndexToModel(getLogTable().getSelectedRow()));
				
				System.out.println("EDITING LOG: " + log);
				
				fillForm(log);
				
				getTabbedPane().setSelectedIndex(1);
				
				/**
				 * @todo also update map
				 */
			}
			
		});
		btnEditLog.setEnabled(false);
		GridBagConstraints gbc_btnEditLog = new GridBagConstraints();
		gbc_btnEditLog.anchor = GridBagConstraints.WEST;
		gbc_btnEditLog.insets = new Insets(0, 0, 0, 5);
		gbc_btnEditLog.gridx = 0;
		gbc_btnEditLog.gridy = 0;
		logInfoBtnPanel.add(btnEditLog, gbc_btnEditLog);
		GridBagConstraints gbc_btnDeleteLog = new GridBagConstraints();
		gbc_btnDeleteLog.anchor = GridBagConstraints.NORTHWEST;
		gbc_btnDeleteLog.gridx = 1;
		gbc_btnDeleteLog.gridy = 0;
		logInfoBtnPanel.add(btnDeleteLog, gbc_btnDeleteLog);
		
		logEntryPanel = new JPanel();
		logEntryPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.addTab("Entry", null, logEntryPanel, null);
		logEntryPanel.setLayout(new BorderLayout(0, 0));

		formErrorPanel = new JPanel();
		formErrorPanel.setLayout(new GridBagLayout());
		formErrorPanel.setBackground(new Color(255, 230, 230));
		formErrorPanel.setBorder(new EmptyBorder(4, 6, 4, 6));
		formErrorPanel.setVisible(false);
		logEntryPanel.add(formErrorPanel, BorderLayout.NORTH);

		logEntryForm = new JPanel();
		logEntryPanel.add(logEntryForm, BorderLayout.CENTER);
		logEntryForm.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,       // 2  Frequency
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,       // 4  Mode
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,       // 6  Date
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,       // 8  Time
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("default:grow"),    // 10 Description
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,       // 12 TX Location panel
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,       // 14 TX Location buttons
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,       // 16 My Location (RX) name
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,       // 18 RX coordinates
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,       // 20 My Location button
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));   // 22 Save / Cancel
		
		label = new JLabel("Frequency");
		label.setFont(new Font("Tahoma", Font.BOLD, 12));
		logEntryForm.add(label, "2, 2, right, default");

		textFrequency = new FrequencyTextField();

		// Status dot: grey = not connected, green = connected
		lblRigStatus = new JLabel("\u25CF"); // ● filled circle
		lblRigStatus.setForeground(Color.LIGHT_GRAY);
		lblRigStatus.setToolTipText("CAT rig control: not connected");

		btnFromRadio = new JButton("From Radio");
		btnFromRadio.setFont(btnFromRadio.getFont().deriveFont(11f));
		btnFromRadio.setMargin(new java.awt.Insets(1, 5, 1, 5));
		btnFromRadio.setToolTipText("Click to connect to the radio and read the current frequency");
		btnFromRadio.addActionListener(ev -> onFromRadio());

		JPanel freqPanel = new JPanel(new BorderLayout(3, 0));
		freqPanel.add(textFrequency, BorderLayout.CENTER);
		JPanel freqBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
		freqBtnPanel.add(btnFromRadio);
		freqBtnPanel.add(lblRigStatus);
		freqPanel.add(freqBtnPanel, BorderLayout.EAST);
		logEntryForm.add(freqPanel, "4, 2, fill, default");

		// Register CAT status and frequency listeners
		RigController.getInstance().addStatusListener(conn -> SwingUtilities.invokeLater(() -> {
			if (conn) {
				lblRigStatus.setForeground(new Color(0, 160, 0));
				lblRigStatus.setToolTipText("CAT rig control: connected");
				btnFromRadio.setText("Disconnect");
			} else {
				lblRigStatus.setForeground(Color.LIGHT_GRAY);
				lblRigStatus.setToolTipText("CAT rig control: not connected");
				btnFromRadio.setText("From Radio");
			}
		}));
		RigController.getInstance().addFrequencyListener(freqKhz -> SwingUtilities.invokeLater(() -> {
			// Format: drop trailing zeros (e.g. 14225.0 → "14225", 14225.5 → "14225.5")
			String formatted;
			if (freqKhz == Math.floor(freqKhz)) {
				formatted = String.valueOf((long) freqKhz.doubleValue());
			} else {
				formatted = String.format("%.3f", freqKhz).replaceAll("0+$", "");
			}
			textFrequency.setText(formatted);
		}));
		
		label_1 = new JLabel("Mode");
		label_1.setFont(new Font("Tahoma", Font.BOLD, 12));
		logEntryForm.add(label_1, "2, 4, right, default");
		
		modesComboBox = new ModesComboBox();
		logEntryForm.add(modesComboBox, "4, 4, fill, default");
		
		label_2 = new JLabel("Date");
		label_2.setFont(new Font("Tahoma", Font.BOLD, 12));
		logEntryForm.add(label_2, "2, 6, right, default");
		
		textDateOn = new JFormattedTextField(dateFormatterFactory);
		textDateOn.setValue(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime());

		JButton btnCalendar = new JButton("\uD83D\uDCC5"); // 📅 calendar emoji
		btnCalendar.setFont(btnCalendar.getFont().deriveFont(11f));
		btnCalendar.setMargin(new java.awt.Insets(1, 4, 1, 4));
		btnCalendar.setToolTipText("Pick a date from the calendar");
		btnCalendar.addActionListener(e -> {
			Object val = textDateOn.getValue();
			Date current = (val instanceof Date) ? (Date) val : null;
			DatePickerDialog picker = new DatePickerDialog(
					LogInteraction.this.getMainWin(), current);
			picker.setVisible(true);
			Date picked = picker.getSelectedDate();
			if (picked != null) {
				textDateOn.setValue(picked);
			}
		});

		JPanel dateFieldPanel = new JPanel(new BorderLayout(2, 0));
		dateFieldPanel.add(textDateOn, BorderLayout.CENTER);
		dateFieldPanel.add(btnCalendar, BorderLayout.EAST);
		logEntryForm.add(dateFieldPanel, "4, 6, fill, default");
		
		lblTime = new JLabel("Time");
		lblTime.setFont(new Font("Tahoma", Font.BOLD, 12));
		logEntryForm.add(lblTime, "2, 8, right, default");
		
		textTimeOn = new TimeOnField();
		logEntryForm.add(textTimeOn, "4, 8, fill, default");
		
		label_3 = new JLabel("Description");
		label_3.setFont(new Font("Tahoma", Font.BOLD, 12));
		logEntryForm.add(label_3, "2, 10, right, top");
		
		scrollPane = new JScrollPane();
		scrollPane.setPreferredSize(new Dimension(4, 70));
		logEntryForm.add(scrollPane, "4, 10, fill, fill");

		textDescription = new JTextArea();
		textDescription.setRows(2);
		scrollPane.setViewportView(textDescription);

		// TX Location buttons — created here so they can be added to the form below
		btnLocation = new JButton("Location\u2026");
		btnLocation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LocationEditor le = new LocationEditor(LogInteraction.this.getMainWin());
				le.setVisible(true);
				Place picked = le.getSelectedPlace();
				if (picked != null) {
					editLocationPanel.setCurrentPlace(picked);
				}
			}
		});

		btnRemoveLocation = new JButton("Remove Location");
		btnRemoveLocation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LogInteraction.this.getEditLocationPanel().unsetLocation();
			}
		});
		btnRemoveLocation.setEnabled(false);

		// TX Location row (row 12)
		JLabel lblTxLocation = new JLabel("TX Location");
		lblTxLocation.setFont(new Font("Tahoma", Font.BOLD, 12));
		logEntryForm.add(lblTxLocation, "2, 12, right, top");

		editLocationPanel = new EditLocationPanel((Place) null);
		logEntryForm.add(editLocationPanel, "4, 12, fill, fill");

		// TX Location action buttons (row 14)
		JPanel txBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		txBtnPanel.add(btnLocation);
		txBtnPanel.add(btnRemoveLocation);
		logEntryForm.add(txBtnPanel, "4, 14");

		// My Location (RX) rows — shifted to 16/18/20
		label_4 = new JLabel("My Location");
		label_4.setFont(new Font("Tahoma", Font.BOLD, 12));
		logEntryForm.add(label_4, "2, 16, right, default");

		lblPlaceName = new JLabel("Place name");
		lblPlaceName.setFont(new Font("Tahoma", Font.PLAIN, 12));
		logEntryForm.add(lblPlaceName, "4, 16");

		lblRxCoordinates = new JLabel("Coordinates");
		lblRxCoordinates.setFont(new Font("Tahoma", Font.PLAIN, 12));
		logEntryForm.add(lblRxCoordinates, "4, 18");

		saveLogBtn = new JButton("Save");
		saveLogBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(!(getCurrentLog() instanceof Log)) {
					processInsert();
				} else {
					processUpdate();
				}
			}
		});
		
		btnChangeLocation = new JButton("My Location\u2026");
		btnChangeLocation.setFont(new Font("Tahoma", Font.PLAIN, 12));
		btnChangeLocation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PlacePickerDialog picker = new PlacePickerDialog(LogInteraction.this.getMainWin());
				picker.setOnPlaceCreated(() -> LogInteraction.this.getLocationsTab().refreshList());
				picker.setVisible(true);
				Place picked = picker.getSelectedPlace();
				if (picked != null) {
					LogInteraction.this.selectedMyPlace = picked;
					LogInteraction.this.getLblPlaceName().setText(picked.getPlaceName());
					LogInteraction.this.getLblRxCoordinates().setText(picked.getLatitude() + ", " + picked.getLongitude());
				}
			}
		});
		btnChangeLocation.setToolTipText("Select your reception (RX) location for this log entry");
		logEntryForm.add(btnChangeLocation, "4, 20, left, default");

		btnCancelLogEntry = new JButton("Cancel");
		btnCancelLogEntry.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LogInteraction.this.setCurrentLog(null);
				LogInteraction.this.resetEntry();
			}
		});

		JPanel actionBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		actionBtnPanel.add(saveLogBtn);
		actionBtnPanel.add(btnCancelLogEntry);
		logEntryForm.add(actionBtnPanel, "4, 22");
		
		locationsPanel = new JPanel();
		tabbedPane.addTab("Locations", null, locationsPanel, null);
		locationsPanel.setLayout(new BorderLayout(0, 0));

		locationsTab = new LocationsTab();
		locationsPanel.add(locationsTab, BorderLayout.CENTER);

		// DX Cluster tab
		dxClusterPanel = new DXClusterPanel();
		dxClusterPanel.setLogInteraction(this);
		tabbedPane.addTab("DX Cluster", null, dxClusterPanel, null);

	}
	
	public ViewRXPanel getViewRXPanel() {
		return viewRXPanel;
	}

	public void setViewRXPanel(ViewRXPanel viewRXPanel) {
		this.viewRXPanel = viewRXPanel;
	}

	/**
	 * Fill entry form log data
	 * @param Log
	 */
	public void fillForm(Log log) {
		setCurrentLog(log);
		
		// stop the time on text field from updating
		getTextTimeOn().getoTimer().stop();
		
		getTextFrequency().setText(String.valueOf(log.getFrequency()));
		getComboModes().setSelectedItem(log.getMode());
		Date date = new Date((long)log.getDateOn()*1000);
		getTextDateOn().setValue(date);
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		getTextTimeOn().setText(sdf.format(date));
		getTextDescription().setText(log.getDescription());
		
		if(log.getLocation() != 0) {
			this.getEditLocationPanel().setCurrentPlace(log.getFullTxPlace());
		}

		if (log.getMyPlace() != 0) {
			Place place = Place.getOne(log.getMyPlace());
			if (place != null) {
				this.selectedMyPlace = place;
				this.getLblPlaceName().setText(place.getPlaceName());
				this.getLblRxCoordinates().setText(place.getLatitude() + ", " + place.getLongitude());
			}
		}
	}
	
	private ArrayList<FormError> getErrors() {
		ArrayList<FormError> errors = new ArrayList<FormError>();
		
		if(!textFrequency.getText().matches("\\d{3,6}(\\.\\d{1,2})?")) {
			errors.add(new FormError(textFrequency, "Not a valid frequency."));
		}
		
		if(!Utilities.isValidDate(textDateOn.getText(), "MM/dd/yyyy")) {
			errors.add(new FormError(textDateOn, "Not a valid date."));
		}
		
		if(!Utilities.isValidDate(textTimeOn.getText(), "HH:mm")) {
			errors.add(new FormError(textTimeOn, "Invalid time."));
		}
		
		if(textDescription.getText().isEmpty()) {
			errors.add(new FormError(textDescription, "Description is required."));
		}
		
		return errors;
	}
	
	private void processUpdate() {
		ArrayList<FormError> errors = getErrors();
		
		if(errors.isEmpty()) {
			
			LogsModel lm = (LogsModel) getLogTable().getModel();
			
			Log log = getCurrentLog();
			log.setFrequency(Float.valueOf(getTextFrequency().getText()));
			log.setMode(getComboModes().getSelectedItem().toString());
			
			String dateStr = getTextDateOn().getText() + " " + getTextTimeOn().getText();
			int timestamp = Utilities.stringToUnixTimeStamp(dateStr, "MM/dd/yyyy HH:mm");
			log.setDateOn(timestamp);
			log.setDescription(getTextDescription().getText());

			Place txLoc = editLocationPanel.getCurrentPlace();
			log.setLocation(txLoc != null ? txLoc.getId() : 0);

			if (this.selectedMyPlace != null) {
				log.setMyPlace(this.selectedMyPlace.getId());
			} else {
				String myPlaceId = Preferences.getOne(Preferences.PREF_NAME_MY_PLACE);
				if (myPlaceId != null) {
					log.setMyPlace(Integer.parseInt(myPlaceId));
				} else {
					log.setMyPlace(0);
				}
			}
			
			lm.update(log);

			formErrorPanel.setVisible(false);
			resetEntry();
			setCurrentLog(null);
		} else {
			showFormErrors(errors);
		}
	}
	
	private void processInsert() {
		ArrayList<FormError> errors = getErrors();
		
		if(errors.isEmpty()) {
			LogsModel lm = (LogsModel) getLogTable().getModel();
			Log log = new Log();
			log.setFrequency(Float.valueOf(textFrequency.getText()));
			log.setMode(modesComboBox.getSelectedItem().toString());
			String dateStr = textDateOn.getText() + " " + textTimeOn.getText();
			int timestamp = Utilities.stringToUnixTimeStamp(dateStr, "MM/dd/yyyy HH:mm");
			log.setDateOn(timestamp);
			log.setDescription(textDescription.getText());
			if (editLocationPanel.getCurrentPlace() != null) {
				log.setLocation(editLocationPanel.getCurrentPlace().getId());
			}
			if (this.selectedMyPlace != null) {
				log.setMyPlace(this.selectedMyPlace.getId());
			} else {
				String myPlaceId = Preferences.getOne(Preferences.PREF_NAME_MY_PLACE);
				if(myPlaceId != null) {
					log.setMyPlace(Integer.parseInt(myPlaceId));
				}
			}

			lm.insert(log);

			formErrorPanel.setVisible(false);
			resetEntry();
		} else {
			showFormErrors(errors);
		}
	}
	
	private void showFormErrors(java.util.ArrayList<FormError> errors) {
		formErrorPanel.removeAll();
		GridBagConstraints ec = new GridBagConstraints();
		ec.gridx = 0;
		ec.gridy = GridBagConstraints.RELATIVE;
		ec.anchor = GridBagConstraints.WEST;
		ec.insets = new Insets(1, 0, 1, 0);
		for (FormError fe : errors) {
			JLabel errLabel = new JLabel("⚠ " + fe.getMessage());
			errLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
			errLabel.setForeground(new Color(160, 0, 0));
			formErrorPanel.add(errLabel, ec);
		}
		formErrorPanel.setVisible(true);
		formErrorPanel.revalidate();
		formErrorPanel.repaint();
	}

	private void onFromRadio() {
		RigController rig = RigController.getInstance();
		if (rig.isConnected()) {
			rig.disconnect();
		} else {
			new Thread(() -> {
				boolean ok = rig.connect();
				if (!ok) {
					SwingUtilities.invokeLater(() -> {
						lblRigStatus.setForeground(Color.RED);
						lblRigStatus.setToolTipText("CAT rig control: connection failed");
						javax.swing.JOptionPane.showMessageDialog(
								LogInteraction.this.getMainWin(),
								"Could not connect to radio.\n"
								+ "Check your CAT settings under Tools \u2192 CAT Settings\u2026",
								"CAT Connection Failed",
								javax.swing.JOptionPane.WARNING_MESSAGE);
					});
				}
			}, "RigConnect").start();
		}
	}

	public void resetEntry() {
		getTextDateOn().setValue(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime());
		getTextTimeOn().getoTimer().start();
		getTextDescription().setText("");
		getTextFrequency().setText("");
		getComboModes().setSelectedIndex(0);
		this.getEditLocationPanel().unsetLocation();
		if (formErrorPanel != null) {
			formErrorPanel.removeAll();
			formErrorPanel.setVisible(false);
		}
		// Reset per-log place selection; fall back to the global preference display
		this.selectedMyPlace = null;
		String prefPlaceId = Preferences.getOne(Preferences.PREF_NAME_MY_PLACE);
		if (prefPlaceId != null) {
			Place pref = Place.getOne(Integer.parseInt(prefPlaceId));
			if (pref != null) {
				getLblPlaceName().setText(pref.getPlaceName());
				getLblRxCoordinates().setText(pref.getLatitude() + ", " + pref.getLongitude());
			} else {
				getLblPlaceName().setText("No location set.");
				getLblRxCoordinates().setText("");
			}
		} else {
			getLblPlaceName().setText("No location set.");
			getLblRxCoordinates().setText("");
		}
	}

	public LogInteraction(LayoutManager layout) {
		super(layout);
	}

	public LogInteraction(boolean isDoubleBuffered) {
		super(isDoubleBuffered);
	}

	public LogInteraction(LayoutManager layout, boolean isDoubleBuffered) {
		super(layout, isDoubleBuffered);
	}

	public Timer getTimeOnTimer() {
		return timeOnTimer;
	}

	public void setTimeOnTimer(Timer timeOnTimer) {
		this.timeOnTimer = timeOnTimer;
	}

	public TimeOnField getTextTimeOn() {
		return textTimeOn;
	}

	public void setTextTimeOn(TimeOnField textTimeOn) {
		this.textTimeOn = textTimeOn;
	}

	public LogsTable getLogTable() {
		return logTable;
	}

	public void setLogTable(LogsTable logTable) {
		this.logTable = logTable;
		
		locationsTab.setLogsTable(this.getLogTable());
	}

	public JButton getBtnDeleteLog() {
		return btnDeleteLog;
	}

	public void setBtnDeleteLog(JButton btnDeleteLog) {
		this.btnDeleteLog = btnDeleteLog;
	}

	public JTextField getTextInfoFreq() {
		return textInfoFreq;
	}

	public void setTextInfoFreq(JTextField textInfoFreq) {
		this.textInfoFreq = textInfoFreq;
	}

	public JTextField getTextInfoDateOn() {
		return textInfoDateOn;
	}

	public void setTextInfoDateOn(JTextField textInfoDateOn) {
		this.textInfoDateOn = textInfoDateOn;
	}

	public TextNote getTextInfoDesc() {
		return textInfoDesc;
	}

	public void setTextInfoDesc(TextNote textInfoDesc) {
		this.textInfoDesc = textInfoDesc;
	}

	public FrequencyTextField getTextFrequency() {
		return textFrequency;
	}

	public void setTextFrequency(FrequencyTextField textFrequency) {
		this.textFrequency = textFrequency;
	}

	public ModesComboBox getComboModes() {
		return modesComboBox;
	}

	public void setComboModes(ModesComboBox comboModes) {
		this.modesComboBox = comboModes;
	}

	public JFormattedTextField getTextDateOn() {
		return textDateOn;
	}

	public void setTextDateOn(JFormattedTextField textDateOn) {
		this.textDateOn = textDateOn;
	}

	public JTextArea getTextDescription() {
		return textDescription;
	}

	public void setTextDescription(JTextArea textDescription) {
		this.textDescription = textDescription;
	}

	public JTabbedPane getTabbedPane() {
		return tabbedPane;
	}

	public void setTabbedPane(JTabbedPane tabbedPane) {
		this.tabbedPane = tabbedPane;
	}

	public JButton getBtnEditLog() {
		return btnEditLog;
	}

	public void setBtnEditLog(JButton btnEditLog) {
		this.btnEditLog = btnEditLog;
	}

	public Log getCurrentLog() {
		return currentLog;
	}

	public void setCurrentLog(Log currentLog) {
		this.currentLog = currentLog;
	}

	public JButton getBtnLocation() {
		return btnLocation;
	}

	public void setBtnLocation(JButton btnLocation) {
		this.btnLocation = btnLocation;
	}

	public EditLocationPanel getEditLocationPanel() {
		return editLocationPanel;
	}

	public void setEditLocationPanel(EditLocationPanel editLocationPanel) {
		this.editLocationPanel = editLocationPanel;
	}

	public JButton getBtnRemoveLocation() {
		return btnRemoveLocation;
	}

	public void setBtnRemoveLocation(JButton btnRemoveLocation) {
		this.btnRemoveLocation = btnRemoveLocation;
	}

	public ViewLocationPanel getViewLocationPanel() {
		return viewLocationPanel;
	}

	public void setViewLocationPanel(ViewLocationPanel viewLocationPanel) {
		this.viewLocationPanel = viewLocationPanel;
	}

	public LocationsTab getLocationsTab() {
		return locationsTab;
	}

	public void setLocationsTab(LocationsTab locationsTab) {
		this.locationsTab = locationsTab;
	}

	public JLabel getLblPlaceName() {
		return lblPlaceName;
	}

	public void setLblPlaceName(JLabel lblPlaceName) {
		this.lblPlaceName = lblPlaceName;
	}

	public JLabel getLblRxCoordinates() {
		return lblRxCoordinates;
	}

	public void setLblRxCoordinates(JLabel lblRxCoordinates) {
		this.lblRxCoordinates = lblRxCoordinates;
	}

	public DXClusterPanel getDxClusterPanel() {
		return dxClusterPanel;
	}

	public PreferencesDialog getPreferencesDialog() {
		return preferencesDialog;
	}

	public void setPreferencesDialog(PreferencesDialog preferencesDialog) {
		this.preferencesDialog = preferencesDialog;
	}

	public MainWin getMainWin() {
		return mainWin;
	}

	public void setMainWin(MainWin mainWin) {
		this.mainWin = mainWin;
	}


}
