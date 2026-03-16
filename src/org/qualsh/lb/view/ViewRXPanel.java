package org.qualsh.lb.view;

import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import java.awt.GridBagLayout;

import javax.swing.JLabel;

import org.qualsh.lb.place.Place;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.BorderLayout;

public class ViewRXPanel extends JPanel {
	
	private static final long serialVersionUID = 8948072149922130328L;
	private JLabel lblCoordinates;
	private JLabel lblPlaceName;
	private Place curPlace = null;

	public Place getCurrentPlace() {
		return curPlace;
	}

	public void setCurrentPlace(Place curPlace) {
		this.curPlace = curPlace;
	}

	public void setLblCoordinates(JLabel lblCoordinates) {
		this.lblCoordinates = lblCoordinates;
	}

	public void setLblPlaceName(JLabel lblPlaceName) {
		this.lblPlaceName = lblPlaceName;
	}

	public ViewRXPanel() {
		setBorder(new TitledBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), new EmptyBorder(5, 5, 5, 5)), "RX Location", TitledBorder.LEADING, TitledBorder.TOP, new Font("Tahoma", Font.BOLD, 12), null));
		setLayout(new BorderLayout(0, 0));
		
		JPanel detailsPanel = new JPanel();
		detailsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		add(detailsPanel);
		GridBagLayout gbl_detailsPanel = new GridBagLayout();
		gbl_detailsPanel.columnWidths = new int[]{0, 0};
		gbl_detailsPanel.rowHeights = new int[] {25, 25, 0};
		gbl_detailsPanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_detailsPanel.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		detailsPanel.setLayout(gbl_detailsPanel);
		
		lblPlaceName = new JLabel("");
		lblPlaceName.setFont(new Font("Tahoma", Font.PLAIN, 12));
		GridBagConstraints gbc_lblPlaceName = new GridBagConstraints();
		gbc_lblPlaceName.insets = new Insets(0, 0, 5, 0);
		gbc_lblPlaceName.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblPlaceName.gridx = 0;
		gbc_lblPlaceName.gridy = 0;
		detailsPanel.add(lblPlaceName, gbc_lblPlaceName);
		
		lblCoordinates = new JLabel("");
		lblCoordinates.setFont(new Font("Tahoma", Font.PLAIN, 12));
		GridBagConstraints gbc_lblCoordinates = new GridBagConstraints();
		gbc_lblCoordinates.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblCoordinates.gridx = 0;
		gbc_lblCoordinates.gridy = 1;
		detailsPanel.add(lblCoordinates, gbc_lblCoordinates);
	}

	public JLabel getLblCoordinates() {
		return lblCoordinates;
	}
	public JLabel getLblPlaceName() {
		return lblPlaceName;
	}
	
	public void fillFields() {
		Place place = this.getCurrentPlace();
		if(place != null) {
			this.getLblCoordinates().setText(place.getLatitude()+", "+place.getLongitude());
			this.getLblPlaceName().setText(place.getPlaceName());
		} else {
			this.getLblCoordinates().setText("");
			this.getLblPlaceName().setText("");
		}
	}

	public void resetFields() {
		this.getLblCoordinates().setText("");
		this.getLblPlaceName().setText("");
	}
}
