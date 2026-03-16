package org.qualsh.lb.view;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.SystemColor;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.qualsh.lb.place.Place;

public class ViewLocationPanel extends JPanel {

	private static final long serialVersionUID = 2816652077014659307L;

	private Place currentPlace = null;
	private JLabel lblPlaceName;
	private JLabel lblCoordinates;

	public ViewLocationPanel() {
		setBorder(new TitledBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), new EmptyBorder(5, 5, 5, 5)), "TX Location", TitledBorder.LEADING, TitledBorder.TOP, new Font("Tahoma", Font.BOLD, 12), null));
		setLayout(new BorderLayout(0, 0));

		JPanel detailsPanel = new JPanel();
		detailsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		detailsPanel.setBackground(SystemColor.info);
		add(detailsPanel, BorderLayout.CENTER);
		GridBagLayout gbl = new GridBagLayout();
		gbl.columnWidths = new int[]{0, 0};
		gbl.rowHeights = new int[]{25, 25, 0};
		gbl.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		detailsPanel.setLayout(gbl);

		lblPlaceName = new JLabel("");
		lblPlaceName.setFont(new Font("Tahoma", Font.PLAIN, 12));
		GridBagConstraints gbc0 = new GridBagConstraints();
		gbc0.anchor = GridBagConstraints.NORTHWEST;
		gbc0.insets = new Insets(0, 0, 5, 0);
		gbc0.gridx = 0;
		gbc0.gridy = 0;
		detailsPanel.add(lblPlaceName, gbc0);

		lblCoordinates = new JLabel("");
		lblCoordinates.setFont(new Font("Tahoma", Font.PLAIN, 12));
		GridBagConstraints gbc1 = new GridBagConstraints();
		gbc1.anchor = GridBagConstraints.NORTHWEST;
		gbc1.gridx = 0;
		gbc1.gridy = 1;
		detailsPanel.add(lblCoordinates, gbc1);
	}

	public void fillFields() {
		if (this.currentPlace != null) {
			lblPlaceName.setText(this.currentPlace.getPlaceName());
			String lat = this.currentPlace.getLatitude();
			String lon = this.currentPlace.getLongitude();
			lblCoordinates.setText((lat != null && lon != null) ? lat + ", " + lon : "");
		} else {
			resetFields();
		}
	}

	public void resetFields() {
		lblPlaceName.setText("");
		lblCoordinates.setText("");
	}

	public Place getCurrentPlace() {
		return currentPlace;
	}

	public void setCurrentPlace(Place place) {
		this.currentPlace = place;
	}
}
