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
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.swing.JButton;
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

import org.json.simple.parser.ParseException;
import org.qualsh.lb.util.Geocode;
import org.qualsh.lb.place.Place;
import org.qualsh.lb.util.Preferences;
import org.qualsh.lb.util.Utilities;
import org.qualsh.lb.view.field.CoordinateTextField;

public class PreferencesDialog extends JDialog {

	private static final long serialVersionUID = -3263330177361727514L;
	private CoordinateTextField textLatitude;
	private CoordinateTextField textLongitude;
	private JButton btnSave;
	private JButton btnCancel;
	private JTextField textFindLocation;
	private JButton btnFindLocation;
	private JLabel lblCurrentLocation;
	private JButton btnReset;
	private LogInteraction logInteraction;

	public PreferencesDialog(JFrame frame) {
		super(frame);
		
		this.setMinimumSize(new Dimension(400, 300));
		this.setTitle("Preferences");
		this.setModal(true);
		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int winW = (int) (screenSize.getWidth() * 0.3);
		int winH = (int) (screenSize.getHeight() * 0.25);
		setSize(winW, winH);
		
		int winX = (int) ((screenSize.getWidth() - getSize().getWidth()) / 2);
		int winY = (int) ((screenSize.getHeight() - getSize().getHeight()) / 2);
		setLocation(winX, winY);
		
		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(panel, BorderLayout.CENTER);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{0, 0};
		gbl_panel.rowHeights = new int[]{0, 0, 0};
		gbl_panel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
		panel.setLayout(gbl_panel);
		
		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new CompoundBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Your Location", TitledBorder.LEADING, TitledBorder.TOP, null, null), new EmptyBorder(5, 5, 5, 5)));
		GridBagConstraints gbc_panel_1 = new GridBagConstraints();
		gbc_panel_1.insets = new Insets(0, 0, 5, 0);
		gbc_panel_1.fill = GridBagConstraints.BOTH;
		gbc_panel_1.gridx = 0;
		gbc_panel_1.gridy = 0;
		panel.add(panel_1, gbc_panel_1);
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.columnWidths = new int[]{0, 0, 0};
		gbl_panel_1.rowHeights = new int[]{0, 0, 0, 0, 0, 0};
		gbl_panel_1.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_panel_1.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
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
		
		JLabel lblFindLocation = new JLabel("Find location");
		lblFindLocation.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblFindLocation = new GridBagConstraints();
		gbc_lblFindLocation.insets = new Insets(0, 0, 5, 5);
		gbc_lblFindLocation.gridx = 0;
		gbc_lblFindLocation.gridy = 1;
		panel_1.add(lblFindLocation, gbc_lblFindLocation);
		
		JPanel panel_4 = new JPanel();
		GridBagConstraints gbc_panel_4 = new GridBagConstraints();
		gbc_panel_4.insets = new Insets(0, 0, 5, 0);
		gbc_panel_4.fill = GridBagConstraints.BOTH;
		gbc_panel_4.gridx = 1;
		gbc_panel_4.gridy = 1;
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
				
				if(!PreferencesDialog.this.getTextFindLocation().getText().isEmpty()) {
					try {
						Geocode gc = (Geocode) Utilities.geocode(PreferencesDialog.this.getTextFindLocation().getText().trim());
						PreferencesDialog.this.getTextLatitude().setText(gc.getLatitude());
						PreferencesDialog.this.getTextLongitude().setText(gc.getLongitude());
						PreferencesDialog.this.getLblCurrentLocation().setText(gc.getCity()+", "+gc.getState());
					} catch (MalformedURLException e1) {
						e1.printStackTrace();
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (ParseException e1) {
						e1.printStackTrace();
					}
				}
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
		gbc_lblLatitude.gridy = 2;
		panel_1.add(lblLatitude, gbc_lblLatitude);
		
		textLatitude = new CoordinateTextField();
		GridBagConstraints gbc_textLatitude = new GridBagConstraints();
		gbc_textLatitude.fill = GridBagConstraints.HORIZONTAL;
		gbc_textLatitude.insets = new Insets(0, 0, 5, 0);
		gbc_textLatitude.gridx = 1;
		gbc_textLatitude.gridy = 2;
		panel_1.add(textLatitude, gbc_textLatitude);
		
		JLabel lblLongitude = new JLabel("Longitude");
		lblLongitude.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblLongitude = new GridBagConstraints();
		gbc_lblLongitude.insets = new Insets(0, 0, 5, 5);
		gbc_lblLongitude.gridx = 0;
		gbc_lblLongitude.gridy = 3;
		panel_1.add(lblLongitude, gbc_lblLongitude);
		
		textLongitude = new CoordinateTextField();
		GridBagConstraints gbc_textLongitude = new GridBagConstraints();
		gbc_textLongitude.insets = new Insets(0, 0, 5, 0);
		gbc_textLongitude.fill = GridBagConstraints.HORIZONTAL;
		gbc_textLongitude.gridx = 1;
		gbc_textLongitude.gridy = 3;
		panel_1.add(textLongitude, gbc_textLongitude);
		
		// reset user's location preference
		btnReset = new JButton("Reset");
		btnReset.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Preferences.remove(Preferences.PREF_NAME_MY_PLACE);
				
				PreferencesDialog.this.getLblCurrentLocation().setText("No location set.");
				PreferencesDialog.this.getTextFindLocation().setText("");
				PreferencesDialog.this.getTextLatitude().setText("");
				PreferencesDialog.this.getTextLongitude().setText("");
			}
			
		});
		GridBagConstraints gbc_btnReset = new GridBagConstraints();
		gbc_btnReset.anchor = GridBagConstraints.EAST;
		gbc_btnReset.gridx = 1;
		gbc_btnReset.gridy = 4;
		panel_1.add(btnReset, gbc_btnReset);
		
		JPanel panel_3 = new JPanel();
		GridBagConstraints gbc_panel_3 = new GridBagConstraints();
		gbc_panel_3.anchor = GridBagConstraints.EAST;
		gbc_panel_3.fill = GridBagConstraints.VERTICAL;
		gbc_panel_3.gridx = 0;
		gbc_panel_3.gridy = 1;
		panel.add(panel_3, gbc_panel_3);
		
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
		
		this.addWindowListener(new WindowListener() {

			public void windowOpened(WindowEvent e) {
				System.out.println("Preferences dialog opened");
				String pref = Preferences.getOne(Preferences.PREF_NAME_MY_PLACE);
				System.out.println("Preferences dialog was opened; User preferences; my_place ID: " + pref);
				if(pref != null) {
					int placeId = Integer.parseInt(pref);
					Place place = Place.getOne(placeId);
					
					PreferencesDialog.this.getLblCurrentLocation().setText(place.getPlaceName());
					PreferencesDialog.this.getTextLatitude().setText(place.getLatitude());
					PreferencesDialog.this.getTextLongitude().setText(place.getLongitude());
				}
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
				if(pref != null) {
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
	}

	private void saveMyPlace() {
		if(!this.getTextLatitude().getText().isEmpty() && 
				!this.getTextLongitude().getText().isEmpty()) { // only set place if coordinates are present
			Place place = new Place();
			place.setLatitude(this.getTextLatitude().getText());
			place.setLongitude(this.getTextLongitude().getText());
			place.setPlaceName(this.getLblCurrentLocation().getText());
			int placeId = place.insert();
			place.setId(placeId);
			Preferences.saveMyPlace(place);
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

}
