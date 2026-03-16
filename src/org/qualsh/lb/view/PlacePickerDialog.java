package org.qualsh.lb.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.qualsh.lb.data.ViewPlacesModel;
import org.qualsh.lb.place.Place;

/**
 * Dialog for picking a user station location (Place) to attach to a log entry.
 * Allows selecting from existing places or creating a new one.
 */
public class PlacePickerDialog extends JDialog {

	private static final long serialVersionUID = 7382910293746182930L;

	private JList<Place> placeList;
	private ViewPlacesModel placesModel;
	private Place selectedPlace = null;
	private JButton btnSelect;
	private Runnable onPlaceCreated;

	public PlacePickerDialog(JFrame owner) {
		super(owner, "Select My Location", true);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setSize((int) (screenSize.getWidth() * 0.22), (int) (screenSize.getHeight() * 0.3));
		setMinimumSize(new Dimension(280, 280));
		setLocation(
			(int) ((screenSize.getWidth() - getWidth()) / 2),
			(int) ((screenSize.getHeight() - getHeight()) / 2)
		);

		JPanel root = new JPanel(new BorderLayout(0, 0));
		root.setBorder(new EmptyBorder(8, 8, 8, 8));
		getContentPane().add(root, BorderLayout.CENTER);

		JLabel lblHint = new JLabel("Select your station location for this log entry:");
		lblHint.setFont(new Font("Tahoma", Font.PLAIN, 11));
		lblHint.setBorder(new EmptyBorder(0, 0, 6, 0));
		root.add(lblHint, BorderLayout.NORTH);

		placesModel = new ViewPlacesModel();
		placesModel.setAllPlaces();

		placeList = new JList<>(placesModel);
		placeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		placeList.setCellRenderer(new PlaceCellRenderer());

		JScrollPane scrollPane = new JScrollPane(placeList);
		root.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new GridBagLayout());
		btnPanel.setBorder(new EmptyBorder(6, 0, 0, 0));
		root.add(btnPanel, BorderLayout.SOUTH);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(0, 0, 0, 4);

		JButton btnAddNew = new JButton("Add New\u2026");
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.anchor = GridBagConstraints.WEST;
		btnPanel.add(btnAddNew, gbc);

		btnSelect = new JButton("Select");
		btnSelect.setEnabled(false);
		gbc.gridx = 1;
		gbc.weightx = 0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.insets = new Insets(0, 0, 0, 4);
		btnPanel.add(btnSelect, gbc);

		JButton btnCancel = new JButton("Cancel");
		gbc.gridx = 2;
		gbc.insets = new Insets(0, 0, 0, 0);
		btnPanel.add(btnCancel, gbc);

		// Enable Select when something is picked
		placeList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				btnSelect.setEnabled(placeList.getSelectedValue() != null);
			}
		});

		// Double-click selects immediately
		placeList.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && placeList.getSelectedValue() != null) {
					selectedPlace = placeList.getSelectedValue();
					PlacePickerDialog.this.setVisible(false);
				}
			}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
		});

		btnSelect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectedPlace = placeList.getSelectedValue();
				PlacePickerDialog.this.setVisible(false);
			}
		});

		btnAddNew.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PlaceDialog dlg = new PlaceDialog(owner, null);
				dlg.setVisible(true);
				Place created = dlg.getResultPlace();
				if (created != null) {
					placesModel.setAllPlaces();
					placeList.updateUI();
					// Auto-select the newly created place
					for (int i = 0; i < placesModel.getSize(); i++) {
						if (placesModel.getElementAt(i).getId() == created.getId()) {
							placeList.setSelectedIndex(i);
							break;
						}
					}
					// Notify external listener (e.g. LocationsTab) of the new place
					if (onPlaceCreated != null) onPlaceCreated.run();
				}
			}
		});

		btnCancel.addActionListener(e -> PlacePickerDialog.this.setVisible(false));
	}

	/** Returns the Place the user selected, or null if cancelled. */
	public Place getSelectedPlace() {
		return selectedPlace;
	}

	/** Register a callback invoked when a new Place is created inside this dialog. */
	public void setOnPlaceCreated(Runnable r) {
		this.onPlaceCreated = r;
	}

	private static class PlaceCellRenderer implements ListCellRenderer<Place> {
		public Component getListCellRendererComponent(
				JList<? extends Place> list, Place value, int index,
				boolean isSelected, boolean cellHasFocus) {

			JPanel pnl = new JPanel(new BorderLayout(0, 2));
			pnl.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

			JLabel lblName = new JLabel(value.getPlaceName());
			lblName.setFont(new Font("Tahoma", Font.BOLD, 12));
			pnl.add(lblName, BorderLayout.NORTH);

			String lat = value.getLatitude();
			String lon = value.getLongitude();
			String coords = (lat != null && lon != null && !lat.isBlank() && !lon.isBlank())
				? lat + ", " + lon : "No coordinates";
			JLabel lblCoords = new JLabel(coords);
			lblCoords.setFont(new Font("Tahoma", Font.PLAIN, 11));
			pnl.add(lblCoords, BorderLayout.SOUTH);

			UIDefaults defs = UIManager.getDefaults();
			if (isSelected || cellHasFocus) {
				pnl.setBackground(defs.getColor("List.selectionBackground"));
				lblName.setForeground(defs.getColor("List.selectionForeground"));
				lblCoords.setForeground(defs.getColor("List.selectionForeground"));
			} else {
				pnl.setBackground(defs.getColor("List.background"));
				lblName.setForeground(defs.getColor("List.foreground"));
				lblCoords.setForeground(defs.getColor("List.foreground"));
			}

			return pnl;
		}
	}
}
