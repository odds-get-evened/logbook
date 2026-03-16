package org.qualsh.lb.view;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.qualsh.lb.place.Place;

public class EditLocationPanel extends JPanel {

	private static final long serialVersionUID = -2927068870982560959L;
	private Place currentPlace;
	private JLabel lblLocation;
	private JLabel lblPlaceName;
	private JLabel lblCoordinates;

	public EditLocationPanel(Place place) {
		setBorder(new EmptyBorder(0, 10, 0, 10));
		setCurrentPlace(place);

		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 1.0};
		setLayout(gridBagLayout);

		lblLocation = new JLabel("Location");
		lblLocation.setFont(new Font("Tahoma", Font.BOLD, 12));
		GridBagConstraints gbc_lblLocation = new GridBagConstraints();
		gbc_lblLocation.anchor = GridBagConstraints.WEST;
		gbc_lblLocation.insets = new Insets(0, 0, 5, 0);
		gbc_lblLocation.gridx = 0;
		gbc_lblLocation.gridy = 0;
		add(lblLocation, gbc_lblLocation);

		lblPlaceName = new JLabel();
		lblPlaceName.setFont(new Font("Tahoma", Font.PLAIN, 12));
		GridBagConstraints gbc_lblPlaceName = new GridBagConstraints();
		gbc_lblPlaceName.anchor = GridBagConstraints.WEST;
		gbc_lblPlaceName.insets = new Insets(0, 0, 5, 0);
		gbc_lblPlaceName.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblPlaceName.gridx = 0;
		gbc_lblPlaceName.gridy = 1;
		add(lblPlaceName, gbc_lblPlaceName);

		lblCoordinates = new JLabel();
		lblCoordinates.setFont(new Font("Tahoma", Font.PLAIN, 12));
		GridBagConstraints gbc_lblCoordinates = new GridBagConstraints();
		gbc_lblCoordinates.anchor = GridBagConstraints.WEST;
		gbc_lblCoordinates.insets = new Insets(0, 0, 5, 0);
		gbc_lblCoordinates.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblCoordinates.gridx = 0;
		gbc_lblCoordinates.gridy = 2;
		add(lblCoordinates, gbc_lblCoordinates);
	}

	public Place getCurrentPlace() {
		return currentPlace;
	}

	public void setCurrentPlace(Place place) {
		this.currentPlace = place;
		setup();
	}

	private void setup() {
		if (this.currentPlace != null) {
			lblPlaceName.setText(this.currentPlace.getPlaceName());
			String lat = this.currentPlace.getLatitude();
			String lon = this.currentPlace.getLongitude();
			lblCoordinates.setText((lat != null && lon != null) ? lat + ", " + lon : "");
			LogInteraction li = (LogInteraction) this.getParent().getParent().getParent().getParent();
			li.getBtnRemoveLocation().setEnabled(true);
		}
	}

	public void unsetLocation() {
		this.currentPlace = null;
		lblPlaceName.setText("");
		lblCoordinates.setText("");
		LogInteraction li = (LogInteraction) this.getParent().getParent().getParent().getParent();
		li.getBtnRemoveLocation().setEnabled(false);
	}
}
